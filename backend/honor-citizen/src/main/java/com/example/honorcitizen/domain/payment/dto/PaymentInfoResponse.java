package com.example.honorcitizen.domain.payment.dto;

import com.example.honorcitizen.common.enums.PaymentStatus;
import lombok.Getter;

@Getter
public class PaymentInfoResponse {

    private final long serviceAmount;
    private final long shippingFee;
    private final long totalAmount;
    private final String currency;
    private final PaymentStatus paymentStatus;

    private PaymentInfoResponse(long serviceAmount, long shippingFee, long totalAmount,
                                String currency, PaymentStatus paymentStatus) {
        this.serviceAmount = serviceAmount;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.paymentStatus = paymentStatus;
    }

    public static PaymentInfoResponse of(long serviceAmount, long shippingFee, long totalAmount,
                                         String currency, PaymentStatus paymentStatus) {
        return new PaymentInfoResponse(serviceAmount, shippingFee, totalAmount, currency, paymentStatus);
    }
}
