package com.example.honorcitizen.common.enums;

// 관리자 "제작신청 관리" 작명 업무 진행중/완료/캔슬 조회(2026-09-24 정책 확정)의 3분류.
// 개인은 Application.status(COMPLETED/CANCELLED)로 그대로 판정하지만, 단체는 status와 무관하게
// 멤버 전원의 카드 생성 완료(cardFrontPath/cardBackPath) 여부로 DONE을 판정한다 — 전원 카드가
// 생성돼도 관리자가 "카드 발급 완료"를 누르기 전까지는 status가 COMPLETED가 아닐 수 있기 때문.
// 상태 전이 로직(completeNaming/markCardReady 등)은 이 enum과 무관하게 그대로 유지된다.
public enum NamingProgress {
    IN_PROGRESS,
    DONE,
    CANCELLED
}
