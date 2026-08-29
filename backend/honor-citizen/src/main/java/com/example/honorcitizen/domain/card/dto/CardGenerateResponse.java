package com.example.honorcitizen.domain.card.dto;

import java.time.LocalDate;

// 카드 생성(3. 카드 생성·저장 — 최소 버전, 2026-08-30) 응답 — 저장된 S3 key와 발급일자만 반환한다.
// 이미지 바이트는 2-C Preview로 이미 확인했으므로 여기서 다시 보내지 않는다.
public record CardGenerateResponse(String cardFrontPath, String cardBackPath, LocalDate issueDate) {
}
