package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.dto.ReviewDetailResponse;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReviewServiceDetailTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private CardType cardType;
    private User owner;
    private User admin;
    private User stranger;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-detail", null, BigDecimal.valueOf(30000)));
        owner = userRepository.save(User.createOAuthUser("owner@example.com", "oauth-owner", "google", "작성자"));
        admin = userRepository.save(User.createOAuthUser("admin@example.com", "oauth-admin", "google", "관리자"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        admin = userRepository.save(admin);
        stranger = userRepository.save(User.createOAuthUser("stranger@example.com", "oauth-stranger", "google", "타인"));

        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://mock/presigned");
    }

    @Test
    void returnsDetailWithoutImageAndWithoutNext() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        ReviewDetailResponse response = reviewService.detail(review.getId(), null);

        assertThat(response.getId()).isEqualTo(review.getId());
        assertThat(response.getTitle()).isEqualTo("제목");
        assertThat(response.getContent()).isEqualTo("내용");
        assertThat(response.getAuthorName()).isEqualTo("홍길동");
        assertThat(response.getApplicationType()).isEqualTo(ApplicationType.INDIVIDUAL);
        assertThat(response.getCardType().getId()).isEqualTo(cardType.getId());
        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getNext()).isNull();
    }

    @Test
    void returnsPresignedImageUrlWhenImagePresent() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", "reviews/x.jpg"));

        ReviewDetailResponse response = reviewService.detail(review.getId(), null);

        assertThat(response.getImageUrl()).isEqualTo("https://mock/presigned");
    }

    @Test
    void returnsNextAsTheImmediatelyOlderReview() {
        Review older = reviewRepository.save(Review.create(owner.getId(), "홍길동", "오래된 글",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));
        Review newer = reviewRepository.save(Review.create(owner.getId(), "홍길동", "새 글",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        ReviewDetailResponse response = reviewService.detail(newer.getId(), null);

        assertThat(response.getNext().getId()).isEqualTo(older.getId());
        assertThat(response.getNext().getTitle()).isEqualTo("오래된 글");
    }

    @Test
    void canEditAndCanDeleteAreTrueForOwner() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        ReviewDetailResponse response = reviewService.detail(review.getId(), owner.getId());

        assertThat(response.isCanEdit()).isTrue();
        assertThat(response.isCanDelete()).isTrue();
    }

    @Test
    void canEditAndCanDeleteAreTrueForAdminEvenIfNotOwner() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        ReviewDetailResponse response = reviewService.detail(review.getId(), admin.getId());

        assertThat(response.isCanEdit()).isTrue();
        assertThat(response.isCanDelete()).isTrue();
    }

    @Test
    void canEditAndCanDeleteAreFalseForOtherLoggedInUser() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        ReviewDetailResponse response = reviewService.detail(review.getId(), stranger.getId());

        assertThat(response.isCanEdit()).isFalse();
        assertThat(response.isCanDelete()).isFalse();
    }

    @Test
    void canEditAndCanDeleteAreFalseWhenAnonymous() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        ReviewDetailResponse response = reviewService.detail(review.getId(), null);

        assertThat(response.isCanEdit()).isFalse();
        assertThat(response.isCanDelete()).isFalse();
    }

    @Test
    void throwsNotFoundForUnknownId() {
        assertThatThrownBy(() -> reviewService.detail(999999L, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }
}
