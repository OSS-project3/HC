package com.example.honorcitizen.domain.manseryeok.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 미리보기 전용 — DB에 아무것도 저장하지 않는다. timezoneId를 안 보내면 위경도로 Google Time Zone API를
// 조회하고, selectedOffset을 보내면 AMBIGUOUS_LOCAL_TIME 후보 중 하나를 확정(EXACT로 전환)한다.
@Getter
@NoArgsConstructor
public class ManseryeokResolveRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String timezoneId;

    private String selectedOffset;
}
