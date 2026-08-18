package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.dto.ReviewUpdateRequest;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReviewServiceUpdateTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private ApplicationRepository applicationRepository;

    @MockitoBean
    private StorageService storageService;

    private CardType cardTypeA;
    private CardType cardTypeB;
    private User owner;
    private User admin;
    private User stranger;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        cardTypeA = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-upd", null, BigDecimal.valueOf(30000)));
        cardTypeB = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_CITIZEN, "명예시민증-upd", null, BigDecimal.valueOf(30000)));

        owner = userRepository.save(User.createNewUser("owner-upd@example.com", "oauth-owner-upd", "google", "작성자"));
        admin = userRepository.save(User.createNewUser("admin-upd@example.com", "oauth-admin-upd", "google", "관리자"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        admin = userRepository.save(admin);
        stranger = userRepository.save(User.createNewUser("stranger-upd@example.com", "oauth-stranger-upd", "google", "타인"));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://mock/presigned");
    }

    private void grantEligibility(User user, ApplicationType type, CardType cardType) {
        Application application = applicationRepository.save(Application.createIndividual(
                999L, "APP-2026-" + System.nanoTime() % 900000, cardType.getId(), IssueType.MOBILE, true, null, null));
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), user.getName(), user.getEmail(), "010-1111-2222"));
        if (type == ApplicationType.GROUP) {
            // 개인 신청으로 단체 자격을 만들 수는 없으므로 GROUP이 필요하면 별도로 호출측에서 처리
        }
    }

    private ReviewUpdateRequest request(String title, ApplicationType type, Long cardTypeId, boolean removeImage) {
        return new ReviewUpdateRequest(title, type, cardTypeId, "새 작성자명", "새 내용", removeImage);
    }

    @Test
    void ownerCanUpdateOwnReviewFields() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "원래 제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "원래 내용", null));

        reviewService.update(review.getId(), owner.getId(),
                request("새 제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), null);

        Review updated = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getAuthorDisplayName()).isEqualTo("새 작성자명");
        assertThat(updated.getContent()).isEqualTo("새 내용");
    }

    @Test
    void adminCanUpdateOtherUsersReviewValidatedAgainstOriginalAuthor() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "원래 제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "원래 내용", null));

        reviewService.update(review.getId(), admin.getId(),
                request("관리자가 수정", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), null);

        Review updated = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("관리자가 수정");
    }

    @Test
    void rejectsUpdateByNeitherOwnerNorAdmin() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "원래 제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "원래 내용", null));

        assertThatThrownBy(() -> reviewService.update(review.getId(), stranger.getId(),
                request("탈취", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsUnknownReviewId() {
        assertThatThrownBy(() -> reviewService.update(999999L, owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void rejectsUnknownCardType() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "원래 제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "원래 내용", null));

        assertThatThrownBy(() -> reviewService.update(review.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, 999999L, false), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsWhenChangedCombinationNotEligible() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "원래 제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "원래 내용", null));

        // cardTypeB에 대한 자격이 없음
        assertThatThrownBy(() -> reviewService.update(review.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeB.getId(), false), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void rejectsWhenChangedCombinationAlreadyUsedByAnotherReviewOfSameAuthor() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeB);
        reviewRepository.save(Review.create(owner.getId(), "홍길동", "이미 있는 후기",
                ApplicationType.INDIVIDUAL, cardTypeB.getId(), "내용", null));
        Review target = reviewRepository.save(Review.create(owner.getId(), "홍길동", "수정 대상",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "내용", null));

        assertThatThrownBy(() -> reviewService.update(target.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeB.getId(), false), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void allowsKeepingSameCombinationOnSameReview() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "원래 제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "원래 내용", null));

        reviewService.update(review.getId(), owner.getId(),
                request("새 제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), null);

        assertThat(reviewRepository.findById(review.getId()).orElseThrow().getTitle()).isEqualTo("새 제목");
    }

    @Test
    void replacesImageAndDeletesOldOneAfterCommit() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "내용", "reviews/old.jpg"));
        MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", jpegBytes());

        reviewService.update(review.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), List.of(newImage));

        Review updated = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(updated.getImagePath()).startsWith("reviews/").endsWith("-new.jpg");
        verify(storageService).delete("reviews/old.jpg");
    }

    @Test
    void removesImageWhenRemoveImageTrueAndNoNewFile() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "내용", "reviews/old.jpg"));

        reviewService.update(review.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), true), null);

        Review updated = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(updated.getImagePath()).isNull();
        verify(storageService).delete("reviews/old.jpg");
    }

    @Test
    void keepsImageWhenNoNewFileAndRemoveImageFalse() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "내용", "reviews/keep.jpg"));

        reviewService.update(review.getId(), owner.getId(),
                request("새 제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), null);

        Review updated = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(updated.getImagePath()).isEqualTo("reviews/keep.jpg");
        verify(storageService, never()).delete(anyString());
    }

    @Test
    void rejectsImageAndRemoveImageTogether() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "내용", "reviews/old.jpg"));
        MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", jpegBytes());

        assertThatThrownBy(() -> reviewService.update(review.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), true), List.of(newImage)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsMoreThanOneImagePart() {
        grantEligibility(owner, ApplicationType.INDIVIDUAL, cardTypeA);
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "내용", null));
        MockMultipartFile first = new MockMultipartFile("image", "a.jpg", "image/jpeg", jpegBytes());
        MockMultipartFile second = new MockMultipartFile("image", "b.jpg", "image/jpeg", jpegBytes());

        assertThatThrownBy(() -> reviewService.update(review.getId(), owner.getId(),
                request("제목", ApplicationType.INDIVIDUAL, cardTypeA.getId(), false), List.of(first, second)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    private byte[] jpegBytes() {
        try {
            var image = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var output = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
