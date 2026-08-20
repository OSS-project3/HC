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
    // 프론트 FeedPost(data/eventFeedPosts.ts)가 이미 쓰는 필드명(company/logoUrl)에 맞춘다 —
    // 엔티티 컬럼명(company_name)은 내부 구현이라 그대로 두고 API 계약(DTO)만 프론트에 맞춘다.
    @Size(max = 100)
    private String company;

    // 생략하면 서비스에서 true로 채운다(data-model.md §1 기본값).
    private Boolean visible;

    private Integer displayOrder;
}
