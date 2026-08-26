package com.example.honorcitizen.domain.card.dto;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.domain.card.entity.CardDesign;

public record CardDesignResponse(
        Long id,
        int designNumber,
        String name,
        CardDesignOrientation orientation,
        boolean isDefault,
        boolean active) {

    public static CardDesignResponse from(CardDesign design) {
        return new CardDesignResponse(design.getId(), design.getDesignNumber(), design.getName(),
                design.getOrientation(), design.isDefault(), design.isActive());
    }
}
