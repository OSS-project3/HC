package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.scheduler.ApplicationPaymentTimeoutScheduler;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ApplicationPaymentWorkflowTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationPaymentTimeoutScheduler scheduler;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private ApplicationDailyLimitRepository dailyLimitRepository;
    @Autowired
    private ApplicationDailyLimitService dailyLimitService;
    @Autowired
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private User user;
    private User admin;
    private CardType cardType;
    private int sequence;

    @BeforeEach
    void setUp() {
        adminActivityLogRepository.deleteAll();
        dailyLimitRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        uploadFileRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        user = User.createOAuthUser("payment-user@example.com", "oauth-payment-user", "google", "User");
        user.agreeTerms(true, true, true);
        user = userRepository.save(user);

        admin = User.createOAuthUser("payment-admin@example.com", "oauth-payment-admin", "google", "Admin");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        admin = userRepository.save(admin);

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-payment", null, BigDecimal.ZERO));
        sequence = 1;
    }

    private Application saveApplication() {
        return applicationRepository.save(Application.createIndividual(
                user.getId(), "APP-PAYMENT-" + sequence++, cardType.getId(),
                IssueType.MOBILE, true, null, null));
    }

    @Test
    void paymentGuideRecordsFirstTimestampAndKeepsDeadlineOnRetry() {
        Application application = saveApplication();

        Application first = applicationService.guidePayment(admin.getId(), application.getId());
        LocalDateTime guidedAt = first.getPaymentGuidedAt();
        LocalDateTime dueAt = first.getPaymentDueAt();
        Application retried = applicationService.guidePayment(admin.getId(), application.getId());

        assertThat(dueAt).isEqualTo(guidedAt.plusDays(3));
        assertThat(retried.getPaymentGuidedAt()).isEqualTo(guidedAt);
        assertThat(retried.getPaymentDueAt()).isEqualTo(dueAt);
        assertThat(adminActivityLogRepository.count()).isZero();
    }

    @Test
    void paymentConfirmationIsIdempotentAndLogsOnlyFirstChange() {
        Application application = saveApplication();

        applicationService.confirmPayment(admin.getId(), application.getId());
        applicationService.confirmPayment(admin.getId(), application.getId());

        Application confirmed = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(confirmed.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(adminActivityLogRepository.count()).isEqualTo(1);
        AdminActivityLog log = adminActivityLogRepository.findAll().get(0);
        assertThat(log.getAdminId()).isEqualTo(admin.getId());
        assertThat(log.getTargetId()).isEqualTo(application.getId());
        assertThat(log.getActionType()).isEqualTo(AdminActivityLog.PAYMENT_CONFIRMED);
    }

    @Test
    void nonAdminCannotGuideOrConfirmPayment() {
        Application application = saveApplication();

        assertThatThrownBy(() -> applicationService.guidePayment(user.getId(), application.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> applicationService.confirmPayment(user.getId(), application.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void schedulerCancelsExpiredUnpaidApplicationAndCleansFilesAfterCommit() {
        Application application = saveApplication();
        application.guidePayment(LocalDateTime.now().minusDays(3).minusMinutes(1));
        application = applicationRepository.save(application);
        dailyLimitService.reserveSlot(
                user.getId(), ApplicationDailyLimitService.toCountDate(application.getCreatedAt()));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/expired.jpg"));
        clearInvocations(storageService);

        scheduler.cancelExpiredUnpaidApplications();

        Application cancelled = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.WAITING);
        assertThat(cancelled.getCancellationType().name()).isEqualTo("SYSTEM");
        assertThat(cancelled.getCancellationReason().name()).isEqualTo("PAYMENT_TIMEOUT");
        assertThat(dailyLimitRepository.findByUserIdAndCountDate(
                user.getId(), ApplicationDailyLimitService.toCountDate(application.getCreatedAt()))
                .orElseThrow().getCount()).isZero();
        assertThat(applicationMemberRepository.findById(member.getId()).orElseThrow().getPhotoPath()).isNull();
        verify(storageService, times(1)).delete(anyString());
    }

    @Test
    void latePaymentConfirmationDoesNotReactivateAutoCancelledApplication() {
        Application application = saveApplication();
        application.guidePayment(LocalDateTime.now().minusDays(3).minusMinutes(1));
        applicationRepository.save(application);
        scheduler.cancelExpiredUnpaidApplications();

        applicationService.confirmPayment(admin.getId(), application.getId());

        Application lateConfirmed = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(lateConfirmed.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(lateConfirmed.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(lateConfirmed.getRefundedAt()).isNull();
        assertThat(adminActivityLogRepository.count()).isEqualTo(1);
    }
}
