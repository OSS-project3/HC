package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.ReviewSearchType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.dto.ReviewListItemResponse;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReviewServiceListTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;

    @MockitoBean
    private StorageService storageService;

    private CardType cardTypeA;
    private CardType cardTypeB;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        cardTypeRepository.deleteAll();

        cardTypeA = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-list", null, BigDecimal.valueOf(30000)));
        cardTypeB = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_CITIZEN, "명예시민증-list", null, BigDecimal.valueOf(30000)));

        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://mock/presigned");

        reviewRepository.save(Review.create(1L, "홍길동", "한국 여행 후기",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "정말 좋았습니다", "reviews/a.jpg"));
        reviewRepository.save(Review.create(2L, "김철수", "가족과 함께한 시간",
                ApplicationType.GROUP, cardTypeB.getId(), "행사에 참여했어요", null));
        reviewRepository.save(Review.create(3L, "이영희", "특별한 기억",
                ApplicationType.INDIVIDUAL, cardTypeA.getId(), "한국 문화를 체험했습니다", null));
    }

    @Test
    void returnsAllReviewsSortedByCreatedAtDesc() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(null, null, null, null, 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getContent()).extracting(ReviewListItemResponse::getTitle)
                .containsExactly("특별한 기억", "가족과 함께한 시간", "한국 여행 후기");
    }

    @Test
    void filtersByCardTypeId() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(cardTypeA.getId(), null, null, null, 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).allSatisfy(item ->
                assertThat(item.getCardType().getId()).isEqualTo(cardTypeA.getId()));
    }

    @Test
    void filtersByHasPhotoTrue() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(null, true, null, null, 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("한국 여행 후기");
        assertThat(response.getContent().get(0).getImageUrl()).isEqualTo("https://mock/presigned");
    }

    @Test
    void filtersByHasPhotoFalse() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(null, false, null, null, 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).allSatisfy(item -> assertThat(item.getImageUrl()).isNull());
    }

    @Test
    void searchTypeTitleMatchesOnlyTitleField() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(
                null, null, ReviewSearchType.TITLE, "한국", 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("한국 여행 후기");
    }

    @Test
    void searchTypeAllMatchesContentToo() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(
                null, null, ReviewSearchType.ALL, "한국", 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void keywordWithoutSearchTypeDefaultsToAll() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(null, null, null, "한국", 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchTypeWithoutKeywordAppliesNoFilter() {
        PageResponse<ReviewListItemResponse> response = reviewService.list(
                null, null, ReviewSearchType.TITLE, null, 0, 9);

        assertThat(response.getTotalElements()).isEqualTo(3);
    }

    @Test
    void rejectsUnknownCardTypeFilter() {
        assertThatThrownBy(() -> reviewService.list(999999L, null, null, null, 0, 9))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> reviewService.list(null, null, null, null, -1, 9))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsSizeAboveHundred() {
        assertThatThrownBy(() -> reviewService.list(null, null, null, null, 0, 101))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsZeroSize() {
        assertThatThrownBy(() -> reviewService.list(null, null, null, null, 0, 0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
