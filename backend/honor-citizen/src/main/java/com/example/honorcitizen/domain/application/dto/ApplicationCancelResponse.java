package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationCancelResponse {

    private final Long applicationId;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final boolean refundRequired;
    private final LocalDateTime cancelledAt;

    private ApplicationCancelResponse(Long applicationId, ApplicationStatus status,
            PaymentStatus paymentStatus, boolean refundRequired, LocalDateTime cancelledAt) {
        this.applicationId = applicationId;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.refundRequired = refundRequired;
        this.cancelledAt = cancelledAt;
    }

    // 환불 진행 상태를 나타내는 값이 아니라 "결제가 확인된 상태로 취소되어 관리자의 외부 수동
    // 환불 절차가 필요한 취소인지"를 알려주는 안내값이다(2026-09-13 정책). 시스템은 환불 완료
    // 여부를 관리하지 않으므로 Application.refundedAt은 참조하지 않는다.
    public static ApplicationCancelResponse from(Application application) {
        boolean refundRequired = application.getPaymentStatus() == PaymentStatus.CONFIRMED;
        return new ApplicationCancelResponse(
                application.getId(), application.getStatus(), application.getPaymentStatus(),
                refundRequired, application.getCancelledAt());
    }
}
