package com.example.honorcitizen.common.enums;

public enum CardType {
    CITIZEN_CARD("명예시민증"),
    STUDENT_CARD("학생증"),
    ACCESS_CARD("출입증");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
