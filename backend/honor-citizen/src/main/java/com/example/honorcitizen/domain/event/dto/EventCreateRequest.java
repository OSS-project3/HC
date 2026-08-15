package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.common.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateRequest {

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

    // 생략하면 서비스에서 true로 채운다(data-model.md §1 기본값).
    private Boolean visible;

    private Integer displayOrder;
}
