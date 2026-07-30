package com.example.honorcitizen.infra.card;

public class ZodiacCalculator {

    private static final String[] ANIMALS = {
            "rat", "ox", "tiger", "rabbit", "dragon", "snake",
            "horse", "goat", "monkey", "rooster", "dog", "pig"
    };

    // 2020 = 쥐(rat) 기준
    public static String getAnimalKey(int birthYear) {
        int index = ((birthYear - 2020) % 12 + 12) % 12;
        return ANIMALS[index];
    }

    private ZodiacCalculator() {}
}
