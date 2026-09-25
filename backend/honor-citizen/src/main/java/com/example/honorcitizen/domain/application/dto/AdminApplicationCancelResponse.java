package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CancellationReason;
import com.example.honorcitizen.common.enums.CancellationType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

// 관리자 강제 취소(2026-09-25 확정) 응답 — ApplicationCancelResponse(사용자용)와 달리 취소 유형·사유·
// 메모와 "이번 호출이 최초 처리였는지"를 함께 내려준다. refundRequired는 사용자용과 동일한 의미
// (PaymentStatus.CONFIRMED면 관리자가 시스템 밖에서 환불해야 한다는 안내값, 환불 완료 여부 아님).
@Getter
public class AdminApplicationCancelResponse {

    private final Long applicationId;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final boolean refundRequired;
    private final LocalDateTime cancelledAt;
    private final CancellationType cancellationType;
    private final CancellationReason cancellationReason;
    private final String cancellationMemo;
    private final boolean firstCancellation;

    private AdminApplicationCancelResponse(Long applicationId, ApplicationStatus status, PaymentStatus paymentStatus,
            boolean refundRequired, LocalDateTime cancelledAt, CancellationType cancellationType,
            CancellationReason cancellationReason, String cancellationMemo, boolean firstCancellation) {
        this.applicationId = applicationId;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.refundRequired = refundRequired;
        this.cancelledAt = cancelledAt;
        this.cancellationType = cancellationType;
        this.cancellationReason = cancellationReason;
        this.cancellationMemo = cancellationMemo;
        this.firstCancellation = firstCancellation;
    }

    public static AdminApplicationCancelResponse from(Application application, boolean firstCancellation) {
        boolean refundRequired = application.getPaymentStatus() == PaymentStatus.CONFIRMED;
        return new AdminApplicationCancelResponse(
                application.getId(), application.getStatus(), application.getPaymentStatus(), refundRequired,
                application.getCancelledAt(), application.getCancellationType(), application.getCancellationReason(),
                application.getCancellationMemo(), firstCancellation);
    }
}
