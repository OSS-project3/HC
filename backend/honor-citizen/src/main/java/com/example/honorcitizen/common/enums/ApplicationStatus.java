package com.example.honorcitizen.common.enums;

public enum ApplicationStatus {
    PAYMENT_PENDING,
    RECEIVED,
    REVIEWING,
    PHOTO_REJECTED,
    NAME_EDITING,
    PRODUCING,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(ApplicationStatus next) {
        if (next == CANCELLED) {
            return this != CANCELLED;
        }
        return switch (this) {
            case PAYMENT_PENDING -> next == RECEIVED;
            case RECEIVED -> next == REVIEWING;
            case REVIEWING -> next == PHOTO_REJECTED || next == NAME_EDITING;
            case PHOTO_REJECTED -> next == REVIEWING;
            case NAME_EDITING -> next == PRODUCING;
            case PRODUCING -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
