package com.example.honorcitizen.domain.event.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventImageTest {

    @Test
    void createSetsAllFields() {
        EventImage eventImage = EventImage.create(1L, "events/gallery/a.webp", "a.webp", 0);

        assertThat(eventImage.getEventPostId()).isEqualTo(1L);
        assertThat(eventImage.getImagePath()).isEqualTo("events/gallery/a.webp");
        assertThat(eventImage.getOriginalFilename()).isEqualTo("a.webp");
        assertThat(eventImage.getDisplayOrder()).isZero();
        assertThat(eventImage.getCreatedAt()).isNotNull();
    }
}
