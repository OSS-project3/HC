package com.example.honorcitizen.domain.event.entity;

import com.example.honorcitizen.common.enums.EventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EventPostTest {

    @Test
    void createSetsAllFields() {
        EventPost eventPost = EventPost.create(EventType.BOOTH, "서울공예트렌드페어", LocalDate.of(2026, 12, 1),
                "2026. 12", "서울 코엑스 Hall C", "한국공예·디자인문화진흥원", "명예한국인증 · 방문증",
                "부스를 찾은 방문객에게...", "events/thumbnails/a.webp", true, null);

        assertThat(eventPost.getEventType()).isEqualTo(EventType.BOOTH);
        assertThat(eventPost.getTitle()).isEqualTo("서울공예트렌드페어");
        assertThat(eventPost.getEventDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(eventPost.getEventDateText()).isEqualTo("2026. 12");
        assertThat(eventPost.getPlace()).isEqualTo("서울 코엑스 Hall C");
        assertThat(eventPost.getHost()).isEqualTo("한국공예·디자인문화진흥원");
        assertThat(eventPost.getCardLabel()).isEqualTo("명예한국인증 · 방문증");
        assertThat(eventPost.getContent()).isEqualTo("부스를 찾은 방문객에게...");
        assertThat(eventPost.getThumbnailImagePath()).isEqualTo("events/thumbnails/a.webp");
        assertThat(eventPost.isVisible()).isTrue();
        assertThat(eventPost.getDisplayOrder()).isNull();
    }

    @Test
    void updateOverwritesFieldsButNotThumbnail() {
        EventPost eventPost = EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "원래 장소", "원래 주최", "원래 카드", "원래 내용", "events/thumbnails/old.webp", true, null);

        eventPost.update(EventType.COLLABORATION, "새 제목", LocalDate.of(2026, 3, 1), "2026. 03",
                "새 장소", "새 주최", "새 카드", "새 내용", false, 2);

        assertThat(eventPost.getEventType()).isEqualTo(EventType.COLLABORATION);
        assertThat(eventPost.getTitle()).isEqualTo("새 제목");
        assertThat(eventPost.getEventDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(eventPost.getEventDateText()).isEqualTo("2026. 03");
        assertThat(eventPost.getPlace()).isEqualTo("새 장소");
        assertThat(eventPost.getHost()).isEqualTo("새 주최");
        assertThat(eventPost.getCardLabel()).isEqualTo("새 카드");
        assertThat(eventPost.getContent()).isEqualTo("새 내용");
        assertThat(eventPost.isVisible()).isFalse();
        assertThat(eventPost.getDisplayOrder()).isEqualTo(2);
        assertThat(eventPost.getThumbnailImagePath()).isEqualTo("events/thumbnails/old.webp");
    }

    @Test
    void updateThumbnailImagePathOverwritesOnlyThumbnail() {
        EventPost eventPost = EventPost.create(EventType.BOOTH, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", "events/thumbnails/old.webp", true, null);

        eventPost.updateThumbnailImagePath("events/thumbnails/new.webp");

        assertThat(eventPost.getThumbnailImagePath()).isEqualTo("events/thumbnails/new.webp");
        assertThat(eventPost.getTitle()).isEqualTo("제목");
    }
}
