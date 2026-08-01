package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
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

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String applicationNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long cardTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationType applicationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private boolean receiverSameAsApplicant;

    @Column(nullable = false)
    private int totalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueType issueType;

    private Long cardDesignId;

    private Long logoFileId;

    private Long sealFileId;

    private Long submitFileId;

    @Column(length = 500)
    private String photoRejectReason;

    public static Application createIndividual(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId) {
        Application application = base(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant);
        application.applicationType = ApplicationType.INDIVIDUAL;
        application.totalQuantity = 1;
        application.logoFileId = logoFileId;
        application.sealFileId = sealFileId;
        return application;
    }

    public static Application createGroup(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, int totalQuantity,
            Long logoFileId, Long sealFileId, Long submitFileId) {
        Application application = base(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant);
        application.applicationType = ApplicationType.GROUP;
        application.totalQuantity = totalQuantity;
        application.logoFileId = logoFileId;
        application.sealFileId = sealFileId;
        application.submitFileId = submitFileId;
        return application;
    }

    private static Application base(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant) {
        Application application = new Application();
        application.userId = userId;
        application.applicationNumber = applicationNumber;
        application.cardTypeId = cardTypeId;
        application.issueType = issueType;
        application.receiverSameAsApplicant = receiverSameAsApplicant;
        application.cardDesignId = null;
        application.status = ApplicationStatus.PAYMENT_PENDING;
        application.paymentStatus = PaymentStatus.WAITING;
        return application;
    }

    public boolean isIndividual() {
        return this.applicationType == ApplicationType.INDIVIDUAL;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void updateTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void confirmPayment() {
        transitionTo(ApplicationStatus.RECEIVED);
        this.paymentStatus = PaymentStatus.CONFIRMED;
    }

    public void startReview() {
        transitionTo(ApplicationStatus.REVIEWING);
    }

    public void rejectPhoto(String reason) {
        transitionTo(ApplicationStatus.PHOTO_REJECTED);
        this.photoRejectReason = reason;
    }

    public void resubmitForReview(Long newSubmitFileId) {
        transitionTo(ApplicationStatus.REVIEWING);
        this.photoRejectReason = null;
        if (newSubmitFileId != null) {
            this.submitFileId = newSubmitFileId;
        }
    }

    public void approveToNaming() {
        transitionTo(ApplicationStatus.NAME_EDITING);
    }

    public void startProducing() {
        transitionTo(ApplicationStatus.PRODUCING);
    }

    public void complete() {
        transitionTo(ApplicationStatus.COMPLETED);
    }

    public void cancel() {
        transitionTo(ApplicationStatus.CANCELLED);
    }

    private void transitionTo(ApplicationStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = next;
    }
}
