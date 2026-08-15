package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.common.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Board PATCH와 동일 원칙 — 전체 재제출(부분수정 아님). 갤러리(EventImage) 편집은 이번 패스 범위 밖.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdateRequest {

    @NotNull
    private EventType eventType;

    @NotBlank
    private String title;

    private LocalDate eventDate;

    @NotBlank
    private String eventDateText;

    @NotBlank
    private String place;

    @NotBlank
    private String host;

    @NotBlank
    private String cardLabel;

    @NotBlank
    private String content;

    private Boolean visible;

    private Integer displayOrder;
}
