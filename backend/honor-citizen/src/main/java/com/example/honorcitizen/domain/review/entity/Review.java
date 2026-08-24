package com.example.honorcitizen.domain.review.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.ApplicationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카드 발급 경험 후기. applicationType/cardTypeId는 실제 Application 레코드에 FK로 연결하지 않는
// 자기 신고(self-report) 값이며, 등록·수정 시점에 로그인 사용자의 실제 신청 이력과 대조 검증한다
// (ReviewEligibilityService 참고). 카드종류·사진이 단일값이라 arch.md §5.1 원칙(모듈 간 영속 참조는
// Long ...Id)에 따라 cardTypeId도 Application.cardTypeId와 동일하게 ID로 직접 저장한다.
@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 실제 작성 계정. author_display_name(화면 표시용)과 별개 — 수정/삭제 권한 판단 기준.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "author_display_name", nullable = false, length = 50)
    private String authorDisplayName;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_type", nullable = false, length = 20)
    private ApplicationType applicationType;

    @Column(name = "card_type_id", nullable = false)
    private Long cardTypeId;

    // 대표(썸네일) 이미지의 S3 key — 목록 썸네일·hasPhoto 필터용으로 review_images 0번(첫) 이미지 경로를
    // 비정규화해 유지한다(전체 이미지는 ReviewImage). UploadFile을 거치지 않는다. 사진 없으면 null.
    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static Review create(Long userId, String authorDisplayName, String title,
            ApplicationType applicationType, Long cardTypeId, String content, String imagePath) {
        Review review = new Review();
        review.userId = userId;
        review.authorDisplayName = authorDisplayName;
        review.title = title;
        review.applicationType = applicationType;
        review.cardTypeId = cardTypeId;
        review.content = content;
        review.imagePath = imagePath;
        return review;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    // PATCH — 프론트가 폼 전체를 매번 재제출하므로 부분수정이 아니라 전체 갱신이다(image_path 제외 — 별도 메서드).
    public void update(String authorDisplayName, String title, ApplicationType applicationType,
            Long cardTypeId, String content) {
        this.authorDisplayName = authorDisplayName;
        this.title = title;
        this.applicationType = applicationType;
        this.cardTypeId = cardTypeId;
        this.content = content;
    }

    public void updateImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
