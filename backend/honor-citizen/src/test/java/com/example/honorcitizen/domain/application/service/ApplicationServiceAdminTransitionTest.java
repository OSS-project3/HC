package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationStatusResponse;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 관리자 신청 상태 전이 5종 — 전이 규칙 자체(Application 엔티티)는 이미
// ApplicationStateTransitionTest에서 커버하므로, 여기서는 Service가 그 규칙을 그대로 호출하고
// 인가·AdminActivityLog 기록을 정확히 하는지만 검증한다.
@SpringBootTest
class ApplicationServiceAdminTransitionTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    private Long adminId;

    @BeforeEach
    void setUp() {
        adminActivityLogRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("transition-admin@example.com", "oauth-transition-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();
    }

    private Application newApplication(String number, IssueType issueType) {
        User owner = userRepository.save(
                User.createOAuthUser(number + "@example.com", "oauth-" + number, "google", "Owner"));
        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-" + number, null, BigDecimal.valueOf(30000)));
        return applicationRepository.save(Application.createIndividual(
                owner.getId(), number, cardType.getId(), issueType, issueType == IssueType.MOBILE, null, null));
    }

    @Test
    void rejectPhotoTransitionsAndLogsReason() {
        Application application = newApplication("APP-2026-940001", IssueType.MOBILE);
        application.confirmPayment();
        application.startReview();
        applicationRepository.saveAndFlush(application);

        ApplicationStatusResponse response = applicationService.rejectPhoto(adminId, application.getId(), "사진이 흐립니다.");

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PHOTO_REJECTED);
        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getPhotoRejectReason()).isEqualTo("사진이 흐립니다.");
        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getActionType()).isEqualTo(AdminActivityLog.PHOTO_REJECT);
                    assertThat(log.getTargetId()).isEqualTo(application.getId());
                    assertThat(log.getDetail()).isEqualTo("사진이 흐립니다.");
                });
    }

    @Test
    void rejectPhotoBeforeReviewIsRejected() {
        Application application = newApplication("APP-2026-940002", IssueType.MOBILE);

        assertThatThrownBy(() -> applicationService.rejectPhoto(adminId, application.getId(), "사유"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void startProducingTransitionsAndLogs() {
        Application application = newApplication("APP-2026-940003", IssueType.MOBILE);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        applicationRepository.saveAndFlush(application);

        ApplicationStatusResponse response = applicationService.startProducing(adminId, application.getId());

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PRODUCING);
        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(log -> assertThat(log.getActionType()).isEqualTo(AdminActivityLog.PRODUCTION_START));
    }

    @Test
    void markCardReadyCompletesMobileOnlyApplicationAndLogs() {
        Application application = newApplication("APP-2026-940004", IssueType.MOBILE);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        applicationRepository.saveAndFlush(application);

        ApplicationStatusResponse response = applicationService.markCardReady(adminId, application.getId());

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getCardReadyAt()).isNotNull();
        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(log -> assertThat(log.getActionType()).isEqualTo(AdminActivityLog.CARD_ISSUE));
    }

    @Test
    void dispatchPhysicalStoresTrackingNumberAndCompletesAndLogs() {
        Application application = newApplication("APP-2026-940005", IssueType.MOBILE_AND_PHYSICAL);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        applicationRepository.saveAndFlush(application);
        applicationService.markCardReady(adminId, application.getId());

        ApplicationStatusResponse response =
                applicationService.dispatchPhysical(adminId, application.getId(), "1234567890");

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getTrackingNumber()).isEqualTo("1234567890");
        assertThat(reloaded.getPhysicalDispatchedAt()).isNotNull();
        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getActionType()).isEqualTo(AdminActivityLog.TRACKING_REGISTER);
                    assertThat(log.getDetail()).isEqualTo("1234567890");
                });
    }

    @Test
    void completeNamingTransitionsAndLogsWithoutKoreanNameConstant() {
        Application application = newApplication("APP-2026-940006", IssueType.MOBILE);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        applicationRepository.saveAndFlush(application);

        ApplicationStatusResponse response = applicationService.completeNaming(adminId, application.getId());

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PRODUCTION_READY);
        assertThat(adminActivityLogRepository.findAll())
                .hasSize(1)
                .anySatisfy(log -> assertThat(log.getActionType()).isEqualTo(AdminActivityLog.NAMING_COMPLETE));
    }

    @Test
    void rejectsForNonAdminCaller() {
        Application application = newApplication("APP-2026-940007", IssueType.MOBILE);
        User user = userRepository.save(
                User.createOAuthUser("transition-plain-user@example.com", "oauth-transition-plain", "google", "User"));

        assertThatThrownBy(() -> applicationService.startProducing(user.getId(), application.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsForMissingApplication() {
        assertThatThrownBy(() -> applicationService.completeNaming(adminId, 999999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }
}
