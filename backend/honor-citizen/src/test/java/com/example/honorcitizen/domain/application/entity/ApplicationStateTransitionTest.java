package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.enums.StudentTextColor;
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

    // 관리자 강제 취소(2026-09-25 확정) — 사용자 취소보다 훨씬 넓게 COMPLETED 전까지 전부 허용된다.
    @Test
    void adminCancellationIsAllowedFromEveryStatusUntilCompleted() {
        for (ApplicationStatus target : java.util.List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.REVIEWING,
                ApplicationStatus.PHOTO_REJECTED, ApplicationStatus.NAME_EDITING, ApplicationStatus.PRODUCTION_READY,
                ApplicationStatus.PRODUCING)) {
            Application application = applicationAt(target);

            assertThat(application.cancelByAdmin(NOW, "운영상 취소")).isTrue();

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(application.getCancelledAt()).isEqualTo(NOW);
            assertThat(application.getCancellationType()).isEqualTo(com.example.honorcitizen.common.enums.CancellationType.ADMIN);
            assertThat(application.getCancellationReason())
                    .isEqualTo(com.example.honorcitizen.common.enums.CancellationReason.ADMIN_DECISION);
            assertThat(application.getCancellationMemo()).isEqualTo("운영상 취소");
        }
    }

    @Test
    void adminCancellationIsRejectedOnceCompleted() {
        Application completed = applicationAt(ApplicationStatus.COMPLETED);

        assertThatThrownBy(() -> completed.cancelByAdmin(NOW, "사유"))
                .isInstanceOf(CustomException.class);
        assertThat(completed.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
    }

    @Test
    void adminCancellationIsIdempotentAndDoesNotOverwriteFirstRecord() {
        Application application = applicationAt(ApplicationStatus.SUBMITTED);

        assertThat(application.cancelByAdmin(NOW, "최초 사유")).isTrue();
        assertThat(application.cancelByAdmin(NOW.plusMinutes(1), "다른 관리자가 다시 입력한 사유")).isFalse();

        assertThat(application.getCancelledAt()).isEqualTo(NOW);
        assertThat(application.getCancellationMemo()).isEqualTo("최초 사유");
    }

    @Test
    void adminCancellationRejectsBlankOrNullMemoWithoutChangingStatus() {
        Application blank = applicationAt(ApplicationStatus.SUBMITTED);
        assertThatThrownBy(() -> blank.cancelByAdmin(NOW, "   "))
                .isInstanceOf(CustomException.class);
        assertThat(blank.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);

        Application nullMemo = applicationAt(ApplicationStatus.SUBMITTED);
        assertThatThrownBy(() -> nullMemo.cancelByAdmin(NOW, null))
                .isInstanceOf(CustomException.class);
        assertThat(nullMemo.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void adminCancellationAllowsExactly500CharsButRejects501() {
        Application tooLong = applicationAt(ApplicationStatus.SUBMITTED);
        assertThatThrownBy(() -> tooLong.cancelByAdmin(NOW, "메".repeat(501)))
                .isInstanceOf(CustomException.class);
        assertThat(tooLong.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);

        Application exact500 = applicationAt(ApplicationStatus.SUBMITTED);
        assertThat(exact500.cancelByAdmin(NOW, "메".repeat(500))).isTrue();
        assertThat(exact500.getCancellationMemo()).hasSize(500);
    }

    // 신청을 원하는 상태까지 실제 전이 메서드로 몰고 간다(결제확인 → 검토시작 → ... ) — 멤버 데이터 없이도
    // Entity 레벨 전이 자체는 항상 가능하다(멤버 검증은 Service 책임, 3-F 등).
    private Application applicationAt(ApplicationStatus target) {
        Application application = individual(IssueType.MOBILE);
        if (target == ApplicationStatus.SUBMITTED) {
            return application;
        }
        application.confirmPayment();
        application.startReview();
        if (target == ApplicationStatus.REVIEWING) {
            return application;
        }
        if (target == ApplicationStatus.PHOTO_REJECTED) {
            application.rejectPhoto("사진이 흐립니다.");
            return application;
        }
        application.approveToNaming();
        if (target == ApplicationStatus.NAME_EDITING) {
            return application;
        }
        application.completeNaming();
        if (target == ApplicationStatus.PRODUCTION_READY) {
            return application;
        }
        application.startProducing();
        if (target == ApplicationStatus.PRODUCING) {
            return application;
        }
        application.markCardReady(NOW);
        return application;
    }

    // 3-B(2026-08-30): 카드 생성 최초 확정 — cardDesignId·cardIssueDate를 한 번에 확정한다.
    @Test
    void confirmCardGenerationSetsDesignIssueDateAndStudentTextColorsTogether() {
        Application application = individual(IssueType.MOBILE);
        assertThat(application.getCardDesignId()).isNull();
        assertThat(application.getCardIssueDate()).isNull();

        application.confirmCardGeneration(7L, LocalDateTime.of(2026, 9, 1, 0, 0).toLocalDate(),
                StudentTextColor.WHITE, StudentTextColor.DARK_GRAY);

        assertThat(application.getCardDesignId()).isEqualTo(7L);
        assertThat(application.getCardIssueDate()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0).toLocalDate());
        assertThat(application.getStudentFrontTextColor()).isEqualTo(StudentTextColor.WHITE);
        assertThat(application.getStudentBackTextColor()).isEqualTo(StudentTextColor.DARK_GRAY);
    }

    // 2026-09-06: 십이간지 캐릭터 디자인 세트 — cardDesignId와 달리 잠금이 없어 카드 생성 확정
    // 이후에도 계속 바꿀 수 있다(정책 확정). 2026-09-13: 1~3 → 1~5로 확장(4/5는 2/3번 스타일의
    // 화이트 버전).
    @Test
    void zodiacDesignSetAcceptsOnlyOneToFive() {
        Application application = individual(IssueType.MOBILE);
        assertThat(application.getZodiacDesignSet()).isNull();

        application.assignZodiacDesignSet(1);
        assertThat(application.getZodiacDesignSet()).isEqualTo(1);
        application.assignZodiacDesignSet(5);
        assertThat(application.getZodiacDesignSet()).isEqualTo(5);

        assertThatThrownBy(() -> application.assignZodiacDesignSet(0)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> application.assignZodiacDesignSet(6)).isInstanceOf(CustomException.class);
    }

    @Test
    void zodiacDesignSetCanBeChangedEvenAfterCardGenerationConfirmed() {
        Application application = individual(IssueType.MOBILE);
        application.assignZodiacDesignSet(1);
        application.confirmCardGeneration(7L, LocalDateTime.of(2026, 9, 1, 0, 0).toLocalDate());

        application.assignZodiacDesignSet(2);

        assertThat(application.getZodiacDesignSet()).isEqualTo(2);
    }

    @Test
    void requireNamingEditableSucceedsInNameEditing() {
        Application application = individual(IssueType.MOBILE);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();

        application.requireNamingEditable();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.NAME_EDITING);
    }

    @Test
    void requireNamingEditableRejectsEveryOtherStatus() {
        assertThatThrownBy(() -> individual(IssueType.MOBILE).requireNamingEditable())
                .isInstanceOf(CustomException.class); // SUBMITTED

        Application reviewing = individual(IssueType.MOBILE);
        reviewing.confirmPayment();
        reviewing.startReview();
        assertThatThrownBy(reviewing::requireNamingEditable).isInstanceOf(CustomException.class);

        Application photoRejected = individual(IssueType.MOBILE);
        photoRejected.confirmPayment();
        photoRejected.startReview();
        photoRejected.rejectPhoto("사진이 흐립니다.");
        assertThatThrownBy(photoRejected::requireNamingEditable).isInstanceOf(CustomException.class);

        Application productionReady = individual(IssueType.MOBILE);
        productionReady.confirmPayment();
        productionReady.startReview();
        productionReady.approveToNaming();
        productionReady.completeNaming();
        assertThatThrownBy(productionReady::requireNamingEditable).isInstanceOf(CustomException.class);

        Application producing = individual(IssueType.MOBILE);
        producing.confirmPayment();
        producing.startReview();
        producing.approveToNaming();
        producing.completeNaming();
        producing.startProducing();
        assertThatThrownBy(producing::requireNamingEditable).isInstanceOf(CustomException.class);

        Application completed = individual(IssueType.MOBILE);
        completed.confirmPayment();
        completed.startReview();
        completed.approveToNaming();
        completed.completeNaming();
        completed.startProducing();
        completed.markCardReady(NOW);
        assertThatThrownBy(completed::requireNamingEditable).isInstanceOf(CustomException.class);

        Application cancelled = individual(IssueType.MOBILE);
        cancelled.cancelByUser(NOW);
        assertThatThrownBy(cancelled::requireNamingEditable).isInstanceOf(CustomException.class);
    }

    private Application individual(IssueType issueType) {
        return Application.createIndividual(
                1L, "APP-2026-000001", 10L, issueType, issueType == IssueType.MOBILE, null, null);
    }
}
