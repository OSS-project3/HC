package com.example.honorcitizen.domain.event.dto;

import com.example.honorcitizen.common.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// Board PATCH와 동일 원칙 — 전체 재제출(부분수정 아님).
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

    // COLLABORATION 전용 선택값. BOOTH가 보내면 Entity가 INVALID_INPUT으로 거절한다.
    // 프론트 FeedPost(data/eventFeedPosts.ts)가 이미 쓰는 필드명(company/logoUrl)에 맞춘다.
    @Size(max = 100)
    private String company;

    // true면 기존 로고/썸네일 연결을 제거한다. 새 파일과 동시에 오면 INVALID_INPUT(EVENT-EXT-3).
    private Boolean removeLogo;
    private Boolean removeThumbnail;

    // null(필드 생략) = 기존 갤러리 전체 유지, 빈 배열 = 기존 갤러리 전체 삭제 — 이 둘을 구분해야 하므로
    // Boolean 플래그가 아니라 리스트 자체의 null 여부로 판단한다(EVENT-EXT-3).
    private List<Long> keepImageIds;

    private Boolean visible;

    private Integer displayOrder;
}
