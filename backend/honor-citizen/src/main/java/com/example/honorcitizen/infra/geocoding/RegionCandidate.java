package com.example.honorcitizen.infra.geocoding;

// 출생지역 검색 결과 후보 — 특정 공급자(Google 등)의 응답 객체를 도메인에 직접 노출하지 않기 위한 경계.
// admin-saju.md "출생지역 해석 정책": 검색 결과가 여러 개면 관리자가 실제 출생지역을 선택한다.
public record RegionCandidate(String displayName, double latitude, double longitude) {
}
