package com.example.honorcitizen.domain.event.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.EventType;
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

    @Column(nullable = false)
    private boolean visible;

    private Integer displayOrder;

    public static EventPost create(EventType eventType, String title, LocalDate eventDate, String eventDateText,
            String place, String host, String cardLabel, String content, String thumbnailImagePath,
            boolean visible, Integer displayOrder) {
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
        eventPost.visible = visible;
        eventPost.displayOrder = displayOrder;
        return eventPost;
    }

    // 전체 재제출(api.md §API 4) — 썸네일 교체는 별도 updateThumbnailImagePath로 처리한다.
    public void update(EventType eventType, String title, LocalDate eventDate, String eventDateText, String place,
            String host, String cardLabel, String content, boolean visible, Integer displayOrder) {
        this.eventType = eventType;
        this.title = title;
        this.eventDate = eventDate;
        this.eventDateText = eventDateText;
        this.place = place;
        this.host = host;
        this.cardLabel = cardLabel;
        this.content = content;
        this.visible = visible;
        this.displayOrder = displayOrder;
    }

    public void updateThumbnailImagePath(String thumbnailImagePath) {
        this.thumbnailImagePath = thumbnailImagePath;
    }
}
