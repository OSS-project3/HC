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
            case NAME_EDITING -> next == PRODUCTION_READY;
            case PRODUCTION_READY -> next == PRODUCING;
            case PRODUCING -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
