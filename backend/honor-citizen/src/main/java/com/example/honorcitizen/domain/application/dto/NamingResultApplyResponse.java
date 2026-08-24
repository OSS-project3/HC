package com.example.honorcitizen.domain.application.dto;

import lombok.Getter;

@Getter
public class NamingResultApplyResponse {

    private final int updatedCount;

    private NamingResultApplyResponse(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public static NamingResultApplyResponse of(int updatedCount) {
        return new NamingResultApplyResponse(updatedCount);
    }
}
