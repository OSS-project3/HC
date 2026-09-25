package com.example.honorcitizen.common.enums;

public enum ApplicationStatus {
    SUBMITTED,
    REVIEWING,
    PHOTO_REJECTED,
    NAME_EDITING,
    PRODUCTION_READY,
    PRODUCING,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case SUBMITTED -> next == REVIEWING || next == CANCELLED;
            case REVIEWING -> next == PHOTO_REJECTED || next == NAME_EDITING || next == CANCELLED;
            case PHOTO_REJECTED -> next == REVIEWING || next == CANCELLED;
            // NAME_EDITING/PRODUCTION_READY/PRODUCING → CANCELLED: 관리자 강제 취소(2026-09-25
            // 확정 정책)를 위한 이탈 경로. 완료(COMPLETED) 전까지는 운영상 언제든 취소할 수 있어야
            // 한다는 요구사항이라, 정상 진행 경로(→ PRODUCTION_READY/PRODUCING/COMPLETED)에 취소
            // 이탈 경로만 추가한다 — 다른 전이는 바꾸지 않는다.
            case NAME_EDITING -> next == PRODUCTION_READY || next == CANCELLED;
            case PRODUCTION_READY -> next == PRODUCING || next == CANCELLED;
            case PRODUCING -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
