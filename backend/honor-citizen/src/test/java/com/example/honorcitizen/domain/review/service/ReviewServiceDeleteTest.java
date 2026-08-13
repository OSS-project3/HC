package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ReviewServiceDeleteTest {

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
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-delete", null, BigDecimal.valueOf(30000)));
        owner = userRepository.save(User.createNewUser("owner-del@example.com", "oauth-owner-del", "google", "작성자"));
        admin = userRepository.save(User.createNewUser("admin-del@example.com", "oauth-admin-del", "google", "관리자"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        admin = userRepository.save(admin);
        stranger = userRepository.save(User.createNewUser("stranger-del@example.com", "oauth-stranger-del", "google", "타인"));
    }

    @Test
    void ownerCanDeleteOwnReviewAndImageIsCleanedUp() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", "reviews/to-delete.jpg"));

        reviewService.delete(review.getId(), owner.getId());

        assertThat(reviewRepository.findById(review.getId())).isEmpty();
        verify(storageService).delete("reviews/to-delete.jpg");
    }

    @Test
    void deletingReviewWithoutImageDoesNotCallStorageDelete() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        reviewService.delete(review.getId(), owner.getId());

        assertThat(reviewRepository.findById(review.getId())).isEmpty();
        verify(storageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void adminCanDeleteOtherUsersReview() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        reviewService.delete(review.getId(), admin.getId());

        assertThat(reviewRepository.findById(review.getId())).isEmpty();
    }

    @Test
    void rejectsDeleteByNeitherOwnerNorAdmin() {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        assertThatThrownBy(() -> reviewService.delete(review.getId(), stranger.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        assertThat(reviewRepository.findById(review.getId())).isPresent();
    }

    @Test
    void rejectsUnknownId() {
        assertThatThrownBy(() -> reviewService.delete(999999L, owner.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }
}
