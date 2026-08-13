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
import com.example.honorcitizen.domain.review.dto.ReviewListItemResponse;
import com.example.honorcitizen.domain.review.dto.ReviewUpdateRequest;
import com.example.honorcitizen.domain.review.entity.Review;
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

import java.util.List;
import java.util.Map;
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

    private final ReviewRepository reviewRepository;
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

        String imagePath = validateAndUploadImage(singleImageOrNull(images));

        Review review = Review.create(userId, request.getAuthorName(), request.getTitle(),
                request.getApplicationType(), request.getCardTypeId(), request.getContent(), imagePath);
        return ReviewCreateResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListItemResponse> list(Long cardTypeId, Boolean hasPhoto,
            ReviewSearchType searchType, String keyword, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
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

        Page<Review> reviews = reviewRepository.findAll(spec, pageable);
        Map<Long, CardType> cardTypeById = cardTypeRepository
                .findAllById(reviews.getContent().stream().map(Review::getCardTypeId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CardType::getId, Function.identity()));

        return PageResponse.from(reviews, review -> ReviewListItemResponse.of(
                review,
                CardTypeSummaryResponse.from(cardTypeById.get(review.getCardTypeId())),
                imageUrlOrNull(review.getImagePath())));
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse detail(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        CardType cardType = cardTypeRepository.findById(review.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        ReviewDetailResponse.NextReview next = reviewRepository.findFirstByIdLessThanOrderByIdDesc(id)
                .map(ReviewDetailResponse.NextReview::from)
                .orElse(null);

        boolean canManage = isAdmin(userId) || review.isOwnedBy(userId);

        return ReviewDetailResponse.of(review, CardTypeSummaryResponse.from(cardType),
                imageUrlOrNull(review.getImagePath()), next, canManage, canManage);
    }

    // 삭제 범위(api.md §API 4): Review row 삭제 후 커밋되면 S3 이미지 객체를 지운다. 커밋 전에 지우면
    // 트랜잭션이 롤백됐을 때 DB엔 Review가 남아있는데 이미지만 사라지는 불일치가 생기므로 순서를 지킨다.
    @Transactional
    public void delete(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!isAdmin(userId) && !review.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        reviewRepository.delete(review);
        deleteImageAfterCommit(review.getImagePath());
    }

    // 수정(api.md §API 5): 등록과 동일 5개 필드를 전부 재제출받는 전체 갱신. applicationType/cardTypeId도
    // 편집 가능해서 등록 때와 동일하게 자격을 다시 검증하되, 기준은 항상 원작성자(review.getUserId())다
    // — 관리자가 대신 수정해도 관리자 본인 기준으로 검증하지 않는다.
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

        MultipartFile image = singleImageOrNull(images);
        if (image != null && request.isRemoveImage()) {
            // 새 파일과 삭제 요청이 동시에 오면 의도가 모순된다 — api.md §API 5 Validation.
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        applyImageChange(review, image, request.isRemoveImage());

        review.update(request.getAuthorName(), request.getTitle(), request.getApplicationType(),
                request.getCardTypeId(), request.getContent());
    }

    // 사진 처리 3가지 경우(api.md §API 5): 1) 새 파일 있음 → 업로드 성공 확인 후 기존 파일 삭제 예약
    // 2) removeImage=true → 기존 파일만 삭제 예약 3) 둘 다 아니면 아무 것도 하지 않음(기존 유지).
    private void applyImageChange(Review review, MultipartFile newImage, boolean removeImage) {
        if (newImage != null) {
            String oldImagePath = review.getImagePath();
            String newImagePath = validateAndUploadImage(newImage);
            review.updateImagePath(newImagePath);
            deleteImageAfterCommit(oldImagePath);
        } else if (removeImage) {
            deleteImageAfterCommit(review.getImagePath());
            review.updateImagePath(null);
        }
    }

    private void deleteImageAfterCommit(String imagePath) {
        if (imagePath == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteImageQuietly(imagePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteImageQuietly(imagePath);
            }
        });
    }

    private void deleteImageQuietly(String imagePath) {
        try {
            storageService.delete(imagePath);
        } catch (RuntimeException e) {
            log.warn("Failed to delete review image from storage. key={}", imagePath, e);
        }
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

    // request.image 파트가 2개 이상 전송되면 INVALID_INPUT — 프론트는 단일 파일 input이지만
    // API 자체는 멀티파트라 클라이언트가 임의로 여러 개 보낼 수 있다(api.md §API 1 Validation).
    private MultipartFile singleImageOrNull(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        if (images.size() > 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        MultipartFile file = images.get(0);
        return file.isEmpty() ? null : file;
    }

    private String validateAndUploadImage(MultipartFile image) {
        if (image == null) {
            return null;
        }
        imageValidator.validate(image);
        String key = "reviews/" + UUID.randomUUID() + "-" + sanitizeFilename(image.getOriginalFilename());
        storageService.upload(key, image);
        return key;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
