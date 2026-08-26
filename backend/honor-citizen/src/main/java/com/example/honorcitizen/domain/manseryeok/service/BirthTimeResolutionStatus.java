package com.example.honorcitizen.domain.manseryeok.service;

// BirthTimeZoneResolver.resolve()의 결과 상태 — admin-saju.md "DST 경계 정책".
// EXACT: 유효 offset 1개, utcInstant 확정.
// NONEXISTENT_LOCAL_TIME: DST 시작으로 그 현지시각 자체가 존재하지 않음(입력 확인 필요, 계산 불가).
// AMBIGUOUS_LOCAL_TIME: DST 종료로 같은 현지시각이 두 번 존재 — 관리자가 후보 중 선택하거나 PARTIAL로 진행.
// UNKNOWN_TIME: 출생시간 미입력 — offset 판정 자체를 시도하지 않는다.
public enum BirthTimeResolutionStatus {
    EXACT,
    NONEXISTENT_LOCAL_TIME,
    AMBIGUOUS_LOCAL_TIME,
    UNKNOWN_TIME
}
