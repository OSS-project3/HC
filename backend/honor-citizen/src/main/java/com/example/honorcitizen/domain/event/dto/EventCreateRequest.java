package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.common.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    // COLLABORATION 전용 선택값. BOOTH가 보내면 Entity가 INVALID_INPUT으로 거절한다(EVENT-EXT-1).
    @Size(max = 100)
    private String companyName;

    // 생략하면 서비스에서 true로 채운다(data-model.md §1 기본값).
    private Boolean visible;

    private Integer displayOrder;
}
