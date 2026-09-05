package com.example.honorcitizen.domain.application.dto;

import lombok.Getter;

import java.time.LocalDateTime;

// 관리자 카드 다운로드(멤버 1명) 응답 — 재인쇄·부분 재제작용. ApplicationCardDownloadResponse(사용자용,
// COMPLETED 게이트)와 별개 계약이다 — 이쪽은 렌더링 결과물 존재 여부만으로 판단한다(2026-09-05 정책).
@Getter
public class AdminMemberCardDownloadResponse {

    private final Long applicationId;
    private final Long memberId;
    private final String cardFrontUrl;
    private final String cardBackUrl;
    private final LocalDateTime expiresAt;

    public AdminMemberCardDownloadResponse(Long applicationId, Long memberId, String cardFrontUrl,
            String cardBackUrl, LocalDateTime expiresAt) {
        this.applicationId = applicationId;
        this.memberId = memberId;
        this.cardFrontUrl = cardFrontUrl;
        this.cardBackUrl = cardBackUrl;
        this.expiresAt = expiresAt;
    }
}
