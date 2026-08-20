package com.example.honorcitizen.domain.event.entity;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventPostTest {

    @Test
    void createSetsAllFields() {
        EventPost eventPost = EventPost.create(EventType.BOOTH, "서울공예트렌드페어", LocalDate.of(2026, 12, 1),
                "2026. 12", "서울 코엑스 Hall C", "한국공예·디자인문화진흥원", "명예한국인증 · 방문증",
                "부스를 찾은 방문객에게...", "events/thumbnails/a.webp", null, null, true, null);

        assertThat(eventPost.getEventType()).isEqualTo(EventType.BOOTH);
        assertThat(eventPost.getTitle()).isEqualTo("서울공예트렌드페어");
        assertThat(eventPost.getEventDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(eventPost.getEventDateText()).isEqualTo("2026. 12");
        assertThat(eventPost.getPlace()).isEqualTo("서울 코엑스 Hall C");
        assertThat(eventPost.getHost()).isEqualTo("한국공예·디자인문화진흥원");
        assertThat(eventPost.getCardLabel()).isEqualTo("명예한국인증 · 방문증");
        assertThat(eventPost.getContent()).isEqualTo("부스를 찾은 방문객에게...");
        assertThat(eventPost.getThumbnailImagePath()).isEqualTo("events/thumbnails/a.webp");
        assertThat(eventPost.getCompanyName()).isNull();
        assertThat(eventPost.getLogoImagePath()).isNull();
        assertThat(eventPost.isVisible()).isTrue();
        assertThat(eventPost.getDisplayOrder()).isNull();
    }

    @Test
    void createStoresCompanyNameAndLogoForCollaboration() {
        EventPost eventPost = EventPost.create(EventType.COLLABORATION, "협업행사", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, "  OO기업  ", "events/logos/a.webp", true, null);

        assertThat(eventPost.getCompanyName()).isEqualTo("OO기업");
        assertThat(eventPost.getLogoImagePath()).isEqualTo("events/logos/a.webp");
    }

    @Test
    void createRejectsCompanyNameForBooth() {
        assertThatThrownBy(() -> EventPost.create(EventType.BOOTH, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, "OO기업", null, true, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createRejectsLogoForBooth() {
        assertThatThrownBy(() -> EventPost.create(EventType.BOOTH, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, null, "events/logos/a.webp", true, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createRejectsCompanyNameOver100Characters() {
        String tooLong = "가".repeat(101);
        assertThatThrownBy(() -> EventPost.create(EventType.COLLABORATION, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, tooLong, null, true, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateOverwritesFieldsButNotThumbnail() {
        EventPost eventPost = EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "원래 장소", "원래 주최", "원래 카드", "원래 내용", "events/thumbnails/old.webp", null, null, true, null);

        eventPost.update(EventType.COLLABORATION, "새 제목", LocalDate.of(2026, 3, 1), "2026. 03",
                "새 장소", "새 주최", "새 카드", "새 내용", "OO기업", false, 2);

        assertThat(eventPost.getEventType()).isEqualTo(EventType.COLLABORATION);
        assertThat(eventPost.getTitle()).isEqualTo("새 제목");
        assertThat(eventPost.getEventDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(eventPost.getEventDateText()).isEqualTo("2026. 03");
        assertThat(eventPost.getPlace()).isEqualTo("새 장소");
        assertThat(eventPost.getHost()).isEqualTo("새 주최");
        assertThat(eventPost.getCardLabel()).isEqualTo("새 카드");
        assertThat(eventPost.getContent()).isEqualTo("새 내용");
        assertThat(eventPost.getCompanyName()).isEqualTo("OO기업");
        assertThat(eventPost.isVisible()).isFalse();
        assertThat(eventPost.getDisplayOrder()).isEqualTo(2);
        assertThat(eventPost.getThumbnailImagePath()).isEqualTo("events/thumbnails/old.webp");
    }

    @Test
    void updateRejectsCompanyNameOver100Characters() {
        EventPost eventPost = EventPost.create(EventType.COLLABORATION, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, null, null, true, null);

        String tooLong = "가".repeat(101);
        assertThatThrownBy(() -> eventPost.update(EventType.COLLABORATION, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", tooLong, true, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateThumbnailImagePathOverwritesOnlyThumbnail() {
        EventPost eventPost = EventPost.create(EventType.BOOTH, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", "events/thumbnails/old.webp", null, null, true, null);

        eventPost.updateThumbnailImagePath("events/thumbnails/new.webp");

        assertThat(eventPost.getThumbnailImagePath()).isEqualTo("events/thumbnails/new.webp");
        assertThat(eventPost.getTitle()).isEqualTo("제목");
    }

    @Test
    void assertCollaborationInvariantRejectsBoothWithResidualLogo() {
        EventPost eventPost = EventPost.create(EventType.COLLABORATION, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, null, "events/logos/a.webp", true, null);

        // BOOTH로 전환하면서 companyName만 지우고 로고를 안 지운 상황을 재현한다.
        eventPost.update(EventType.BOOTH, "제목", null, "2026. 01", "장소", "주최", "카드", "내용",
                null, true, null);

        assertThatThrownBy(eventPost::assertCollaborationInvariant)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void assertCollaborationInvariantPassesAfterLogoCleared() {
        EventPost eventPost = EventPost.create(EventType.COLLABORATION, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, null, "events/logos/a.webp", true, null);

        eventPost.update(EventType.BOOTH, "제목", null, "2026. 01", "장소", "주최", "카드", "내용",
                null, true, null);
        eventPost.updateLogoImagePath(null);

        eventPost.assertCollaborationInvariant();
        assertThat(eventPost.getLogoImagePath()).isNull();
    }
}
