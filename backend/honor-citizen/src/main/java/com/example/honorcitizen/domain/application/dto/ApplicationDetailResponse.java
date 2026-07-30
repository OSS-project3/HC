package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ApplicationDetailResponse {

    private final Long applicationId;
    private final String status;
    private final String nameEn;
    private final String nationality;
    private final KoreanNameDetail koreanName;
    private final CitizenCardDetail citizenCard;
    private final ShippingDetail shipping;
    private final PaymentDetail payment;

    private ApplicationDetailResponse(
            Long applicationId,
            String status,
            String nameEn,
            String nationality,
            KoreanNameDetail koreanName,
            CitizenCardDetail citizenCard,
            ShippingDetail shipping,
            PaymentDetail payment) {
        this.applicationId = applicationId;
        this.status = status;
        this.nameEn = nameEn;
        this.nationality = nationality;
        this.koreanName = koreanName;
        this.citizenCard = citizenCard;
        this.shipping = shipping;
        this.payment = payment;
    }

    public static ApplicationDetailResponse from(Application application) {
        return new ApplicationDetailResponse(
                application.getId(),
                application.getStatus().name(),
                application.getNameEn(),
                application.getNationality(),
                null,
                null,
                null,
                null);
    }

    @Getter
    public static class KoreanNameDetail {
        private final String fullNameKo;
        private final String fullNameEn;
        private final String meaning;
        private final String nameOrigin;

        public KoreanNameDetail(String fullNameKo, String fullNameEn, String meaning, String nameOrigin) {
            this.fullNameKo = fullNameKo;
            this.fullNameEn = fullNameEn;
            this.meaning = meaning;
            this.nameOrigin = nameOrigin;
        }
    }

    @Getter
    public static class CitizenCardDetail {
        private final String cardNumber;
        private final String downloadUrl;
        private final LocalDateTime issuedAt;

        public CitizenCardDetail(String cardNumber, String downloadUrl, LocalDateTime issuedAt) {
            this.cardNumber = cardNumber;
            this.downloadUrl = downloadUrl;
            this.issuedAt = issuedAt;
        }
    }

    @Getter
    public static class ShippingDetail {
        private final String trackingNumber;
        private final String carrier;
        private final String orderStatus;
        private final LocalDate estimatedDelivery;
        private final String trackingUrl;

        public ShippingDetail(
                String trackingNumber,
                String carrier,
                String orderStatus,
                LocalDate estimatedDelivery,
                String trackingUrl) {
            this.trackingNumber = trackingNumber;
            this.carrier = carrier;
            this.orderStatus = orderStatus;
            this.estimatedDelivery = estimatedDelivery;
            this.trackingUrl = trackingUrl;
        }
    }

    @Getter
    public static class PaymentDetail {
        private final String orderId;
        private final Long paidAmount;
        private final LocalDateTime paidAt;

        public PaymentDetail(String orderId, Long paidAmount, LocalDateTime paidAt) {
            this.orderId = orderId;
            this.paidAmount = paidAmount;
            this.paidAt = paidAt;
        }
    }
}
