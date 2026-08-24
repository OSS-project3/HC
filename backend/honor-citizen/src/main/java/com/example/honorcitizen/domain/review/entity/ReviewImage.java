package com.example.honorcitizen.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 후기 첨부 이미지(여러 장). UploadFile을 경유하지 않고 S3 key를 직접 저장한다 — EventImage와 동일한 직접
// 저장 패턴. Review.imagePath는 목록 썸네일용으로 displayOrder 0번(대표) 이미지 경로를 비정규화해 유지한다.
@Entity
@Table(name = "review_images", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "display_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    private String originalFilename;

    // 0부터 시작하는 갤러리 노출 순서 — 첨부 전송 순서대로 채운다.
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ReviewImage create(Long reviewId, String imagePath, String originalFilename, int displayOrder) {
        ReviewImage reviewImage = new ReviewImage();
        reviewImage.reviewId = reviewId;
        reviewImage.imagePath = imagePath;
        reviewImage.originalFilename = originalFilename;
        reviewImage.displayOrder = displayOrder;
        reviewImage.createdAt = LocalDateTime.now();
        return reviewImage;
    }

    // 갤러리 편집(keepImageIds 재정렬) 전용 — id는 그대로 두고 순서만 바꾼다.
    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
