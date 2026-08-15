package com.example.honorcitizen.domain.event.dto;

import lombok.Getter;

@Getter
public class EventImageResponse {

    private final Long id;
    private final String originalFileName;
    private final String url;

    private EventImageResponse(Long id, String originalFileName, String url) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.url = url;
    }

    public static EventImageResponse of(Long imageId, String originalFileName, String url) {
        return new EventImageResponse(imageId, originalFileName, url);
    }
}
