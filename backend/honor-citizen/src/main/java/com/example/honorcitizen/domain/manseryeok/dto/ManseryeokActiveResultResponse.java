package com.example.honorcitizen.domain.manseryeok.dto;

import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 현재 활성(active=true) 만세력 결과 조회 응답 — 카드 띠 이미지 결정(confirmedPillars.year) 등에 쓴다.
@Getter
public class ManseryeokActiveResultResponse {

    private final String timezoneId;
    private final Double longitude;
    private final String selectedOffset;
    private final Instant utcInstant;
    private final TimeAccuracy timeAccuracy;
    private final Map<String, Map<String, String>> confirmedPillars;
    private final List<String> uncertainPillars;
    private final Map<String, Integer> elementCounts;
    private final String tzdbVersion;
    private final String calculationEngineVersion;
    private final LocalDateTime calculatedAt;

    public ManseryeokActiveResultResponse(ManseryeokResult result,
            Map<String, Map<String, String>> confirmedPillars, List<String> uncertainPillars,
            Map<String, Integer> elementCounts) {
        this.timezoneId = result.getTimezoneId();
        this.longitude = result.getLongitude();
        this.selectedOffset = result.getSelectedOffset();
        this.utcInstant = result.getUtcInstant();
        this.timeAccuracy = result.getTimeAccuracy();
        this.confirmedPillars = confirmedPillars;
        this.uncertainPillars = uncertainPillars;
        this.elementCounts = elementCounts;
        this.tzdbVersion = result.getTzdbVersion();
        this.calculationEngineVersion = result.getCalculationEngineVersion();
        this.calculatedAt = result.getCalculatedAt();
    }
}
