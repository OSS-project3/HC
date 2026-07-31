package com.example.honorcitizen.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LookupMethod {
    APPLICATION("application"),
    CARD("card");

    private final String value;

    LookupMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LookupMethod from(String value) {
        for (LookupMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown lookup method: " + value);
    }
}
