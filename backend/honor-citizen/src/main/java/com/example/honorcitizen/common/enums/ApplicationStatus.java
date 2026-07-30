package com.example.honorcitizen.common.enums;

public enum ApplicationStatus {
    DRAFT,
    PENDING,
    REVIEWING,
    PHOTO_REJECTED,
    CARD_READY,
    SHIPPING,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case DRAFT -> next == PENDING || next == CANCELLED;
            case PENDING -> next == REVIEWING || next == CANCELLED;
            case REVIEWING -> next == PHOTO_REJECTED || next == CARD_READY || next == CANCELLED;
            case PHOTO_REJECTED -> next == PENDING;
            case CARD_READY -> next == SHIPPING;
            case SHIPPING -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
