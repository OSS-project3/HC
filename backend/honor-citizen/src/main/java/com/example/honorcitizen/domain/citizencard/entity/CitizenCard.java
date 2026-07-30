package com.example.honorcitizen.domain.citizencard.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "citizen_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CitizenCard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, unique = true, length = 20)
    private String cardNumber;          // HN-KR-YYMM-NNNN

    @Column(nullable = false, length = 500)
    private String imagePath;           // S3 키: cards/{cardNumber}.png

    @Column(length = 500)
    private String nameMeaningPath;     // S3 키: cards/meaning_{cardNumber}.png

    @Column(length = 500)
    private String zipPath;             // S3 키: zips/{cardNumber}.zip

    @Column(length = 1000)
    private String downloadUrl;         // Presigned URL (유효 7일)

    private LocalDateTime urlExpiresAt;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    public static CitizenCard create(Long applicationId, String cardNumber,
                                     String imagePath, String nameMeaningPath,
                                     String zipPath, String downloadUrl,
                                     LocalDateTime urlExpiresAt) {
        CitizenCard card = new CitizenCard();
        card.applicationId = applicationId;
        card.cardNumber = cardNumber;
        card.imagePath = imagePath;
        card.nameMeaningPath = nameMeaningPath;
        card.zipPath = zipPath;
        card.downloadUrl = downloadUrl;
        card.urlExpiresAt = urlExpiresAt;
        card.issuedAt = LocalDateTime.now();
        return card;
    }

    public void refreshDownloadUrl(String downloadUrl, LocalDateTime urlExpiresAt) {
        this.downloadUrl = downloadUrl;
        this.urlExpiresAt = urlExpiresAt;
    }

    public boolean isDownloadUrlExpiringSoon() {
        return urlExpiresAt == null || urlExpiresAt.isBefore(LocalDateTime.now().plusHours(24));
    }
}
