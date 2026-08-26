package com.example.honorcitizen.domain.manseryeok.dto;

import com.example.honorcitizen.common.enums.TimeAccuracy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// 프론트(saju.ts)가 Spring이 확정한 utcInstant/longitude로 진태양시 보정 + 만세력 계산을 마친 뒤
// 최종 결과를 저장 요청할 때 쓴다. confirmedPillars/uncertainPillars/elementCounts는 실제 사주 계산
// 로직이 프론트에만 있어(admin-saju.md 원칙) Spring이 재계산·검증하지 않고 그대로 저장한다 — 대신
// timezoneId/utcInstant/selectedOffset은 Spring이 자체 재계산해 무결성을 검증한다(뒤 Service 참고).
@Getter
@NoArgsConstructor
public class ManseryeokConfirmRequest {

    @NotBlank
    private String timezoneId;

    @NotNull
    private Double longitude;

    // EXACT일 때만 필수. PARTIAL/UNKNOWN이면 비워둔다.
    private String selectedOffset;
    private Instant utcInstant;

    @NotNull
    private TimeAccuracy timeAccuracy;

    // {"year":{"stem":"갑","branch":"자"}, "month":{...}, "day":{...}, "hour":{...}} 중 확정된 주만.
    @NotNull
    private Map<String, Map<String, String>> confirmedPillars;

    // 확정 못한 주 이름(예: ["hour"]). PARTIAL/UNKNOWN이 아니면 빈 리스트.
    private List<String> uncertainPillars;

    // {"목":1,"화":2,"토":0,"금":3,"수":2}
    private Map<String, Integer> elementCounts;

    @NotBlank
    private String calculationEngineVersion;

    @NotBlank
    private String inputHash;
}
