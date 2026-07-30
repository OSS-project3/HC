package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardType;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private String nationality;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private LocalTime birthTime;

    @Column(nullable = false)
    private String birthRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private String photoPath;

    @Column(nullable = false)
    private String photoId;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Column
    private Long bulkOrderId;  // null = 단건 신청

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    public static Application createSingle(
            Long userId, String nameEn, String nationality,
            LocalDate birthDate, LocalTime birthTime, String birthRegion,
            Gender gender, String photoId, String photoPath,
            LocalDate entryDate, String address, CardType cardType) {
        Application application = new Application();
        application.userId = userId;
        application.nameEn = nameEn;
        application.nationality = nationality;
        application.birthDate = birthDate;
        application.birthTime = birthTime;
        application.birthRegion = birthRegion;
        application.gender = gender;
        application.photoId = photoId;
        application.photoPath = photoPath;
        application.entryDate = entryDate;
        application.address = address;
        application.cardType = cardType;
        application.status = ApplicationStatus.PENDING;
        return application;
    }

    public static Application createBulk(
            Long userId, String nameEn, String nationality,
            LocalDate birthDate, LocalTime birthTime, String birthRegion,
            Gender gender, String photoId, String photoPath,
            LocalDate entryDate, String address, CardType cardType, Long bulkOrderId) {
        Application application = createSingle(userId, nameEn, nationality,
                birthDate, birthTime, birthRegion, gender, photoId, photoPath,
                entryDate, address, cardType);
        application.bulkOrderId = bulkOrderId;
        application.status = ApplicationStatus.DRAFT;
        return application;
    }

    public void approvePayment() {
        validateTransition(ApplicationStatus.PENDING);
        this.status = ApplicationStatus.PENDING;
    }

    public void startReview() {
        validateTransition(ApplicationStatus.REVIEWING);
        this.status = ApplicationStatus.REVIEWING;
    }

    public void rejectPhoto() {
        validateTransition(ApplicationStatus.PHOTO_REJECTED);
        this.status = ApplicationStatus.PHOTO_REJECTED;
    }

    public void resubmitPhoto(String newPhotoPath, String newPhotoId) {
        validateTransition(ApplicationStatus.PENDING);
        this.status = ApplicationStatus.PENDING;
        this.photoPath = newPhotoPath;
        this.photoId = newPhotoId;
    }

    public void markCardReady() {
        validateTransition(ApplicationStatus.CARD_READY);
        this.status = ApplicationStatus.CARD_READY;
    }

    public void startShipping() {
        validateTransition(ApplicationStatus.SHIPPING);
        this.status = ApplicationStatus.SHIPPING;
    }

    public void completeDelivery() {
        validateTransition(ApplicationStatus.DELIVERED);
        this.status = ApplicationStatus.DELIVERED;
    }

    public void cancel() {
        if (this.status == ApplicationStatus.SHIPPING || this.status == ApplicationStatus.DELIVERED) {
            throwInvalidTransition(ApplicationStatus.CANCELLED);
        }
        this.status = ApplicationStatus.CANCELLED;
    }

    private void validateTransition(ApplicationStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throwInvalidTransition(next);
        }
    }

    private void throwInvalidTransition(ApplicationStatus next) {
        throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
    }
}
