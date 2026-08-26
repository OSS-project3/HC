package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CancellationReason;
import com.example.honorcitizen.common.enums.CancellationType;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.Receiver;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyApplicationDetailResponse {

    private final Long applicationId;
    private final String applicationNumber;
    private final ApplicationType applicationType;
    private final Long cardTypeId;
    private final String cardTypeName;
    private final IssueType issueType;
    private final int totalQuantity;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final LocalDateTime paymentGuidedAt;
    private final LocalDateTime paymentDueAt;
    private final LocalDateTime cancelledAt;
    private final CancellationType cancellationType;
    private final CancellationReason cancellationReason;
    private final LocalDateTime refundedAt;
    private final LocalDateTime cardReadyAt;
    private final LocalDateTime physicalDispatchedAt;
    private final String photoRejectReason;
    private final ApplicantSummary applicant;
    private final ReceiverSummary receiver;
    private final long memberCount;
    private final LocalDateTime createdAt;
    private final String depositorName;
    // 낙관적 락 버전 — 카드번호 일괄 저장(PUT .../card-numbers)의 applicationVersion 대조용.
    private final Long version;

    private MyApplicationDetailResponse(Long applicationId, String applicationNumber, ApplicationType applicationType,
            Long cardTypeId, String cardTypeName, IssueType issueType, int totalQuantity, ApplicationStatus status,
            PaymentStatus paymentStatus, LocalDateTime paymentGuidedAt, LocalDateTime paymentDueAt,
            LocalDateTime cancelledAt, CancellationType cancellationType, CancellationReason cancellationReason,
            LocalDateTime refundedAt, LocalDateTime cardReadyAt, LocalDateTime physicalDispatchedAt,
            String photoRejectReason, ApplicantSummary applicant, ReceiverSummary receiver, long memberCount,
            LocalDateTime createdAt, String depositorName, Long version) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.applicationType = applicationType;
        this.cardTypeId = cardTypeId;
        this.cardTypeName = cardTypeName;
        this.issueType = issueType;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentGuidedAt = paymentGuidedAt;
        this.paymentDueAt = paymentDueAt;
        this.cancelledAt = cancelledAt;
        this.cancellationType = cancellationType;
        this.cancellationReason = cancellationReason;
        this.refundedAt = refundedAt;
        this.cardReadyAt = cardReadyAt;
        this.physicalDispatchedAt = physicalDispatchedAt;
        this.photoRejectReason = photoRejectReason;
        this.applicant = applicant;
        this.receiver = receiver;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.depositorName = depositorName;
        this.version = version;
    }

    // receiver는 issueType=MOBILE이면 항상 null(api.md API 7 참고) — 호출측이 조회 여부부터 결정해서 넘긴다.
    public static MyApplicationDetailResponse of(Application application, String cardTypeName, Applicant applicant,
            Receiver receiver, long memberCount) {
        return new MyApplicationDetailResponse(application.getId(), application.getApplicationNumber(),
                application.getApplicationType(), application.getCardTypeId(), cardTypeName,
                application.getIssueType(), application.getTotalQuantity(), application.getStatus(),
                application.getPaymentStatus(), application.getPaymentGuidedAt(), application.getPaymentDueAt(),
                application.getCancelledAt(), application.getCancellationType(), application.getCancellationReason(),
                application.getRefundedAt(), application.getCardReadyAt(), application.getPhysicalDispatchedAt(),
                application.getPhotoRejectReason(), ApplicantSummary.from(applicant),
                receiver == null ? null : ReceiverSummary.from(receiver), memberCount, application.getCreatedAt(),
                application.getDepositorName(), application.getVersion());
    }

    @Getter
    public static class ApplicantSummary {
        private final String name;
        private final String email;
        private final String phone;
        private final String organizationName;
        private final String department;

        private ApplicantSummary(String name, String email, String phone, String organizationName, String department) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.organizationName = organizationName;
            this.department = department;
        }

        public static ApplicantSummary from(Applicant applicant) {
            return new ApplicantSummary(applicant.getName(), applicant.getEmail(), applicant.getPhone(),
                    applicant.getOrganizationName(), applicant.getDepartment());
        }
    }

    @Getter
    public static class ReceiverSummary {
        private final String name;
        private final String phone;
        private final String zipCode;
        private final String address;
        private final String detailAddress;
        private final String deliveryRequest;
        private final String organizationName;
        private final String department;

        private ReceiverSummary(String name, String phone, String zipCode, String address, String detailAddress,
                String deliveryRequest, String organizationName, String department) {
            this.name = name;
            this.phone = phone;
            this.zipCode = zipCode;
            this.address = address;
            this.detailAddress = detailAddress;
            this.deliveryRequest = deliveryRequest;
            this.organizationName = organizationName;
            this.department = department;
        }

        public static ReceiverSummary from(Receiver receiver) {
            return new ReceiverSummary(receiver.getReceiverName(), receiver.getReceiverPhone(), receiver.getZipCode(),
                    receiver.getAddress(), receiver.getDetailAddress(), receiver.getDeliveryRequest(),
                    receiver.getOrganizationName(), receiver.getDepartment());
        }
    }
}
