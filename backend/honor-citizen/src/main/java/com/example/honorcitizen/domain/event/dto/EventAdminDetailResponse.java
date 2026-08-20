package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.domain.event.entity.EventPost;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 관리자 상세(api.md API 7) — 공개 상세와 동일한 필드 + visible(숨긴 글도 조회 가능해야 하므로).
@Getter
public class EventAdminDetailResponse {

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
    // 프론트 FeedPost(data/eventFeedPosts.ts)가 이미 쓰는 필드명(company/logoUrl)에 맞춘다.
    private final String company;
    private final String logoUrl;
    private final boolean visible;
    private final Integer displayOrder;
    private final List<EventImageResponse> images;

    private EventAdminDetailResponse(Long id, EventType eventType, String title, LocalDate eventDate,
            String eventDateText, String place, String host, String cardLabel, String content,
            String thumbnailImageUrl, String company, String logoUrl, boolean visible,
            Integer displayOrder, List<EventImageResponse> images) {
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
        this.visible = visible;
        this.displayOrder = displayOrder;
        this.images = images;
    }

    public static EventAdminDetailResponse of(EventPost eventPost, String thumbnailImageUrl, String logoUrl,
            List<EventImageResponse> images) {
        return new EventAdminDetailResponse(eventPost.getId(), eventPost.getEventType(), eventPost.getTitle(),
                eventPost.getEventDate(), eventPost.getEventDateText(), eventPost.getPlace(), eventPost.getHost(),
                eventPost.getCardLabel(), eventPost.getContent(), thumbnailImageUrl, eventPost.getCompanyName(),
                logoUrl, eventPost.isVisible(), eventPost.getDisplayOrder(), images);
    }
}
