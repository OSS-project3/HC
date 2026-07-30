package com.example.honorcitizen.domain.payment.dto;

import lombok.Getter;

@Getter
public class TossWebhookRequest {

    private String eventType;
    private TossPaymentData data;

    @Getter
    public static class TossPaymentData {
        private String paymentKey;
        private String orderId;
        private String status;
        private Long totalAmount;
        private TossVirtualAccount virtualAccount;
    }

    @Getter
    public static class TossVirtualAccount {
        private String accountNumber;
        private String bankCode;
        private String depositorName;
        private String dueDate;
    }
}
