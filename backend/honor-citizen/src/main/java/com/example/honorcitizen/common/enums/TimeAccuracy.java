package com.example.honorcitizen.common.enums;

// admin-saju.md "시간 정확도" — 만세력 계산에 사용할 수 있는 출생시각 확정도.
// EXACT: 절대 시점(utcInstant) 확정, 시주 포함 전체 계산 가능.
// PARTIAL: DST 중복(AMBIGUOUS_LOCAL_TIME) 등으로 절대 시점을 하나로 확정 못함 — 후보 간 공통 주만 확정.
// UNKNOWN: 출생시간 미입력 — 시주는 항상 제외, 날짜 범위 후보 중 공통 주만 확정.
public enum TimeAccuracy {
    EXACT,
    PARTIAL,
    UNKNOWN
}
