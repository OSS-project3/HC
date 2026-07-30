package com.example.honorcitizen.domain.citizencard.dto;

import com.example.honorcitizen.domain.citizencard.entity.CitizenCard;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CitizenCardIssueResponse {

    private final String cardNumber;
    private final String imagePath;
    private final String zipPath;
    private final String downloadUrl;
    private final LocalDateTime issuedAt;
    private final boolean emailSent;    // EmailLog 도메인 구현 후 true

    private CitizenCardIssueResponse(String cardNumber, String imagePath, String zipPath,
                                      String downloadUrl, LocalDateTime issuedAt, boolean emailSent) {
        this.cardNumber = cardNumber;
        this.imagePath = imagePath;
        this.zipPath = zipPath;
        this.downloadUrl = downloadUrl;
        this.issuedAt = issuedAt;
        this.emailSent = emailSent;
    }

    public static CitizenCardIssueResponse from(CitizenCard card) {
        return new CitizenCardIssueResponse(
                card.getCardNumber(),
                card.getImagePath(),
                card.getZipPath(),
                card.getDownloadUrl(),
                card.getIssuedAt(),
                false
        );
    }
}
