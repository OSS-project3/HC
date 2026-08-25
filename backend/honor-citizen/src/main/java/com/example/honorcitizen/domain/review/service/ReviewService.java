package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ReviewSearchType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.review.dto.CardTypeSummaryResponse;
import com.example.honorcitizen.domain.review.dto.ReviewCreateRequest;
import com.example.honorcitizen.domain.review.dto.ReviewCreateResponse;
import com.example.honorcitizen.domain.review.dto.ReviewDetailResponse;
import com.example.honorcitizen.domain.review.dto.ReviewImageResponse;
import com.example.honorcitizen.domain.review.dto.ReviewListItemResponse;
import com.example.honorcitizen.domain.review.dto.ReviewUpdateRequest;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.entity.ReviewImage;
import com.example.honorcitizen.domain.review.repository.ReviewImageRepository;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    // 프론트 카드 목록에 상시 노출되는 이미지라 만료를 짧게 둘 이유가 없다 — 매 목록/단건 조회마다 새로 발급된다.
    private static final long IMAGE_URL_EXPIRY_SECONDS = 60 * 60L;
    private static final int MAX_PAGE_SIZE = 100;
    // 후기 한 건에 첨부 가능한 최대 사진 수.
    private static final int MAX_IMAGE_COUNT = 5;
    // UNIQUE(review_id, display_order) 순간 충돌을 피하기 위한 임시 오프셋(EventService와 동일 기법).
    private static final int REORDER_TEMP_OFFSET = 100_000;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UserRepository userRepository;
    private final ReviewEligibilityService eligibilityService;
    private final ReviewImageValidator imageValidator;
    private final StorageService storageService;

    @Transactional
    public ReviewCreateResponse create(Long userId, ReviewCreateRequest request, List<MultipartFile> images) {
        cardTypeRepository.findById(request.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        eligibilityService.validateForCreate(userId, request.getApplicationType(), request.getCardTypeId());

        List<MultipartFile> files = presentImages(images);
        if (files.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        files.forEach(imageValidator::validate);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            List<String> paths = new ArrayList<>();
            for (MultipartFile file : files) {
                paths.add(uploadImage(file, uploadedKeys));
            }
            // image_path(대표/썸네일) = 첫 이미지 경로를 비정규화해 유지 — 목록 썸네일·hasPhoto 필터가 그대로 동작.
            String primary = paths.isEmpty() ? null : paths.get(0);

            Review review = Review.create(userId, request.getAuthorName(), request.getTitle(),
                    request.getApplicationType(), request.getCardTypeId(), request.getContent(), primary);
            reviewRepository.save(review);

            for (int i = 0; i < paths.size(); i++) {
                reviewImageRepository.save(
                        ReviewImage.create(review.getId(), paths.get(i), files.get(i).getOriginalFilename(), i));
            }
            return ReviewCreateResponse.from(review);
        } catch (RuntimeException e) {
            deleteUploadedFilesQuietlyReversed(uploadedKeys);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListItemResponse> list(Long cardTypeId, Boolean hasPhoto,
            ReviewSearchType searchType, String keyword, int page, int size) {
        validatePage(page, size);
        if (cardTypeId != null && !cardTypeRepository.existsById(cardTypeId)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        Specification<Review> spec = Specification
                .where(ReviewSpecifications.cardTypeId(cardTypeId))
                .and(ReviewSpecifications.hasPhoto(hasPhoto))
                .and(ReviewSpecifications.keyword(searchType, keyword));
        // createdAt만으로는 동시 등록 시 밀리초 단위로 값이 같아질 수 있어(H2 등) id를 2차 정렬키로 더한다.
        // IDENTITY 자동증가 + 즉시 저장이라 id 순서가 항상 createdAt 순서와 일치한다(ReviewRepository 참고).
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return toListResponse(reviewRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListItemResponse> listMine(Long userId, int page, int size) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return toListResponse(reviewRepository.findByUserId(userId, pageable));
    }

    private PageResponse<ReviewListItemResponse> toListResponse(Page<Review> reviews) {
        Map<Long, CardType> cardTypeById = cardTypeRepository
                .findAllById(reviews.getContent().stream().map(Review::getCardTypeId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CardType::getId, Function.identity()));

        return PageResponse.from(reviews, review -> ReviewListItemResponse.of(
                review,
                CardTypeSummaryResponse.from(cardTypeById.get(review.getCardTypeId())),
                imageUrlOrNull(review.getImagePath())));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // 단건조회는 레거시(단일 image_path) 데이터를 review_images 행으로 지연 마이그레이션할 수 있어 쓰기 트랜잭션이다.
    @Transactional
    public ReviewDetailResponse detail(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        CardType cardType = cardTypeRepository.findById(review.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        ReviewDetailResponse.NextReview next = reviewRepository.findFirstByIdLessThanOrderByIdDesc(id)
                .map(ReviewDetailResponse.NextReview::from)
                .orElse(null);

        boolean canManage = isAdmin(userId) || review.isOwnedBy(userId);

        List<ReviewImageResponse> images = ensureImageRows(review).stream()
                .map(image -> ReviewImageResponse.of(image.getId(), imageUrlOrNull(image.getImagePath())))
                .toList();

        return ReviewDetailResponse.of(review, CardTypeSummaryResponse.from(cardType), images, next, canManage, canManage);
    }

    // 삭제(api.md §API 4): Review·ReviewImage row를 한 트랜잭션에서 지우고 커밋 후 S3 객체를 지운다.
    // 커밋 전에 지우면 롤백 시 DB엔 남아있는데 이미지만 사라지는 불일치가 생기므로 순서를 지킨다.
    @Transactional
    public void delete(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!isAdmin(userId) && !review.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Set<String> keys = new LinkedHashSet<>();
        reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(id)
                .forEach(image -> keys.add(image.getImagePath()));
        if (review.getImagePath() != null) {
            keys.add(review.getImagePath());
        }

        reviewImageRepository.deleteByReviewId(id);
        reviewRepository.delete(review);
        deleteFilesAfterCommit(new ArrayList<>(keys));
    }

    // 수정(api.md §API 5): 등록과 동일 5개 필드 전체 갱신 + 갤러리(keepImageIds) 편집.
    // applicationType/cardTypeId도 편집 가능해 자격을 다시 검증하되 기준은 항상 원작성자(review.getUserId())다.
    @Transactional
    public void update(Long id, Long editorUserId, ReviewUpdateRequest request, List<MultipartFile> images) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!isAdmin(editorUserId) && !review.isOwnedBy(editorUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        cardTypeRepository.findById(request.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        eligibilityService.validateForUpdate(
                review.getUserId(), request.getApplicationType(), request.getCardTypeId(), review.getId());

        // 레거시 단일 이미지를 keepImageIds로 참조 가능하도록 행으로 승격한다.
        ensureImageRows(review);

        List<MultipartFile> newFiles = presentImages(images);
        newFiles.forEach(imageValidator::validate);

        List<ReviewImage> keptImages = resolveKeptImages(id, request.getKeepImageIds(), Boolean.TRUE.equals(request.getRemoveImage()));
        if (keptImages.size() + newFiles.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<String> newUploadedKeys = new ArrayList<>();
        try {
            List<String> deletedKeys = reconcileImages(id, keptImages, newFiles, newUploadedKeys);

            // 대표 이미지(썸네일) = 최종 목록의 첫 번째: 유지분이 있으면 그 첫 장, 없으면 새 업로드의 첫 장.
            String primary = !keptImages.isEmpty() ? keptImages.get(0).getImagePath()
                    : newUploadedKeys.isEmpty() ? null : newUploadedKeys.get(0);
            review.updateImagePath(primary);

            review.update(request.getAuthorName(), request.getTitle(), request.getApplicationType(),
                    request.getCardTypeId(), request.getContent());

            deleteFilesAfterCommit(deletedKeys);
        } catch (RuntimeException e) {
            deleteUploadedFilesQuietlyReversed(newUploadedKeys);
            throw e;
        }
    }

    // 단일 image_path 시절 데이터 지연 마이그레이션 — image_path를 review_images 0번 행으로 승격한다.
    private List<ReviewImage> ensureImageRows(Review review) {
        List<ReviewImage> rows = reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(review.getId());
        if (rows.isEmpty() && review.getImagePath() != null) {
            return List.of(reviewImageRepository.save(
                    ReviewImage.create(review.getId(), review.getImagePath(), null, 0)));
        }
        return rows;
    }

    // keepImageIds 해석: null(필드 생략)=기존 전체 유지(단, removeImage=true면 전체 삭제 — 레거시 단일이미지 호환),
    // 빈 배열=전체 삭제, 그 외=지정 id만 지정 순서대로 유지. 타 Review 소유·존재하지 않는 id는 거절.
    private List<ReviewImage> resolveKeptImages(Long reviewId, List<Long> keepImageIds, boolean removeImage) {
        if (keepImageIds == null) {
            return removeImage ? List.of() : reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
        }
        if (keepImageIds.isEmpty()) {
            return List.of();
        }
        List<ReviewImage> found = reviewImageRepository.findByIdInAndReviewId(keepImageIds, reviewId);
        if (found.size() != keepImageIds.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Map<Long, ReviewImage> byId = found.stream().collect(Collectors.toMap(ReviewImage::getId, Function.identity()));
        return keepImageIds.stream().map(byId::get).toList();
    }

    // kept 이미지 재정렬 + 미포함 기존 이미지 삭제 + 신규 이미지 추가. 반환값은 커밋 후 S3에서 지울 키 목록.
    private List<String> reconcileImages(Long reviewId, List<ReviewImage> keptImages,
            List<MultipartFile> newFiles, List<String> newUploadedKeys) {
        List<ReviewImage> allExisting = reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
        Set<Long> keptIds = keptImages.stream().map(ReviewImage::getId).collect(Collectors.toSet());
        List<ReviewImage> toDelete = allExisting.stream().filter(image -> !keptIds.contains(image.getId())).toList();

        // UNIQUE(review_id, display_order) 순간 충돌을 피하려고 임시 오프셋으로 한 번 민 뒤 최종 배정한다
        // (예: 0번과 1번을 맞바꾸면 중간 상태에서 값이 겹칠 수 있음).
        for (int i = 0; i < keptImages.size(); i++) {
            keptImages.get(i).updateDisplayOrder(REORDER_TEMP_OFFSET + i);
        }
        reviewImageRepository.flush();
        for (int i = 0; i < keptImages.size(); i++) {
            keptImages.get(i).updateDisplayOrder(i);
        }

        List<String> deletedKeys = toDelete.stream().map(ReviewImage::getImagePath).toList();
        List<Long> toDeleteIds = toDelete.stream().map(ReviewImage::getId).toList();
        if (!toDeleteIds.isEmpty()) {
            reviewImageRepository.deleteAllById(toDeleteIds);
            // 삭제를 먼저 DB에 반영해야 새로 추가되는 이미지가 방금 비운 display_order를 재사용할 때
            // UNIQUE(review_id, display_order) 제약과 충돌하지 않는다(Hibernate는 기본적으로 INSERT를
            // DELETE보다 먼저 flush하므로 명시적으로 순서를 강제한다).
            reviewImageRepository.flush();
        }

        int nextOrder = keptImages.size();
        for (MultipartFile file : newFiles) {
            String path = uploadImage(file, newUploadedKeys);
            reviewImageRepository.save(ReviewImage.create(reviewId, path, file.getOriginalFilename(), nextOrder++));
        }

        return deletedKeys;
    }

    // 비로그인(userId == null)이면 항상 false. 로그인 상태에서만 실제 role을 조회한다.
    private boolean isAdmin(Long userId) {
        return userId != null && userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    private String imageUrlOrNull(String imagePath) {
        return imagePath == null ? null : storageService.generatePresignedUrl(imagePath, IMAGE_URL_EXPIRY_SECONDS);
    }

    private List<MultipartFile> presentImages(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(file -> !file.isEmpty()).toList();
    }

    private String uploadImage(MultipartFile image, List<String> uploadedKeys) {
        String key = "reviews/" + UUID.randomUUID() + "-" + sanitizeFilename(image.getOriginalFilename());
        storageService.upload(key, image);
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
                keys.forEach(ReviewService.this::deleteFileQuietly);
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
            log.warn("Failed to delete review image from storage. key={}", key, e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
