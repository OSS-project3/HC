package com.example.honorcitizen.domain.citizencard.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.citizencard.dto.CitizenCardDownloadResponse;
import com.example.honorcitizen.domain.citizencard.dto.CitizenCardIssueResponse;
import com.example.honorcitizen.domain.citizencard.entity.CitizenCard;
import com.example.honorcitizen.domain.citizencard.repository.CitizenCardRepository;
import com.example.honorcitizen.domain.koreanname.entity.KoreanName;
import com.example.honorcitizen.domain.koreanname.repository.KoreanNameRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.entity.ApplicationStatusLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.log.repository.ApplicationStatusLogRepository;
import com.example.honorcitizen.infra.card.CardImageContext;
import com.example.honorcitizen.infra.card.CardImageData;
import com.example.honorcitizen.infra.card.CardImageGenerator;
import com.example.honorcitizen.infra.card.ZodiacCalculator;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenCardService {

    private static final long PRESIGNED_URL_EXPIRY_SECONDS = 7 * 24 * 3600L;
    private static final List<ApplicationStatus> DOWNLOADABLE_STATUSES = List.of(
            ApplicationStatus.CARD_READY,
            ApplicationStatus.SHIPPING,
            ApplicationStatus.DELIVERED
    );

    private final CitizenCardRepository citizenCardRepository;
    private final ApplicationRepository applicationRepository;
    private final KoreanNameRepository koreanNameRepository;
    private final StorageService storageService;
    private final CardImageGenerator cardImageGenerator;
    private final ApplicationStatusLogRepository statusLogRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;

    @Value("${app.card.template-key}")
    private String cardTemplateKey;

    @Value("${app.card.zodiac-key-prefix}")
    private String zodiacKeyPrefix;

    @Value("${app.card.issuer-name}")
    private String issuerName;

    @Transactional
    public CitizenCardIssueResponse issueCard(Long adminId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        Optional.of(application.getStatus())
                .filter(s -> s == ApplicationStatus.REVIEWING)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_APPLICATION_STATUS));

        KoreanName koreanName = koreanNameRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.KOREAN_NAME_NOT_FOUND));
        Optional.of(applicationId)
                .filter(id -> !citizenCardRepository.existsByApplicationId(id))
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_ALREADY_ISSUED));

        String cardNumber = generateCardNumber();
        CardImageData imageData = buildImageData(application, koreanName, cardNumber);

        byte[] templateBytes = storageService.download(cardTemplateKey);
        byte[] photoBytes = downloadQuietly(application.getPhotoPath());
        String zodiacAnimal = ZodiacCalculator.getAnimalKey(application.getBirthDate().getYear());
        byte[] zodiacBytes = downloadQuietly(zodiacKeyPrefix + "/" + zodiacAnimal + ".png");

        CardImageContext context = new CardImageContext(templateBytes, photoBytes, zodiacBytes, imageData);
        byte[] cardImageBytes = cardImageGenerator.generateCitizenCard(context);
        byte[] nameMeaningBytes = cardImageGenerator.generateNameMeaningCard(context);
        byte[] zipBytes = createZip(cardNumber, cardImageBytes, nameMeaningBytes);

        String imagePath = "cards/" + cardNumber + ".png";
        String meaningPath = "cards/meaning_" + cardNumber + ".png";
        String zipPath = "zips/" + cardNumber + ".zip";
        storageService.uploadBytes(imagePath, cardImageBytes, "image/png");
        storageService.uploadBytes(meaningPath, nameMeaningBytes, "image/png");
        storageService.uploadBytes(zipPath, zipBytes, "application/zip");

        LocalDateTime urlExpiresAt = LocalDateTime.now().plusSeconds(PRESIGNED_URL_EXPIRY_SECONDS);
        String downloadUrl = storageService.generatePresignedUrl(zipPath, PRESIGNED_URL_EXPIRY_SECONDS);

        CitizenCard card = CitizenCard.create(
                applicationId, cardNumber, imagePath, meaningPath,
                zipPath, downloadUrl, urlExpiresAt);
        citizenCardRepository.save(card);

        ApplicationStatus before = application.getStatus();
        application.markCardReady();
        applicationRepository.save(application);

        statusLogRepository.save(ApplicationStatusLog.create(
                applicationId, before, ApplicationStatus.CARD_READY, adminId, null));
        adminActivityLogRepository.save(AdminActivityLog.create(
                adminId, AdminActivityLog.CARD_ISSUE, applicationId, card.getCardNumber()));

        return CitizenCardIssueResponse.from(card);
    }

    @Transactional
    public CitizenCardDownloadResponse getDownloadUrl(Long userId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        Optional.of(application)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));
        Optional.of(application.getStatus())
                .filter(DOWNLOADABLE_STATUSES::contains)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_READY));

        CitizenCard card = citizenCardRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));

        if (card.isDownloadUrlExpiringSoon()) {
            LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(PRESIGNED_URL_EXPIRY_SECONDS);
            String newUrl = storageService.generatePresignedUrl(card.getZipPath(), PRESIGNED_URL_EXPIRY_SECONDS);
            card.refreshDownloadUrl(newUrl, newExpiry);
            citizenCardRepository.save(card);
        }

        return CitizenCardDownloadResponse.from(card);
    }

    @Transactional(readOnly = true)
    public Optional<CitizenCard> findOptionalByApplicationId(Long applicationId) {
        return citizenCardRepository.findByApplicationId(applicationId);
    }

    private String generateCardNumber() {
        LocalDate now = LocalDate.now();
        String yymm = String.format("%02d%02d", now.getYear() % 100, now.getMonthValue());
        int nextSeq = citizenCardRepository.findMaxSequence().orElse(0) + 1;
        return String.format("HN-KR-%s-%04d", yymm, nextSeq);
    }

    private CardImageData buildImageData(Application application, KoreanName koreanName, String cardNumber) {
        return CardImageData.builder()
                .fullNameKo(koreanName.getFullNameKo())
                .nameOrigin(koreanName.getNameOrigin())
                .fullNameEn(koreanName.getFullNameEn())
                .cardNumber(cardNumber)
                .birthRegion(application.getBirthRegion())
                .issuerName(issuerName)
                .meaning(koreanName.getMeaning())
                .issuedDate(LocalDate.now())
                .birthDate(application.getBirthDate())
                .build();
    }

    private byte[] createZip(String cardNumber, byte[] cardImageBytes, byte[] nameMeaningBytes) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry cardEntry = new ZipEntry(cardNumber + ".png");
            zos.putNextEntry(cardEntry);
            zos.write(cardImageBytes);
            zos.closeEntry();

            ZipEntry meaningEntry = new ZipEntry("meaning_" + cardNumber + ".png");
            zos.putNextEntry(meaningEntry);
            zos.write(nameMeaningBytes);
            zos.closeEntry();

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("ZIP 생성 실패 cardNumber={}", cardNumber, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    // S3에 없는 경우 null 반환 (사진/띠 이미지 없어도 카드 생성 가능)
    private byte[] downloadQuietly(String key) {
        try {
            return storageService.download(key);
        } catch (CustomException e) {
            log.warn("S3 다운로드 실패, 건너뜀: {}", key);
            return null;
        }
    }
}
