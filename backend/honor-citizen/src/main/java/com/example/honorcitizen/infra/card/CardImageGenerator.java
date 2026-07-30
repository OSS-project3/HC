package com.example.honorcitizen.infra.card;

public interface CardImageGenerator {

    byte[] generateCitizenCard(CardImageContext context);

    byte[] generateNameMeaningCard(CardImageContext context);
}
