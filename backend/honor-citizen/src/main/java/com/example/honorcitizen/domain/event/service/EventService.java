package com.example.honorcitizen.domain.event.service;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGE_COUNT = 10;
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
        return PageResponse.from(eventPosts,
                eventPost -> EventListItemResponse.of(eventPost, thumbnailUrlOrNull(eventPost.getThumbnailImagePath())));
    }

    @Transactional(readOnly = true)
    public EventDetailResponse detail(Long id) {
        EventPost eventPost = eventPostRepository.findByIdAndVisibleTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        List<EventImageResponse> images = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(id).stream()
                .map(image -> EventImageResponse.of(image.getId(), image.getOriginalFilename(),
                        storageService.generatePresignedUrl(image.getImagePath(), IMAGE_URL_EXPIRY_SECONDS)))
                .toList();

        return EventDetailResponse.of(eventPost, thumbnailUrlOrNull(eventPost.getThumbnailImagePath()), images);
    }

    // 생성(api.md §API 3, data-model.md §4.1과 동일 골격): 썸네일+갤러리 S3 업로드 → DB 트랜잭션 저장.
    // 실패 시 uploadedKeys 역순 보상삭제(Board BoardService.create()와 동일 패턴).
    @Transactional
    public EventCreateResponse create(EventCreateRequest request, MultipartFile thumbnail, List<MultipartFile> images) {
        MultipartFile thumbnailFile = presentFile(thumbnail);
        List<MultipartFile> galleryFiles = presentFiles(images);
        if (galleryFiles.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (thumbnailFile != null) {
            imageValidator.validate(thumbnailFile);
        }
        galleryFiles.forEach(imageValidator::validate);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            String thumbnailPath = thumbnailFile != null ? uploadImage(thumbnailFile, "thumbnails", uploadedKeys) : null;

            EventPost eventPost = eventPostRepository.save(EventPost.create(
                    request.getEventType(), request.getTitle(), request.getEventDate(), request.getEventDateText(),
                    request.getPlace(), request.getHost(), request.getCardLabel(), request.getContent(),
                    thumbnailPath, visibleOrDefault(request.getVisible()), request.getDisplayOrder()));

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

    // 수정(api.md §API 4) — 전체 재제출. 갤러리(EventImage)는 이번 패스에서 건드리지 않는다.
    // 썸네일은 새 파일이 있을 때만 교체(기존 파일은 커밋 이후 삭제, Review applyImageChange와 동일 패턴).
    @Transactional
    public void update(Long id, EventUpdateRequest request, MultipartFile thumbnail) {
        EventPost eventPost = eventPostRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        MultipartFile thumbnailFile = presentFile(thumbnail);
        if (thumbnailFile != null) {
            imageValidator.validate(thumbnailFile);
        }

        eventPost.update(request.getEventType(), request.getTitle(), request.getEventDate(), request.getEventDateText(),
                request.getPlace(), request.getHost(), request.getCardLabel(), request.getContent(),
                visibleOrDefault(request.getVisible()), request.getDisplayOrder());

        if (thumbnailFile != null) {
            String oldThumbnailPath = eventPost.getThumbnailImagePath();
            String newThumbnailPath = uploadImage(thumbnailFile, "thumbnails", new ArrayList<>());
            eventPost.updateThumbnailImagePath(newThumbnailPath);
            deleteFilesAfterCommit(oldThumbnailPath == null ? List.of() : List.of(oldThumbnailPath));
        }
    }

    // 삭제(api.md §API 5, data-model.md와 동일 원칙): EventImage+EventPost를 한 트랜잭션에서 지우고
    // S3(썸네일+갤러리 전체)는 커밋 이후 삭제한다 — 순서를 바꾸면 롤백 시 DB·S3 불일치가 생긴다.
    @Transactional
    public void delete(Long id) {
        EventPost eventPost = eventPostRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        List<EventImage> images = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(id);
        List<String> keys = new ArrayList<>();
        if (eventPost.getThumbnailImagePath() != null) {
            keys.add(eventPost.getThumbnailImagePath());
        }
        images.forEach(image -> keys.add(image.getImagePath()));

        eventImageRepository.deleteByEventPostId(id);
        eventPostRepository.delete(eventPost);

        deleteFilesAfterCommit(keys);
    }

    // visible을 생략하면 true로 채운다(data-model.md §1 기본값) — create/update 공통.
    private boolean visibleOrDefault(Boolean visible) {
        return visible == null || visible;
    }

    private String thumbnailUrlOrNull(String thumbnailImagePath) {
        return thumbnailImagePath == null ? null
                : storageService.generatePresignedUrl(thumbnailImagePath, IMAGE_URL_EXPIRY_SECONDS);
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
