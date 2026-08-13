package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.dto.ReviewCreateRequest;
import com.example.honorcitizen.domain.review.dto.ReviewCreateResponse;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReviewServiceCreateTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private User user;
    private CardType cardType;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-create", null, BigDecimal.valueOf(30000)));
        user = userRepository.save(User.createNewUser("writer@example.com", "oauth-writer", "google", "작성자"));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    private void grantEligibility() {
        Application application = applicationRepository.save(Application.createIndividual(
                999L, "APP-2026-900001", cardType.getId(), IssueType.MOBILE, true, null, null));
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.startProducing();
        application.complete();
        applicationRepository.save(application);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "작성자", "writer@example.com", "010-1111-2222"));
    }

    private ReviewCreateRequest request() {
        return new ReviewCreateRequest("제목", ApplicationType.INDIVIDUAL, cardType.getId(), "홍길동", "내용입니다.");
    }

    @Test
    void createsReviewWithoutImage() {
        grantEligibility();

        ReviewCreateResponse response = reviewService.create(user.getId(), request(), null);

        Review saved = reviewRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getTitle()).isEqualTo("제목");
        assertThat(saved.getAuthorDisplayName()).isEqualTo("홍길동");
        assertThat(saved.getImagePath()).isNull();
        verify(storageService, never()).upload(anyString(), any());
    }

    @Test
    void createsReviewWithImageAndUploadsToStorage() {
        grantEligibility();
        MockMultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", jpegBytes());

        ReviewCreateResponse response = reviewService.create(user.getId(), request(), List.of(image));

        Review saved = reviewRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getImagePath()).startsWith("reviews/").endsWith("-photo.jpg");
        verify(storageService).upload(anyString(), any());
    }

    @Test
    void rejectsUnknownCardType() {
        grantEligibility();
        ReviewCreateRequest request = new ReviewCreateRequest(
                "제목", ApplicationType.INDIVIDUAL, 999999L, "홍길동", "내용");

        assertThatThrownBy(() -> reviewService.create(user.getId(), request, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsWhenNotEligible() {
        assertThatThrownBy(() -> reviewService.create(user.getId(), request(), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void rejectsWhenReviewForSameCombinationAlreadyExists() {
        grantEligibility();
        reviewRepository.save(Review.create(user.getId(), "홍길동", "기존 후기",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        assertThatThrownBy(() -> reviewService.create(user.getId(), request(), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void rejectsMoreThanOneImagePart() {
        grantEligibility();
        MockMultipartFile first = new MockMultipartFile("image", "a.jpg", "image/jpeg", jpegBytes());
        MockMultipartFile second = new MockMultipartFile("image", "b.jpg", "image/jpeg", jpegBytes());

        assertThatThrownBy(() -> reviewService.create(user.getId(), request(), List.of(first, second)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsInvalidImage() {
        grantEligibility();
        MockMultipartFile invalid = new MockMultipartFile("image", "a.jpg", "image/jpeg", "not-image".getBytes());

        assertThatThrownBy(() -> reviewService.create(user.getId(), request(), List.of(invalid)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE_FILE);
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
