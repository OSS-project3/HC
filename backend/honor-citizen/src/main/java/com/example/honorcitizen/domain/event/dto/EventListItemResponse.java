package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.domain.event.entity.EventPost;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class EventListItemResponse {

    private final Long id;
    private final EventType eventType;
    private final String title;
    private final LocalDate eventDate;
    private final String eventDateText;
    private final String place;
    private final String host;
    private final String cardLabel;
    private final String content;
    private final String thumbnailImageUrl;
    // 프론트 FeedPost(data/eventFeedPosts.ts)가 이미 쓰는 필드명(company/logoUrl)에 맞춘다 — 엔티티
    // 컬럼명(company_name/logo_image_path)은 내부 구현이라 그대로 두고 API 계약만 프론트에 맞춘다.
    private final String company;
    private final String logoUrl;
    private final Integer displayOrder;

    private EventListItemResponse(Long id, EventType eventType, String title, LocalDate eventDate,
            String eventDateText, String place, String host, String cardLabel, String content,
            String thumbnailImageUrl, String company, String logoUrl, Integer displayOrder) {
        this.id = id;
        this.eventType = eventType;
        this.title = title;
        this.eventDate = eventDate;
        this.eventDateText = eventDateText;
        this.place = place;
        this.host = host;
        this.cardLabel = cardLabel;
        this.content = content;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.company = company;
        this.logoUrl = logoUrl;
        this.displayOrder = displayOrder;
    }

    public static EventListItemResponse of(EventPost eventPost, String thumbnailImageUrl, String logoUrl) {
        return new EventListItemResponse(eventPost.getId(), eventPost.getEventType(), eventPost.getTitle(),
                eventPost.getEventDate(), eventPost.getEventDateText(), eventPost.getPlace(), eventPost.getHost(),
                eventPost.getCardLabel(), eventPost.getContent(), thumbnailImageUrl, eventPost.getCompanyName(),
                logoUrl, eventPost.getDisplayOrder());
    }
}
