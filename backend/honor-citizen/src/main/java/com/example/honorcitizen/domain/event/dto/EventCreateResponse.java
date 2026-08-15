package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.domain.event.entity.EventPost;
import lombok.Getter;

@Getter
public class EventCreateResponse {

    private final Long id;

    private EventCreateResponse(Long id) {
        this.id = id;
    }

    public static EventCreateResponse from(EventPost eventPost) {
        return new EventCreateResponse(eventPost.getId());
    }
}
