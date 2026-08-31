package com.example.honorcitizen.domain.review.dto;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.domain.review.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewListItemResponse {

    private final Long id;
    private final String imageUrl;
    private final ApplicationType applicationType;
    private final CardTypeSummaryResponse cardType;
    private final String title;
    private final String content;
    private final String authorName;
    private final LocalDateTime createdAt;

    private ReviewListItemResponse(Long id, String imageUrl, ApplicationType applicationType,
            CardTypeSummaryResponse cardType, String title, String content, String authorName,
            LocalDateTime createdAt) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.applicationType = applicationType;
        this.cardType = cardType;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    // imageUrl(presigned)과 cardType({id,name})은 배치 조회 결과라 Review 엔티티 자체에서 얻을 수 없어 인자로 받는다.
    public static ReviewListItemResponse of(Review review, CardTypeSummaryResponse cardType, String imageUrl) {
        return new ReviewListItemResponse(review.getId(), imageUrl, review.getApplicationType(), cardType,
                review.getTitle(), review.getContent(), review.getAuthorDisplayName(), review.getCreatedAt());
    }

    // 영어 응답용 사본 — authorName(사람 이름)은 번역하지 않는다.
    public ReviewListItemResponse withTranslated(String title, String content) {
        return new ReviewListItemResponse(id, imageUrl, applicationType, cardType, title, content,
                authorName, createdAt);
    }
}
