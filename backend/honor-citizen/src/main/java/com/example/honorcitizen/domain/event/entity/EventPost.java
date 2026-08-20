package com.example.honorcitizen.domain.event.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
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

import java.time.LocalDate;

// 행사사업 부스 운영/법인·단체 협업 기록. EventType enum 하나로 통합 관리 —
// arch.md §5.1 원칙에 따라 다른 도메인과 JPA 연관관계를 두지 않는다(카드 종류는 card_label 자유 텍스트로만 표시).
@Entity
@Table(name = "event_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType eventType;

    @Column(nullable = false)
    private String title;

    // 정렬·관리 기준 날짜. 화면 표시는 eventDateText를 쓴다.
    private LocalDate eventDate;

    @Column(nullable = false, length = 50)
    private String eventDateText;

    @Column(nullable = false)
    private String place;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private String cardLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // UploadFile을 경유하지 않고 S3 key를 직접 저장한다(Review.imagePath와 동일 패턴).
    @Column(length = 500)
    private String thumbnailImagePath;

    // ✅ 2026-08-21 추가 — COLLABORATION 전용 선택 필드(EVENT-EXT-1). host(행사 주최)와는 다른 개념이라
    // 별도 컬럼으로 둔다. BOOTH는 이 필드를 절대 갖지 않는다(validateCollaborationOnlyFields로 강제).
    @Column(length = 100)
    private String companyName;

    // ✅ 2026-08-21 추가 — 협업 로고. thumbnailImagePath(카드 대표 이미지)·EventImage(상세 갤러리)와는
    // 별도 파일 역할이다. companyName과 동일하게 COLLABORATION 전용.
    @Column(length = 500)
    private String logoImagePath;

    private static final int MAX_COMPANY_NAME_LENGTH = 100;

    @Column(nullable = false)
    private boolean visible;

    private Integer displayOrder;

    public static EventPost create(EventType eventType, String title, LocalDate eventDate, String eventDateText,
            String place, String host, String cardLabel, String content, String thumbnailImagePath,
            String companyName, String logoImagePath, boolean visible, Integer displayOrder) {
        String normalizedCompanyName = normalizeCompanyName(companyName);
        validateCollaborationOnlyFields(eventType, normalizedCompanyName, logoImagePath);

        EventPost eventPost = new EventPost();
        eventPost.eventType = eventType;
        eventPost.title = title;
        eventPost.eventDate = eventDate;
        eventPost.eventDateText = eventDateText;
        eventPost.place = place;
        eventPost.host = host;
        eventPost.cardLabel = cardLabel;
        eventPost.content = content;
        eventPost.thumbnailImagePath = thumbnailImagePath;
        eventPost.companyName = normalizedCompanyName;
        eventPost.logoImagePath = logoImagePath;
        eventPost.visible = visible;
        eventPost.displayOrder = displayOrder;
        return eventPost;
    }

    // 전체 재제출(api.md §API 4) — 썸네일·로고 교체는 별도 updateThumbnailImagePath/updateLogoImagePath로
    // 처리한다. 이 메서드는 companyName까지만 반영하고, BOOTH 전환 시 "로고가 아직 안 지워졌다"는
    // 잔여 상태 검사는 Service가 로고 교체/삭제를 먼저 적용한 뒤 assertCollaborationInvariant()로
    // 최종 상태를 한 번에 검증한다(호출 순서에 의존하지 않기 위함).
    public void update(EventType eventType, String title, LocalDate eventDate, String eventDateText, String place,
            String host, String cardLabel, String content, String companyName, boolean visible, Integer displayOrder) {
        String normalizedCompanyName = normalizeCompanyName(companyName);
        if (normalizedCompanyName != null && normalizedCompanyName.length() > MAX_COMPANY_NAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        this.eventType = eventType;
        this.title = title;
        this.eventDate = eventDate;
        this.eventDateText = eventDateText;
        this.place = place;
        this.host = host;
        this.cardLabel = cardLabel;
        this.content = content;
        this.companyName = normalizedCompanyName;
        this.visible = visible;
        this.displayOrder = displayOrder;
    }

    public void updateThumbnailImagePath(String thumbnailImagePath) {
        this.thumbnailImagePath = thumbnailImagePath;
    }

    public void updateLogoImagePath(String logoImagePath) {
        this.logoImagePath = logoImagePath;
    }

    // update() + updateLogoImagePath() 호출이 모두 끝난 뒤 Service가 한 번 호출해 최종 상태를 검증한다
    // (EVENT-EXT-3 "COLLABORATION → BOOTH 전환 시 남은 협업 데이터가 있으면 INVALID_INPUT").
    public void assertCollaborationInvariant() {
        validateCollaborationOnlyFields(this.eventType, this.companyName, this.logoImagePath);
    }

    private static String normalizeCompanyName(String companyName) {
        if (companyName == null) {
            return null;
        }
        String trimmed = companyName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void validateCollaborationOnlyFields(EventType eventType, String companyName, String logoImagePath) {
        if (eventType != EventType.COLLABORATION && (companyName != null || logoImagePath != null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (companyName != null && companyName.length() > MAX_COMPANY_NAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
