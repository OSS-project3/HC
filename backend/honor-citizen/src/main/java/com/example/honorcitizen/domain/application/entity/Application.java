package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CancellationReason;
import com.example.honorcitizen.common.enums.CancellationType;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.enums.SchoolType;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 신청의 중심 엔티티. Applicant/ApplicationMember/Receiver는 JPA 연관관계(@OneToMany 등) 없이
// applicationId(Long) 컬럼으로만 이 엔티티를 참조한다 — 조회는 각 Repository의 findByApplicationId로 직접 수행.
@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "APP-{연도}-{6자리}" 형식. count+1이 아니라 application_seq DB Sequence로 채번(동시성 안전)
    @Column(nullable = false, unique = true, length = 20)
    private String applicationNumber;

    // 신청한 로그인 계정의 User FK (User 엔티티와도 JPA 연관관계 없이 Long으로만 참조)
    @Column(nullable = false)
    private Long userId;

    // 명예한국인증/명예시민증/방문증/학생증 등 CardType FK
    @Column(nullable = false)
    private Long cardTypeId;

    // INDIVIDUAL(개인) / GROUP(단체)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationType applicationType;

    // 신청 업무 진행 상태. 결제 확인 여부는 paymentStatus로 독립 관리한다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    private LocalDateTime paymentGuidedAt;

    private LocalDateTime paymentDueAt;

    // 무통장 입금 대조용 입금자명 — 신청 완료 화면(제출 직후, SUBMITTED·결제대기)에서 신청자가 입력한다.
    // 생성 시점엔 없고 별도 등록(registerDepositorName)으로 채워지므로 팩토리 인자가 아니다. 결제 확인 전까지만 수정 가능.
    @Column(length = 60)
    private String depositorName;

    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CancellationType cancellationType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CancellationReason cancellationReason;

    private LocalDateTime refundedAt;

    private LocalDateTime cardReadyAt;

    private LocalDateTime physicalDispatchedAt;

    // 실물 배송 운송장번호 — physicalDispatchedAt과 함께 markPhysicalDispatched에서만 채워진다.
    // 감사로그(AdminActivityLog)와 별도로 두는 이유: 조회 화면에서 계속 참조되는 업무 상태 데이터라서.
    @Column(length = 100)
    private String trackingNumber;

    @Version
    @Column(nullable = false)
    private Long version;

    // 신청 시점 sameAsApplicant 요청값을 기록만 하는 스냅샷 — 저장 이후 어떤 로직도 이 필드를 다시 읽지 않는다.
    // 실제 배송지 복사 여부/범위는 저장 시점에 ApplicationPersistenceService가 receiverRequest로 바로 결정한다.
    // 신청자가 마이페이지에서 주소를 바꾸면,
    @Column(nullable = false)
    private boolean receiverSameAsApplicant;

    // 카드 발급 매수. 개인은 항상 1, 단체는 제출 ZIP 엑셀의 데이터 행 수
    @Column(nullable = false)
    private int totalQuantity;

    // MOBILE(모바일만) / MOBILE_AND_PHYSICAL(모바일+실물배송). MOBILE_AND_PHYSICAL일 때만 Receiver가 존재
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueType issueType;

    // 관리자가 심사 후 배정하는 카드 디자인 — 신청 시점엔 항상 null(이번 리팩터링 범위 밖)
    private Long cardDesignId;

    // 로고 UploadFile FK — 의미가 카드종류/신청유형에 따라 다르다.
    //   개인+학생증: 학교 로고(선택 안 하면 null). 개인+비학생증: 로고 자체가 없어 항상 null.
    //   단체(GROUP): 학생증이든 아니든 항상 필수 — 비학생증 단체 신청에선 사실상 "회사 로고".
    private Long logoFileId;

    // 직인 UploadFile FK — logoFileId와 같은 방식으로 의미가 갈린다.
    //   개인+학생증: 학교 직인(선택). 단체+학생증: 선택. 단체+비학생증: 필수(="회사 직인").
    private Long sealFileId;

    // 단체 신청 원본 제출 ZIP의 UploadFile FK. 개인 신청은 항상 null
    private Long submitFileId;

    @Column(length = 500)
    private String photoRejectReason;

    // 학생증(STUDENT)일 때만 값이 있다(개인·단체 공통, 신청서 전체에 1개) — 그 외 카드종류는 항상 null.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Orientation orientation;

    // 학생증(STUDENT)일 때만 값이 있다(개인·단체 공통). UNIVERSITY일 때만 ApplicationMember.studentId/department가 필수가 된다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SchoolType schoolType;

    // 학생증(STUDENT)일 때만 값이 있다(개인·단체 공통, 신청서 전체에 1개) — UNIVERSITY/HIGH_SCHOOL 둘 다 필수.
    // orientation/schoolType과 동일하게 DB는 nullable, 학생증 여부에 따른 필수 검증은 서비스 레벨(validateStudentFields)에서 강제한다.
    @Column(length = 20)
    private String schoolName;

    // 기존 호출부(비학생증 픽스처 다수)와의 하위 호환용 — orientation/schoolType/schoolName 없이 호출하면 전부 null로 생성한다.
    public static Application createIndividual(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId) {
        return createIndividual(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant,
                logoFileId, sealFileId, null, null, null);
    }

    // 기존 호출부(orientation/schoolType까지만 반영된 픽스처)와의 하위 호환용 — schoolName 없이 호출하면 null로 생성한다.
    public static Application createIndividual(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId,
            Orientation orientation, SchoolType schoolType) {
        return createIndividual(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant,
                logoFileId, sealFileId, orientation, schoolType, null);
    }

    // 개인 신청: 대상자(ApplicationMember)는 항상 1명, submitFileId(제출 ZIP)는 없음
    public static Application createIndividual(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId,
            Orientation orientation, SchoolType schoolType, String schoolName) {
        Application application = base(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant);
        application.applicationType = ApplicationType.INDIVIDUAL;
        application.totalQuantity = 1;
        application.logoFileId = logoFileId;
        application.sealFileId = sealFileId;
        application.orientation = orientation;
        application.schoolType = schoolType;
        application.schoolName = schoolName;
        return application;
    }

    // 기존 호출부와의 하위 호환용 — orientation/schoolType/schoolName 없이 호출하면 전부 null로 생성한다.
    public static Application createGroup(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, int totalQuantity,
            Long logoFileId, Long sealFileId, Long submitFileId) {
        return createGroup(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant,
                totalQuantity, logoFileId, sealFileId, submitFileId, null, null, null);
    }

    // 기존 호출부(orientation/schoolType까지만 반영된 픽스처)와의 하위 호환용 — schoolName 없이 호출하면 null로 생성한다.
    public static Application createGroup(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, int totalQuantity,
            Long logoFileId, Long sealFileId, Long submitFileId, Orientation orientation, SchoolType schoolType) {
        return createGroup(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant,
                totalQuantity, logoFileId, sealFileId, submitFileId, orientation, schoolType, null);
    }

    // 단체 신청: 대상자는 제출 ZIP(엑셀) 행 수만큼(totalQuantity), submitFileId로 원본 ZIP을 추적
    public static Application createGroup(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, int totalQuantity,
            Long logoFileId, Long sealFileId, Long submitFileId, Orientation orientation, SchoolType schoolType,
            String schoolName) {
        Application application = base(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant);
        application.applicationType = ApplicationType.GROUP;
        application.totalQuantity = totalQuantity;
        application.logoFileId = logoFileId;
        application.sealFileId = sealFileId;
        application.submitFileId = submitFileId;
        application.orientation = orientation;
        application.schoolType = schoolType;
        application.schoolName = schoolName;
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
        application.status = ApplicationStatus.SUBMITTED;
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

    public boolean guidePayment(LocalDateTime guidedAt) {
        requireTimestamp(guidedAt);
        if (this.paymentGuidedAt != null) {
            return false;
        }
        if (this.status != ApplicationStatus.SUBMITTED || this.paymentStatus != PaymentStatus.WAITING) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.paymentGuidedAt = guidedAt;
        this.paymentDueAt = guidedAt.plusDays(3);
        return true;
    }

    public boolean confirmPayment() {
        if (this.paymentStatus == PaymentStatus.CONFIRMED) {
            return false;
        }
        if (this.status != ApplicationStatus.SUBMITTED && this.status != ApplicationStatus.CANCELLED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.paymentStatus = PaymentStatus.CONFIRMED;
        return true;
    }

    // 입금자명 등록/수정 — 결제 확인 전(SUBMITTED·WAITING)에만 허용. 신청자 본인 검증은 서비스에서 한다.
    public void registerDepositorName(String depositorName) {
        if (this.status != ApplicationStatus.SUBMITTED || this.paymentStatus != PaymentStatus.WAITING) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.depositorName = depositorName;
    }

    public void startReview() {
        if (this.status != ApplicationStatus.SUBMITTED || this.paymentStatus != PaymentStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
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

    public void completeNaming() {
        transitionTo(ApplicationStatus.PRODUCTION_READY);
    }

    public void startProducing() {
        if (this.paymentStatus != PaymentStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        transitionTo(ApplicationStatus.PRODUCING);
    }

    public boolean markCardReady(LocalDateTime readyAt) {
        requireTimestamp(readyAt);
        if (this.cardReadyAt != null) {
            return false;
        }
        if (this.status != ApplicationStatus.PRODUCING) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.cardReadyAt = readyAt;
        if (this.issueType == IssueType.MOBILE) {
            transitionTo(ApplicationStatus.COMPLETED);
        }
        return true;
    }

    public boolean markPhysicalDispatched(LocalDateTime dispatchedAt, String trackingNumber) {
        requireTimestamp(dispatchedAt);
        if (this.physicalDispatchedAt != null) {
            return false;
        }
        if (this.issueType != IssueType.MOBILE_AND_PHYSICAL
                || this.status != ApplicationStatus.PRODUCING
                || this.cardReadyAt == null) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.trackingNumber = trackingNumber;
        this.physicalDispatchedAt = dispatchedAt;
        transitionTo(ApplicationStatus.COMPLETED);
        return true;
    }

    public boolean canCancelByUser() {
        return this.status == ApplicationStatus.SUBMITTED
                || this.status == ApplicationStatus.REVIEWING
                || this.status == ApplicationStatus.PHOTO_REJECTED;
    }

    public boolean cancelByUser(LocalDateTime cancelledAt) {
        if (this.status == ApplicationStatus.CANCELLED) {
            return false;
        }
        requireTimestamp(cancelledAt);
        if (!canCancelByUser()) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        cancel(cancelledAt, CancellationType.USER, CancellationReason.USER_REQUEST);
        return true;
    }

    public boolean cancelForPaymentTimeout(LocalDateTime cancelledAt) {
        if (this.status == ApplicationStatus.CANCELLED) {
            return false;
        }
        requireTimestamp(cancelledAt);
        if (this.status != ApplicationStatus.SUBMITTED
                || this.paymentStatus != PaymentStatus.WAITING
                || this.paymentDueAt == null
                || cancelledAt.isBefore(this.paymentDueAt)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        cancel(cancelledAt, CancellationType.SYSTEM, CancellationReason.PAYMENT_TIMEOUT);
        return true;
    }

    public boolean markRefunded(LocalDateTime refundedAt) {
        if (this.refundedAt != null) {
            return false;
        }
        requireTimestamp(refundedAt);
        if (this.status != ApplicationStatus.CANCELLED || this.paymentStatus != PaymentStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.refundedAt = refundedAt;
        return true;
    }

    public void clearManagedFileReferences() {
        this.logoFileId = null;
        this.sealFileId = null;
        this.submitFileId = null;
    }

    private void cancel(LocalDateTime cancelledAt, CancellationType type, CancellationReason reason) {
        transitionTo(ApplicationStatus.CANCELLED);
        this.cancelledAt = cancelledAt;
        this.cancellationType = type;
        this.cancellationReason = reason;
    }

    private void transitionTo(ApplicationStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = next;
    }

    private void requireTimestamp(LocalDateTime value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
