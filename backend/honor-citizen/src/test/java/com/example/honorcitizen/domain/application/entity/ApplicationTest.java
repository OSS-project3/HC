package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    @Test
    void createIndividualFixesQuantityToOneAndLeavesCardDesignNull() {
        Application application = Application.createIndividual(
                1L, "APP-2026-000001", 10L, IssueType.MOBILE, true, null, null);

        assertThat(application.getApplicationType()).isEqualTo(ApplicationType.INDIVIDUAL);
        assertThat(application.getTotalQuantity()).isEqualTo(1);
        assertThat(application.getCardDesignId()).isNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PAYMENT_PENDING);
        assertThat(application.getPaymentStatus()).isEqualTo(PaymentStatus.WAITING);
    }

    @Test
    void createGroupUsesRequestedQuantityAndLeavesCardDesignNull() {
        Application application = Application.createGroup(
                1L, "APP-2026-000002", 10L, IssueType.MOBILE_AND_PHYSICAL, false, 42, 100L, 101L, 102L);

        assertThat(application.getApplicationType()).isEqualTo(ApplicationType.GROUP);
        assertThat(application.getTotalQuantity()).isEqualTo(42);
        assertThat(application.getCardDesignId()).isNull();
        assertThat(application.getLogoFileId()).isEqualTo(100L);
        assertThat(application.getSealFileId()).isEqualTo(101L);
        assertThat(application.getSubmitFileId()).isEqualTo(102L);
    }

    @Test
    void isOwnedByComparesUserId() {
        Application application = Application.createIndividual(
                5L, "APP-2026-000003", 10L, IssueType.MOBILE, true, null, null);

        assertThat(application.isOwnedBy(5L)).isTrue();
        assertThat(application.isOwnedBy(6L)).isFalse();
    }

    @Test
    void happyPathTransitionsThroughFullLifecycle() {
        Application application = Application.createIndividual(
                1L, "APP-2026-000004", 10L, IssueType.MOBILE, true, null, null);

        application.confirmPayment();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.RECEIVED);
        assertThat(application.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        application.startReview();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REVIEWING);

        application.approveToNaming();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.NAME_EDITING);

        application.startProducing();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PRODUCING);

        application.complete();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
    }

    @Test
    void photoRejectionCyclesBackToReviewingAndClearsReason() {
        Application application = Application.createIndividual(
                1L, "APP-2026-000005", 10L, IssueType.MOBILE, true, null, null);
        application.confirmPayment();
        application.startReview();

        application.rejectPhoto("사진이 흐립니다.");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PHOTO_REJECTED);
        assertThat(application.getPhotoRejectReason()).isEqualTo("사진이 흐립니다.");

        application.resubmitForReview(null);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REVIEWING);
        assertThat(application.getPhotoRejectReason()).isNull();
    }

    @Test
    void cancelIsAllowedFromAnyNonTerminalStatus() {
        Application application = Application.createIndividual(
                1L, "APP-2026-000006", 10L, IssueType.MOBILE, true, null, null);

        application.cancel();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void cancelledApplicationRejectsFurtherTransitions() {
        Application application = Application.createIndividual(
                1L, "APP-2026-000007", 10L, IssueType.MOBILE, true, null, null);
        application.cancel();

        assertThatThrownBy(application::confirmPayment)
                .isInstanceOf(CustomException.class);
    }

    @Test
    void skippingStatusStepsIsRejected() {
        Application application = Application.createIndividual(
                1L, "APP-2026-000008", 10L, IssueType.MOBILE, true, null, null);

        assertThatThrownBy(application::startReview)
                .isInstanceOf(CustomException.class);
    }
}
