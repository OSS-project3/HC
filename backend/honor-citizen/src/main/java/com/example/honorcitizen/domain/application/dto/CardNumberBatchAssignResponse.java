package com.example.honorcitizen.domain.application.dto;

import lombok.Getter;

@Getter
public class CardNumberBatchAssignResponse {

    private final int updatedCount;

    private CardNumberBatchAssignResponse(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public static CardNumberBatchAssignResponse of(int updatedCount) {
        return new CardNumberBatchAssignResponse(updatedCount);
    }
}
