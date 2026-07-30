package com.example.honorcitizen.domain.citizencard.dto;

import com.example.honorcitizen.domain.citizencard.entity.CitizenCard;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CitizenCardDownloadResponse {

    private final String downloadUrl;
    private final LocalDateTime expiresAt;
    private final String fileName;
    private final List<String> includes;

    private CitizenCardDownloadResponse(String downloadUrl, LocalDateTime expiresAt,
                                         String fileName, List<String> includes) {
        this.downloadUrl = downloadUrl;
        this.expiresAt = expiresAt;
        this.fileName = fileName;
        this.includes = includes;
    }

    public static CitizenCardDownloadResponse from(CitizenCard card) {
        String cardNumber = card.getCardNumber();
        return new CitizenCardDownloadResponse(
                card.getDownloadUrl(),
                card.getUrlExpiresAt(),
                cardNumber + ".zip",
                List.of(cardNumber + ".png", "meaning_" + cardNumber + ".png")
        );
    }
}
