package com.example.honorcitizen.domain.review.dto;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.domain.review.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ReviewDetailResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final String authorName;
    private final ApplicationType applicationType;
    private final CardTypeSummaryResponse cardType;
    // imageUrl은 대표(첫) 이미지 — 단일 이미지 시절 클라이언트 호환용. images가 전체 목록(id 포함)이다.
    private final String imageUrl;
    private final List<ReviewImageResponse> images;
    private final LocalDateTime createdAt;
    private final NextReview next;
    private final boolean canEdit;
    private final boolean canDelete;

    private ReviewDetailResponse(Long id, String title, String content, String authorName,
            ApplicationType applicationType, CardTypeSummaryResponse cardType, String imageUrl,
            List<ReviewImageResponse> images, LocalDateTime createdAt, NextReview next, boolean canEdit, boolean canDelete) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.applicationType = applicationType;
        this.cardType = cardType;
        this.imageUrl = imageUrl;
        this.images = images;
        this.createdAt = createdAt;
        this.next = next;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }

    public static ReviewDetailResponse of(Review review, CardTypeSummaryResponse cardType,
            List<ReviewImageResponse> images, NextReview next, boolean canEdit, boolean canDelete) {
        List<ReviewImageResponse> safe = images == null ? List.of() : images;
        String primary = safe.isEmpty() ? null : safe.get(0).getImageUrl();
        return new ReviewDetailResponse(review.getId(), review.getTitle(), review.getContent(),
                review.getAuthorDisplayName(), review.getApplicationType(), cardType, primary,
                safe, review.getCreatedAt(), next, canEdit, canDelete);
    }

    @Getter
    public static class NextReview {
        private final Long id;
        private final String title;

        private NextReview(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public static NextReview from(Review review) {
            return new NextReview(review.getId(), review.getTitle());
        }
    }
}
