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

    // 신청 진행 상태(PAYMENT_PENDING → RECEIVED → REVIEWING → ... ). 전이 규칙은 transitionTo/ApplicationStatus 참고
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

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

    // 개인 신청: 대상자(ApplicationMember)는 항상 1명, submitFileId(제출 ZIP)는 없음
    public static Application createIndividual(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId) {
        Application application = base(userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant);
        application.applicationType = ApplicationType.INDIVIDUAL;
        application.totalQuantity = 1;
        application.logoFileId = logoFileId;
        application.sealFileId = sealFileId;
        return application;
    }

    // 단체 신청: 대상자는 제출 ZIP(엑셀) 행 수만큼(totalQuantity), submitFileId로 원본 ZIP을 추적
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

    // 아래 confirmPayment~cancel까지는 전부 상태 전이(transitionTo)를 감싸는 것 — 허용되는 전이 경로는
    // ApplicationStatus.canTransitionTo에 정의돼 있고, 여기서 벗어나면 INVALID_STATUS_TRANSITION.
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
