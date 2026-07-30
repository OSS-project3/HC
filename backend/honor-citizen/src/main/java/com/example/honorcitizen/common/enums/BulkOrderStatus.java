package com.example.honorcitizen.common.enums;

public enum BulkOrderStatus {
    DRAFT("임시저장"),
    PENDING("결제대기");
    // TODO: 추후 상태 추가 (관리자 승인, 완료 등)

    private final String displayName;

    BulkOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
