package com.example.honorcitizen.domain.event.entity;

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

// EventPost 상세 갤러리 이미지. UploadFile을 경유하지 않고 S3 key를 직접 저장한다
// (Board의 BoardAttachment=join 엔티티와 달리 Review.imagePath와 동일한 직접 저장 패턴, data-model.md §2).
@Entity
@Table(name = "event_images", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_post_id", "display_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_post_id", nullable = false)
    private Long eventPostId;

    @Column(nullable = false, length = 500)
    private String imagePath;

    private String originalFilename;

    // 0부터 시작하는 갤러리 노출 순서 — 첨부 전송 순서대로 채운다.
    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static EventImage create(Long eventPostId, String imagePath, String originalFilename, int displayOrder) {
        EventImage eventImage = new EventImage();
        eventImage.eventPostId = eventPostId;
        eventImage.imagePath = imagePath;
        eventImage.originalFilename = originalFilename;
        eventImage.displayOrder = displayOrder;
        eventImage.createdAt = LocalDateTime.now();
        return eventImage;
    }
}
