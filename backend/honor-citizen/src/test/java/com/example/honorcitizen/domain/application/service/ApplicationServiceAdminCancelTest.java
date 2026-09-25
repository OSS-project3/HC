package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CancellationReason;
import com.example.honorcitizen.common.enums.CancellationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.AdminApplicationCancelResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 관리자 강제 취소(2026-09-25 확정, docs/collab/TODO.md "관리자 강제 취소 구현 체크리스트").
// 픽스처는 실제 신청 흐름(createIndividual/createGroup) 대신 Repository 직접 저장 +
// ReflectionTestUtils로 원하는 상태까지 빠르게 이동한다(ApplicationServicePhotoReuploadTest와
// 동일한 패턴) — 6개 허용 상태·부분 카드생성 단체 등 다양한 픽스처를 가볍게 구성하기 위해서다.
@SpringBootTest
class ApplicationServiceAdminCancelTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private ApplicationDailyLimitRepository applicationDailyLimitRepository;
    @Autowired
    private ApplicationDailyLimitService applicationDailyLimitService;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private Long ownerId;
    private CardType cardType;
    private int applicationSeq;

    @BeforeEach
    void setUp() {
        adminActivityLogRepository.deleteAll();
        applicationDailyLimitRepository.deleteAll();
        uploadFileRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("admin-cancel@example.com", "oauth-admin-cancel", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("owner-cancel@example.com", "oauth-owner-cancel", "google", "Owner"));
        ownerId = owner.getId();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-admincancel", null, java.math.BigDecimal.ZERO));

        when(storageService.download(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(storageService.uploadBytes(anyString(), any(byte[].class), anyString())).thenReturn("stored");
    }

    private Application individualAt(ApplicationStatus status, PaymentStatus paymentStatus) {
        applicationSeq++;
        Application application = Application.createIndividual(
                ownerId, "APP-2026-30" + String.format("%04d", applicationSeq), cardType.getId(), IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(application, "status", status);
        ReflectionTestUtils.setField(application, "paymentStatus", paymentStatus);
        application = applicationRepository.save(application);
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "owner@example.com", "010-1234-5678"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(application.getId(), "Hong Gildong",
                LocalDate.of(1990, 1, 1), "US", LocalTime.of(9, 0), "Seoul", Gender.MALE, null, null, null,
                "photos/face.jpg"));
        return application;
    }

    @Test
    void adminCancelsSubmittedApplicationAndReleasesSlotAndLogsOnce() {
        Application application = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        applicationDailyLimitService.reserveSlot(ownerId, ApplicationDailyLimitService.toCountDate(application.getCreatedAt()));
        clearInvocations(storageService);

        AdminApplicationCancelResponse response = applicationService.cancelByAdmin(adminId, application.getId(), "중복 신청으로 취소");

        assertThat(response.isFirstCancellation()).isTrue();
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(response.getCancellationType()).isEqualTo(CancellationType.ADMIN);
        assertThat(response.getCancellationReason()).isEqualTo(CancellationReason.ADMIN_DECISION);
        assertThat(response.getCancellationMemo()).isEqualTo("중복 신청으로 취소");

        Application saved = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(saved.getCancellationMemo()).isEqualTo("중복 신청으로 취소");
        assertThat(applicationMemberRepository.findByApplicationId(application.getId()).get(0).getPhotoPath()).isNull();
        assertThat(applicationDailyLimitRepository.findByUserIdAndCountDate(
                ownerId, ApplicationDailyLimitService.toCountDate(application.getCreatedAt())).orElseThrow().getCount())
                .isZero();
        verify(storageService, times(1)).delete(anyString());

        assertThat(adminActivityLogRepository.findAll())
                .filteredOn(log -> log.getActionType().equals(AdminActivityLog.APPLICATION_CANCEL))
                .hasSize(1)
                .allSatisfy(log -> {
                    assertThat(log.getAdminId()).isEqualTo(adminId);
                    assertThat(log.getTargetId()).isEqualTo(application.getId());
                });
    }

    @Test
    void adminCancelIsIdempotentAndDoesNotDuplicateAuditLogOrReleaseSlotTwice() {
        Application application = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        applicationDailyLimitService.reserveSlot(ownerId, ApplicationDailyLimitService.toCountDate(application.getCreatedAt()));

        applicationService.cancelByAdmin(adminId, application.getId(), "최초 사유");
        AdminApplicationCancelResponse second = applicationService.cancelByAdmin(adminId, application.getId(), "두 번째 사유");

        assertThat(second.isFirstCancellation()).isFalse();
        Application saved = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(saved.getCancellationMemo()).isEqualTo("최초 사유");
        assertThat(adminActivityLogRepository.findAll())
                .filteredOn(log -> log.getActionType().equals(AdminActivityLog.APPLICATION_CANCEL))
                .hasSize(1);
        assertThat(applicationDailyLimitRepository.findByUserIdAndCountDate(
                ownerId, ApplicationDailyLimitService.toCountDate(application.getCreatedAt())).orElseThrow().getCount())
                .isZero();
    }

    @Test
    void nonAdminCallerIsForbiddenAndApplicationIsUnchanged() {
        Application application = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);

        assertThatThrownBy(() -> applicationService.cancelByAdmin(ownerId, application.getId(), "사유"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void completedApplicationCannotBeCancelledByAdmin() {
        Application application = individualAt(ApplicationStatus.COMPLETED, PaymentStatus.CONFIRMED);

        assertThatThrownBy(() -> applicationService.cancelByAdmin(adminId, application.getId(), "사유"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.COMPLETED);
    }

    @Test
    void blankOrOversizedMemoIsRejectedAsBadRequestWithoutChangingApplication() {
        Application blank = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        assertThatThrownBy(() -> applicationService.cancelByAdmin(adminId, blank.getId(), "   "))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(applicationRepository.findById(blank.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.SUBMITTED);

        Application tooLong = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        assertThatThrownBy(() -> applicationService.cancelByAdmin(adminId, tooLong.getId(), "메".repeat(501)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(applicationRepository.findById(tooLong.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void paymentStatusAndRefundedAtAreUntouchedByAdminCancellation() {
        Application waiting = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        applicationService.cancelByAdmin(adminId, waiting.getId(), "사유");
        Application savedWaiting = applicationRepository.findById(waiting.getId()).orElseThrow();
        assertThat(savedWaiting.getPaymentStatus()).isEqualTo(PaymentStatus.WAITING);
        assertThat(savedWaiting.getRefundedAt()).isNull();

        Application confirmed = individualAt(ApplicationStatus.REVIEWING, PaymentStatus.CONFIRMED);
        AdminApplicationCancelResponse response = applicationService.cancelByAdmin(adminId, confirmed.getId(), "사유");
        assertThat(response.isRefundRequired()).isTrue();
        Application savedConfirmed = applicationRepository.findById(confirmed.getId()).orElseThrow();
        assertThat(savedConfirmed.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(savedConfirmed.getRefundedAt()).isNull();
    }

    // 관리자 취소만의 고유 요구사항(2026-09-25) — 사용자 취소는 카드가 생성될 수 없는 상태에서만
    // 허용되지만, 관리자 취소는 PRODUCTION_READY/PRODUCING에서도 허용돼 일부 멤버만 카드가 생성된
    // 단체 신청도 있을 수 있다. 카드가 있는 멤버·없는 멤버 모두 빠짐없이 정리되는지 확인한다.
    @Test
    void groupCancellationClearsPartiallyGeneratedCardImagesForEveryMember() {
        Application group = Application.createGroup(
                ownerId, "APP-2026-300002", cardType.getId(), IssueType.MOBILE, true, 2, null, null, null);
        ReflectionTestUtils.setField(group, "status", ApplicationStatus.PRODUCTION_READY);
        ReflectionTestUtils.setField(group, "paymentStatus", PaymentStatus.CONFIRMED);
        group = applicationRepository.save(group);
        Long groupId = group.getId();
        applicantRepository.save(Applicant.createGroup(groupId, "담당자", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));

        ApplicationMember withCard = ApplicationMember.createGroupRow(groupId, "James Park",
                LocalDate.of(1995, 2, 7), "US", LocalTime.of(10, 0), "Seoul", Gender.MALE, null,
                "james@example.com", "010-0000-0001", "Seoul", null, null, "photos/james.jpg");
        withCard.assignCardImages("applications/300002/members/1/front.png", "applications/300002/members/1/back.png",
                LocalDate.now());
        applicationMemberRepository.save(withCard);

        ApplicationMember withoutCard = ApplicationMember.createGroupRow(groupId, "Yuki Sato",
                LocalDate.of(1996, 3, 8), "JP", LocalTime.of(11, 0), "Tokyo", Gender.FEMALE, null,
                "yuki@example.com", "010-0000-0002", "Tokyo", null, null, "photos/yuki.jpg");
        applicationMemberRepository.save(withoutCard);

        clearInvocations(storageService);
        applicationService.cancelByAdmin(adminId, groupId, "행사 취소로 전원 취소");

        assertThat(applicationMemberRepository.findByApplicationId(groupId))
                .allSatisfy(member -> {
                    assertThat(member.getPhotoPath()).isNull();
                    assertThat(member.getCardFrontPath()).isNull();
                    assertThat(member.getCardBackPath()).isNull();
                });
        // 3명분 파일(사진 2 + 카드 앞/뒤 2)이 지워진다: james 사진+앞+뒤(3) + yuki 사진(1) = 4건.
        verify(storageService, times(4)).delete(anyString());
    }

    @Test
    void rollbackDoesNotReleaseSlotOrDeleteS3Files() {
        Application application = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        applicationDailyLimitService.reserveSlot(ownerId, ApplicationDailyLimitService.toCountDate(application.getCreatedAt()));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        clearInvocations(storageService);

        assertThatThrownBy(() -> transaction.executeWithoutResult(ignored -> {
            applicationService.cancelByAdmin(adminId, application.getId(), "사유");
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(applicationDailyLimitRepository.findByUserIdAndCountDate(
                ownerId, ApplicationDailyLimitService.toCountDate(application.getCreatedAt())).orElseThrow().getCount())
                .isEqualTo(1);
        assertThat(applicationMemberRepository.findByApplicationId(application.getId()).get(0).getPhotoPath())
                .isNotNull();
        verify(storageService, never()).delete(anyString());
        assertThat(adminActivityLogRepository.findAll()).isEmpty();
    }

    @Test
    void s3DeleteFailureDoesNotRollbackCommittedCancellation() {
        Application application = individualAt(ApplicationStatus.SUBMITTED, PaymentStatus.WAITING);
        clearInvocations(storageService);
        doThrow(new IllegalStateException("S3 unavailable")).when(storageService).delete(anyString());

        applicationService.cancelByAdmin(adminId, application.getId(), "사유");

        assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.CANCELLED);
    }
}
