package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CancellationReason;
import com.example.honorcitizen.common.enums.CancellationType;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.enums.StudentTextColor;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.Receiver;
import lombok.Getter;

import java.time.LocalDate;
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
    private final LocalDateTime cardReadyAt;
    private final LocalDateTime physicalDispatchedAt;
    // 관리자가 화면을 닫았다 다시 열어도 카드 제작 진행 상태를 복원할 수 있도록 노출한다(2026-09-13
    // 추가) — 셋 다 이미 Application에 저장돼 있던 값이고, 신규 저장 로직은 없다.
    private final Integer zodiacDesignSet;
    private final Long cardDesignId;
    private final LocalDate cardIssueDate;
    // 학생증 앞·뒷면 텍스트 색상(checklist.md §6) — zodiacDesignSet과 동일한 이유로 새로고침 후
    // 복원용 노출. 카드 생성 전이면 null(미확정) — 프론트가 기본값(DARK_GRAY)으로 해석한다.
    private final StudentTextColor studentFrontTextColor;
    private final StudentTextColor studentBackTextColor;
    private final String photoRejectReason;
    private final ApplicantSummary applicant;
    private final ReceiverSummary receiver;
    private final long memberCount;
    private final LocalDateTime createdAt;
    private final String depositorName;
    // 카드 표기용 주소 — 개인 신청(멤버 1명)만 노출한다. 단체는 구성원별 상세를 이 응답에서
    // 다루지 않으므로(407행 주석 "별도 API로 분리 예정") 항상 null. 배송용 ReceiverSummary.address와는
    // 별도 값이다(2026-09-13, 개인 신청 주소 누락 검증 후속 조치로 추가).
    private final String memberAddress;
    // 낙관적 락 버전 — 카드번호 일괄 저장(PUT .../card-numbers)의 applicationVersion 대조용.
    private final Long version;

    private MyApplicationDetailResponse(Long applicationId, String applicationNumber, ApplicationType applicationType,
            Long cardTypeId, String cardTypeName, IssueType issueType, int totalQuantity, ApplicationStatus status,
            PaymentStatus paymentStatus, LocalDateTime paymentGuidedAt, LocalDateTime paymentDueAt,
            LocalDateTime cancelledAt, CancellationType cancellationType, CancellationReason cancellationReason,
            LocalDateTime cardReadyAt, LocalDateTime physicalDispatchedAt,
            Integer zodiacDesignSet, Long cardDesignId, LocalDate cardIssueDate,
            StudentTextColor studentFrontTextColor, StudentTextColor studentBackTextColor,
            String photoRejectReason, ApplicantSummary applicant, ReceiverSummary receiver, long memberCount,
            LocalDateTime createdAt, String depositorName, String memberAddress, Long version) {
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
        this.cardReadyAt = cardReadyAt;
        this.physicalDispatchedAt = physicalDispatchedAt;
        this.zodiacDesignSet = zodiacDesignSet;
        this.cardDesignId = cardDesignId;
        this.cardIssueDate = cardIssueDate;
        this.studentFrontTextColor = studentFrontTextColor;
        this.studentBackTextColor = studentBackTextColor;
        this.photoRejectReason = photoRejectReason;
        this.applicant = applicant;
        this.receiver = receiver;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.depositorName = depositorName;
        this.memberAddress = memberAddress;
        this.version = version;
    }

    // receiver는 issueType=MOBILE이면 항상 null(api.md API 7 참고) — 호출측이 조회 여부부터 결정해서 넘긴다.
    // memberAddress는 개인 신청일 때만 호출측이 그 1명의 ApplicationMember.address를 조회해서 넘긴다
    // (단체는 null 그대로 전달).
    public static MyApplicationDetailResponse of(Application application, String cardTypeName, Applicant applicant,
            Receiver receiver, long memberCount, String memberAddress) {
        return new MyApplicationDetailResponse(application.getId(), application.getApplicationNumber(),
                application.getApplicationType(), application.getCardTypeId(), cardTypeName,
                application.getIssueType(), application.getTotalQuantity(), application.getStatus(),
                application.getPaymentStatus(), application.getPaymentGuidedAt(), application.getPaymentDueAt(),
                application.getCancelledAt(), application.getCancellationType(), application.getCancellationReason(),
                application.getCardReadyAt(), application.getPhysicalDispatchedAt(),
                application.getZodiacDesignSet(), application.getCardDesignId(), application.getCardIssueDate(),
                application.getStudentFrontTextColor(), application.getStudentBackTextColor(),
                application.getPhotoRejectReason(), ApplicantSummary.from(applicant),
                receiver == null ? null : ReceiverSummary.from(receiver), memberCount, application.getCreatedAt(),
                application.getDepositorName(), memberAddress, application.getVersion());
    }

    // 영어 응답용 사본 — 자유 텍스트인 photoRejectReason만 번역한다(cardTypeName·status 등은 그대로).
    public MyApplicationDetailResponse withTranslated(String photoRejectReason) {
        return new MyApplicationDetailResponse(applicationId, applicationNumber, applicationType, cardTypeId,
                cardTypeName, issueType, totalQuantity, status, paymentStatus, paymentGuidedAt, paymentDueAt,
                cancelledAt, cancellationType, cancellationReason, cardReadyAt, physicalDispatchedAt,
                zodiacDesignSet, cardDesignId, cardIssueDate, studentFrontTextColor, studentBackTextColor,
                photoRejectReason, applicant, receiver, memberCount, createdAt, depositorName, memberAddress, version);
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
