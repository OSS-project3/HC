package com.example.honorcitizen.domain.card.dto;

// 카드 미리보기(2-C, 2026-08-27 계약 변경) — 앞/뒤 PNG를 base64로 한 번에 반환한다.
// 관리자 화면 미리보기 용도라 이미지 크기가 작고, 매번 앞/뒤 두 번 호출할 때 공통 DB조회·검증·
// 만세력조회·S3다운로드가 중복 실행되는 낭비를 없애기 위해 raw image/png 단일 응답 대신
// ApiResponse<CardPreviewResponse> JSON 응답(base64 2개)으로 바꿨다.
public record CardPreviewResponse(String front, String back) {
}
