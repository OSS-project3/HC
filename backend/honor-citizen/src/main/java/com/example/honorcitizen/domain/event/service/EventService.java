package com.example.honorcitizen.domain.event.service;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.event.dto.EventAdminDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventAdminListItemResponse;
import com.example.honorcitizen.domain.event.dto.EventCreateRequest;
import com.example.honorcitizen.domain.event.dto.EventCreateResponse;
import com.example.honorcitizen.domain.event.dto.EventDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventImageResponse;
import com.example.honorcitizen.domain.event.dto.EventListItemResponse;
import com.example.honorcitizen.domain.event.dto.EventUpdateRequest;
import com.example.honorcitizen.domain.event.entity.EventImage;
import com.example.honorcitizen.domain.event.entity.EventPost;
import com.example.honorcitizen.domain.event.repository.EventImageRepository;
import com.example.honorcitizen.domain.event.repository.EventPostRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGE_COUNT = 10;
    // 갤러리 재정렬 시 UNIQUE(event_post_id, display_order) 순간 충돌을 피하기 위한 임시 오프셋
    // (EVENT-EXT-3 — 유지되는 이미지 개수가 이 값보다 적다는 전제, MAX_IMAGE_COUNT보다 충분히 크면 안전).
    private static final int REORDER_TEMP_OFFSET = 1000;
    // 상세 화면에 상시 노출되는 이미지라 만료를 짧게 둘 이유가 없다(Board/Review와 동일 이유).
    private static final long IMAGE_URL_EXPIRY_SECONDS = 60 * 60L;

    private final EventPostRepository eventPostRepository;
    private final EventImageRepository eventImageRepository;
    private final EventImageValidator imageValidator;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public PageResponse<EventListItemResponse> list(EventType type, int page, int size) {
        if (type == null || page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 정렬은 Repository의 고정 ORDER BY(display_order→event_date→created_at)가 전담하므로
        // Pageable에는 정렬을 싣지 않는다(data-model.md §1).
        Pageable pageable = PageRequest.of(page, size);
        Page<EventPost> eventPosts = eventPostRepository.findVisibleByEventType(type, pageable);
        return PageResponse.from(eventPosts, eventPost -> EventListItemResponse.of(eventPost,
                thumbnailUrlOrNull(eventPost.getThumbnailImagePath()), thumbnailUrlOrNull(eventPost.getLogoImagePath())));
    }

    @Transactional(readOnly = true)
    public EventDetailResponse detail(Long id) {
        EventPost eventPost = eventPostRepository.findByIdAndVisibleTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        List<EventImageResponse> images = loadImageResponses(id);
        return EventDetailResponse.of(eventPost, thumbnailUrlOrNull(eventPost.getThumbnailImagePath()),
                thumbnailUrlOrNull(eventPost.getLogoImagePath()), images);
    }

    // 관리자 목록(api.md §API 6) — visible 무관 전체, type/visible 둘 다 선택 필터.
    @Transactional(readOnly = true)
    public PageResponse<EventAdminListItemResponse> listForAdmin(EventType type, Boolean visible, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<EventPost> eventPosts = eventPostRepository.findAllForAdmin(type, visible, pageable);
        return PageResponse.from(eventPosts, eventPost -> EventAdminListItemResponse.of(eventPost,
                thumbnailUrlOrNull(eventPost.getThumbnailImagePath()), thumbnailUrlOrNull(eventPost.getLogoImagePath())));
    }

    // 관리자 상세(api.md §API 7) — visible=false도 조회 가능(공개 detail과의 유일한 차이).
    @Transactional(readOnly = true)
    public EventAdminDetailResponse detailForAdmin(Long id) {
        EventPost eventPost = eventPostRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        List<EventImageResponse> images = loadImageResponses(id);
        return EventAdminDetailResponse.of(eventPost, thumbnailUrlOrNull(eventPost.getThumbnailImagePath()),
                thumbnailUrlOrNull(eventPost.getLogoImagePath()), images);
    }

    // 생성(api.md §API 3, data-model.md §4.1과 동일 골격): 썸네일+로고+갤러리 S3 업로드 → DB 트랜잭션 저장.
    // 실패 시 uploadedKeys 역순 보상삭제(Board BoardService.create()와 동일 패턴).
    @Transactional
    public EventCreateResponse create(EventCreateRequest request, MultipartFile thumbnail, MultipartFile logo,
            List<MultipartFile> images) {
        MultipartFile thumbnailFile = presentFile(thumbnail);
        MultipartFile logoFile = presentFile(logo);
        List<MultipartFile> galleryFiles = presentFiles(images);
        if (galleryFiles.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        // BOOTH가 협업 전용 데이터를 보내면 업로드 전에 즉시 거절한다(파일 검증은 S3 업로드 전에 완료).
        if (request.getEventType() != EventType.COLLABORATION
                && (request.getCompanyName() != null || logoFile != null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (thumbnailFile != null) {
            imageValidator.validate(thumbnailFile);
        }
        if (logoFile != null) {
            imageValidator.validate(logoFile);
        }
        galleryFiles.forEach(imageValidator::validate);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            String thumbnailPath = thumbnailFile != null ? uploadImage(thumbnailFile, "thumbnails", uploadedKeys) : null;
            String logoPath = logoFile != null ? uploadImage(logoFile, "logos", uploadedKeys) : null;

            EventPost eventPost = eventPostRepository.save(EventPost.create(
                    request.getEventType(), request.getTitle(), request.getEventDate(), request.getEventDateText(),
                    request.getPlace(), request.getHost(), request.getCardLabel(), request.getContent(),
                    thumbnailPath, request.getCompanyName(), logoPath, visibleOrDefault(request.getVisible()),
                    request.getDisplayOrder()));

            int displayOrder = 0;
            for (MultipartFile file : galleryFiles) {
                String path = uploadImage(file, "gallery", uploadedKeys);
                eventImageRepository.save(EventImage.create(eventPost.getId(), path, file.getOriginalFilename(), displayOrder++));
            }

            return EventCreateResponse.from(eventPost);
        } catch (RuntimeException e) {
            deleteUploadedFilesQuietlyReversed(uploadedKeys);
            throw e;
        }
    }

    // 수정(api.md §API 4) — 전체 재제출 + 로고/썸네일 유지·교체·삭제 + 갤러리(keepImageIds) 편집(EVENT-EXT-3).
    @Transactional
    public void update(Long id, EventUpdateRequest request, MultipartFile thumbnail, MultipartFile logo,
            List<MultipartFile> images) {
        EventPost eventPost = eventPostRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        MultipartFile thumbnailFile = presentFile(thumbnail);
        MultipartFile logoFile = presentFile(logo);
        List<MultipartFile> newGalleryFiles = presentFiles(images);
        boolean removeLogo = Boolean.TRUE.equals(request.getRemoveLogo());
        boolean removeThumbnail = Boolean.TRUE.equals(request.getRemoveThumbnail());

        if (removeLogo && logoFile != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (removeThumbnail && thumbnailFile != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        // BOOTH 전환은 companyName=null + removeLogo=true(또는 애초에 로고가 없어야) 함께 요구한다 —
        // 업로드 전에 미리 걸러 불필요한 S3 업로드를 막는다. 최종 상태는 assertCollaborationInvariant로
        // 한 번 더 검증한다(엔티티가 스스로의 불변조건을 보장하는 이 프로젝트의 기존 관례).
        if (request.getEventType() != EventType.COLLABORATION) {
            if (request.getCompanyName() != null || logoFile != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            if (eventPost.getLogoImagePath() != null && !removeLogo) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        }

        if (thumbnailFile != null) {
            imageValidator.validate(thumbnailFile);
        }
        if (logoFile != null) {
            imageValidator.validate(logoFile);
        }
        newGalleryFiles.forEach(imageValidator::validate);

        List<EventImage> keptImages = resolveKeptImages(id, request.getKeepImageIds());
        if (keptImages.size() + newGalleryFiles.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<String> newUploadedKeys = new ArrayList<>();
        try {
            eventPost.update(request.getEventType(), request.getTitle(), request.getEventDate(), request.getEventDateText(),
                    request.getPlace(), request.getHost(), request.getCardLabel(), request.getContent(),
                    request.getCompanyName(), visibleOrDefault(request.getVisible()), request.getDisplayOrder());

            String oldThumbnailPath = null;
            if (removeThumbnail) {
                oldThumbnailPath = eventPost.getThumbnailImagePath();
                eventPost.updateThumbnailImagePath(null);
            } else if (thumbnailFile != null) {
                oldThumbnailPath = eventPost.getThumbnailImagePath();
                eventPost.updateThumbnailImagePath(uploadImage(thumbnailFile, "thumbnails", newUploadedKeys));
            }

            String oldLogoPath = null;
            if (removeLogo) {
                oldLogoPath = eventPost.getLogoImagePath();
                eventPost.updateLogoImagePath(null);
            } else if (logoFile != null) {
                oldLogoPath = eventPost.getLogoImagePath();
                eventPost.updateLogoImagePath(uploadImage(logoFile, "logos", newUploadedKeys));
            }

            eventPost.assertCollaborationInvariant();

            List<String> deletedGalleryKeys = reconcileGallery(id, keptImages, newGalleryFiles, newUploadedKeys);

            List<String> oldFilesToDelete = new ArrayList<>(deletedGalleryKeys);
            if (oldThumbnailPath != null) {
                oldFilesToDelete.add(oldThumbnailPath);
            }
            if (oldLogoPath != null) {
                oldFilesToDelete.add(oldLogoPath);
            }
            deleteFilesAfterCommit(oldFilesToDelete);
        } catch (RuntimeException e) {
            deleteUploadedFilesQuietlyReversed(newUploadedKeys);
            throw e;
        }
    }

    // 삭제(api.md §API 5, data-model.md와 동일 원칙): EventImage+EventPost를 한 트랜잭션에서 지우고
    // S3(썸네일+로고+갤러리 전체)는 커밋 이후 삭제한다 — 순서를 바꾸면 롤백 시 DB·S3 불일치가 생긴다.
    @Transactional
    public void delete(Long id) {
        EventPost eventPost = eventPostRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        List<EventImage> images = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(id);
        List<String> keys = new ArrayList<>();
        if (eventPost.getThumbnailImagePath() != null) {
            keys.add(eventPost.getThumbnailImagePath());
        }
        if (eventPost.getLogoImagePath() != null) {
            keys.add(eventPost.getLogoImagePath());
        }
        images.forEach(image -> keys.add(image.getImagePath()));

        eventImageRepository.deleteByEventPostId(id);
        eventPostRepository.delete(eventPost);

        deleteFilesAfterCommit(keys);
    }

    // keepImageIds 해석(EVENT-EXT-3): null(필드 생략)=기존 전체 유지, 빈 배열=기존 전체 삭제,
    // 그 외=지정된 id만 유지하고 지정한 순서대로 정렬. 소유권 불일치(타 Event id·존재하지 않는 id)는 거절.
    private List<EventImage> resolveKeptImages(Long eventPostId, List<Long> keepImageIds) {
        if (keepImageIds == null) {
            return eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(eventPostId);
        }
        if (keepImageIds.isEmpty()) {
            return List.of();
        }
        List<EventImage> found = eventImageRepository.findByIdInAndEventPostId(keepImageIds, eventPostId);
        if (found.size() != keepImageIds.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Map<Long, EventImage> byId = found.stream().collect(Collectors.toMap(EventImage::getId, Function.identity()));
        return keepImageIds.stream().map(byId::get).toList();
    }

    // kept 이미지 재정렬 + 미포함 기존 이미지 삭제 + 신규 이미지 추가. 반환값은 커밋 후 S3에서 지울 키 목록.
    private List<String> reconcileGallery(Long eventPostId, List<EventImage> keptImages,
            List<MultipartFile> newGalleryFiles, List<String> newUploadedKeys) {
        List<EventImage> allExisting = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(eventPostId);
        Set<Long> keptIds = keptImages.stream().map(EventImage::getId).collect(Collectors.toSet());
        List<EventImage> toDelete = allExisting.stream().filter(image -> !keptIds.contains(image.getId())).toList();

        // UNIQUE(event_post_id, display_order) 순간 충돌을 피하려고 임시 오프셋으로 한 번 민 뒤 최종 배정한다
        // (예: 이미지 0번과 1번을 서로 맞바꾸면 중간 상태에서 값이 겹칠 수 있음).
        for (int i = 0; i < keptImages.size(); i++) {
            keptImages.get(i).updateDisplayOrder(REORDER_TEMP_OFFSET + i);
        }
        eventImageRepository.flush();
        for (int i = 0; i < keptImages.size(); i++) {
            keptImages.get(i).updateDisplayOrder(i);
        }

        List<String> deletedKeys = toDelete.stream().map(EventImage::getImagePath).toList();
        List<Long> toDeleteIds = toDelete.stream().map(EventImage::getId).toList();
        if (!toDeleteIds.isEmpty()) {
            eventImageRepository.deleteAllById(toDeleteIds);
        }

        int nextOrder = keptImages.size();
        for (MultipartFile file : newGalleryFiles) {
            String path = uploadImage(file, "gallery", newUploadedKeys);
            eventImageRepository.save(EventImage.create(eventPostId, path, file.getOriginalFilename(), nextOrder++));
        }

        return deletedKeys;
    }

    private List<EventImageResponse> loadImageResponses(Long eventPostId) {
        return eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(eventPostId).stream()
                .map(image -> EventImageResponse.of(image.getId(), image.getOriginalFilename(),
                        storageService.generatePresignedUrl(image.getImagePath(), IMAGE_URL_EXPIRY_SECONDS)))
                .toList();
    }

    // visible을 생략하면 true로 채운다(data-model.md §1 기본값) — create/update 공통.
    private boolean visibleOrDefault(Boolean visible) {
        return visible == null || visible;
    }

    private String thumbnailUrlOrNull(String imagePath) {
        return imagePath == null ? null : storageService.generatePresignedUrl(imagePath, IMAGE_URL_EXPIRY_SECONDS);
    }

    private MultipartFile presentFile(MultipartFile file) {
        return file == null || file.isEmpty() ? null : file;
    }

    private List<MultipartFile> presentFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(file -> !file.isEmpty()).toList();
    }

    private String uploadImage(MultipartFile file, String subPath, List<String> uploadedKeys) {
        String key = "events/" + subPath + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        storageService.upload(key, file);
        uploadedKeys.add(key);
        return key;
    }

    private void deleteFilesAfterCommit(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            keys.forEach(this::deleteFileQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(EventService.this::deleteFileQuietly);
            }
        });
    }

    private void deleteUploadedFilesQuietlyReversed(List<String> uploadedKeys) {
        for (int i = uploadedKeys.size() - 1; i >= 0; i--) {
            deleteFileQuietly(uploadedKeys.get(i));
        }
    }

    private void deleteFileQuietly(String key) {
        try {
            storageService.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete event image from storage. key={}", key, e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
