package com.example.honorcitizen.domain.manseryeok.dto;

import lombok.Getter;

// 재진입 일괄 복원(1-E-3) 응답 항목 — Application 소속 Member의 활성 만세력 결과.
// 활성 결과가 없는 Member는 목록에서 누락된다(null 항목을 넣지 않는 계약으로 고정 — 프론트는 누락=미확정).
@Getter
public class ManseryeokMemberResultResponse {

    private final Long memberId;
    private final ManseryeokActiveResultResponse result;

    public ManseryeokMemberResultResponse(Long memberId, ManseryeokActiveResultResponse result) {
        this.memberId = memberId;
        this.result = result;
    }
}
