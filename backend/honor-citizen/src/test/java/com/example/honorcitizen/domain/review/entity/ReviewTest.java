package com.example.honorcitizen.domain.review.entity;

import com.example.honorcitizen.common.enums.ApplicationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTest {

    @Test
    void createSetsAllFields() {
        Review review = Review.create(1L, "홍길동", "제목", ApplicationType.INDIVIDUAL, 2L, "내용", "path/to/image.jpg");

        assertThat(review.getUserId()).isEqualTo(1L);
        assertThat(review.getAuthorDisplayName()).isEqualTo("홍길동");
        assertThat(review.getTitle()).isEqualTo("제목");
        assertThat(review.getApplicationType()).isEqualTo(ApplicationType.INDIVIDUAL);
        assertThat(review.getCardTypeId()).isEqualTo(2L);
        assertThat(review.getContent()).isEqualTo("내용");
        assertThat(review.getImagePath()).isEqualTo("path/to/image.jpg");
    }

    @Test
    void createAllowsNullImagePath() {
        Review review = Review.create(1L, "홍길동", "제목", ApplicationType.INDIVIDUAL, 2L, "내용", null);

        assertThat(review.getImagePath()).isNull();
    }

    @Test
    void isOwnedByComparesUserId() {
        Review review = Review.create(1L, "홍길동", "제목", ApplicationType.INDIVIDUAL, 2L, "내용", null);

        assertThat(review.isOwnedBy(1L)).isTrue();
        assertThat(review.isOwnedBy(2L)).isFalse();
    }

    @Test
    void updateOverwritesFieldsExceptImagePath() {
        Review review = Review.create(1L, "홍길동", "제목", ApplicationType.INDIVIDUAL, 2L, "내용", "old-path.jpg");

        review.update("김철수", "새 제목", ApplicationType.GROUP, 3L, "새 내용");

        assertThat(review.getAuthorDisplayName()).isEqualTo("김철수");
        assertThat(review.getTitle()).isEqualTo("새 제목");
        assertThat(review.getApplicationType()).isEqualTo(ApplicationType.GROUP);
        assertThat(review.getCardTypeId()).isEqualTo(3L);
        assertThat(review.getContent()).isEqualTo("새 내용");
        assertThat(review.getImagePath()).isEqualTo("old-path.jpg");
    }

    @Test
    void updateImagePathCanClearToNull() {
        Review review = Review.create(1L, "홍길동", "제목", ApplicationType.INDIVIDUAL, 2L, "내용", "old-path.jpg");

        review.updateImagePath(null);

        assertThat(review.getImagePath()).isNull();
    }
}
