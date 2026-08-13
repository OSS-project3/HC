package com.example.honorcitizen.domain.review.dto;

import com.example.honorcitizen.domain.card.entity.CardType;
import lombok.Getter;

@Getter
public class CardTypeSummaryResponse {

    private final Long id;
    private final String name;

    private CardTypeSummaryResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CardTypeSummaryResponse from(CardType cardType) {
        return new CardTypeSummaryResponse(cardType.getId(), cardType.getName());
    }
}
