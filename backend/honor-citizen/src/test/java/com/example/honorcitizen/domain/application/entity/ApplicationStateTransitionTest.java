package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationStateTransitionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 12, 0);

    @Test
    void newApplicationStartsAsSubmittedAndWaiting() {
        Application application = individual(IssueType.MOBILE);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.getPaymentStatus()).isEqualTo(PaymentStatus.WAITING);
    }

    @Test
    void paymentConfirmationIsIdempotentAndDoesNotChangeApplicationStatus() {
        Application application = individual(IssueType.MOBILE);

        assertThat(application.confirmPayment()).isTrue();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        assertThat(application.confirmPayment()).isFalse();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void depositorNameIsRegisteredWhileWaitingForPayment() {
        Application application = individual(IssueType.MOBILE);

        application.registerDepositorName("홍길동");

        assertThat(application.getDepositorName()).isEqualTo("홍길동");
    }

    @Test
    void depositorNameIsRejectedAfterPaymentConfirmed() {
        Application application = individual(IssueType.MOBILE);
        application.confirmPayment();

        assertThatThrownBy(() -> application.registerDepositorName("홍길동"))
                .isInstanceOf(com.example.honorcitizen.common.exception.CustomException.class);
        assertThat(application.getDepositorName()).isNull();
    }

    @Test
    void mobileApplicationTransitionsThroughTheCompleteLifecycle() {
        Application application = individual(IssueType.MOBILE);
        application.confirmPayment();

        application.startReview();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REVIEWING);

        application.rejectPhoto("사진이 흐립니다.");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PHOTO_REJECTED);

        application.resubmitForReview(null);
        application.approveToNaming();
        application.completeNaming();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PRODUCTION_READY);

        application.startProducing();
        application.markCardReady(NOW);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(application.getCardReadyAt()).isEqualTo(NOW);
    }

    @Test
    void physicalApplicationCompletesOnlyAfterCarrierHandoff() {
        Application application = individual(IssueType.MOBILE_AND_PHYSICAL);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();

        application.markCardReady(NOW);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PRODUCING);

        application.markPhysicalDispatched(NOW.plusHours(1), "1234567890");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(application.getPhysicalDispatchedAt()).isEqualTo(NOW.plusHours(1));
        assertThat(application.getTrackingNumber()).isEqualTo("1234567890");
    }

    @Test
    void reviewCannotStartBeforePaymentConfirmation() {
        Application application = individual(IssueType.MOBILE);

        assertThatThrownBy(application::startReview)
                .isInstanceOf(CustomException.class);
    }

    @Test
    void userCancellationIsIdempotentAndRejectedAfterNamingStarts() {
        Application cancellable = individual(IssueType.MOBILE);

        assertThat(cancellable.cancelByUser(NOW)).isTrue();
        assertThat(cancellable.cancelByUser(NOW.plusMinutes(1))).isFalse();
        assertThat(cancellable.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(cancellable.getCancelledAt()).isEqualTo(NOW);

        Application tooLate = individual(IssueType.MOBILE);
        tooLate.confirmPayment();
        tooLate.startReview();
        tooLate.approveToNaming();

        assertThatThrownBy(() -> tooLate.cancelByUser(NOW))
                .isInstanceOf(CustomException.class);
    }

    // 3-B(2026-08-30): 카드 생성 최초 확정 — cardDesignId·cardIssueDate를 한 번에 확정한다.
    @Test
    void confirmCardGenerationSetsDesignAndIssueDateTogether() {
        Application application = individual(IssueType.MOBILE);
        assertThat(application.getCardDesignId()).isNull();
        assertThat(application.getCardIssueDate()).isNull();

        application.confirmCardGeneration(7L, LocalDateTime.of(2026, 9, 1, 0, 0).toLocalDate());

        assertThat(application.getCardDesignId()).isEqualTo(7L);
        assertThat(application.getCardIssueDate()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0).toLocalDate());
    }

    // 2026-09-06: 십이간지 캐릭터 디자인 세트 — 1~3만 허용, cardDesignId와 달리 잠금이 없어
    // 카드 생성 확정 이후에도 계속 바꿀 수 있다(정책 확정).
    @Test
    void zodiacDesignSetAcceptsOnlyOneToThree() {
        Application application = individual(IssueType.MOBILE);
        assertThat(application.getZodiacDesignSet()).isNull();

        application.assignZodiacDesignSet(1);
        assertThat(application.getZodiacDesignSet()).isEqualTo(1);
        application.assignZodiacDesignSet(3);
        assertThat(application.getZodiacDesignSet()).isEqualTo(3);

        assertThatThrownBy(() -> application.assignZodiacDesignSet(0)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> application.assignZodiacDesignSet(4)).isInstanceOf(CustomException.class);
    }

    @Test
    void zodiacDesignSetCanBeChangedEvenAfterCardGenerationConfirmed() {
        Application application = individual(IssueType.MOBILE);
        application.assignZodiacDesignSet(1);
        application.confirmCardGeneration(7L, LocalDateTime.of(2026, 9, 1, 0, 0).toLocalDate());

        application.assignZodiacDesignSet(2);

        assertThat(application.getZodiacDesignSet()).isEqualTo(2);
    }

    private Application individual(IssueType issueType) {
        return Application.createIndividual(
                1L, "APP-2026-000001", 10L, issueType, issueType == IssueType.MOBILE, null, null);
    }
}
