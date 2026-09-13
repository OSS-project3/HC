package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 십이간지 캐릭터 디자인 세트 지정(2026-09-06 신규, 2026-09-13 1~3 → 1~5로 확장) —
// 1~5 중 하나, 신청 1건당 값 1개. 4/5는 2/3번 스타일의 화이트 버전(card-templates/zodiac/4,5).
@Getter
@NoArgsConstructor
public class ZodiacDesignSetRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer zodiacDesignSet;
}
