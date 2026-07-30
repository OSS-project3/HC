package com.example.honorcitizen.domain.payment.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentMethod;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.payment.dto.PaymentInfoResponse;
import com.example.honorcitizen.domain.payment.dto.TossWebhookRequest;
import com.example.honorcitizen.domain.payment.dto.VirtualAccountRequest;
import com.example.honorcitizen.domain.payment.dto.VirtualAccountResponse;
import com.example.honorcitizen.domain.payment.entity.Payment;
import com.example.honorcitizen.domain.payment.entity.PaymentLog;
import com.example.honorcitizen.domain.payment.repository.PaymentLogRepository;
import com.example.honorcitizen.domain.payment.repository.PaymentRepository;
import com.example.honorcitizen.domain.log.entity.ApplicationStatusLog;
import com.example.honorcitizen.domain.log.repository.ApplicationStatusLogRepository;
import com.example.honorcitizen.infra.toss.TossPaymentsClient;
import com.example.honorcitizen.infra.toss.TossPaymentsProperties;
import com.example.honorcitizen.infra.toss.TossVirtualAccountRequest;
import com.example.honorcitizen.infra.toss.TossVirtualAccountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class PaymentService {

    private static final String ORDER_NAME = "명예 시민증 발급";
    private static final int VIRTUAL_ACCOUNT_VALID_HOURS = 24;

    @Value("${app.payment.service-amount:30000}")
    private long serviceAmount;

    @Value("${app.payment.shipping-fee:25000}")
    private long shippingFee;

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final ApplicationRepository applicationRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final TossPaymentsProperties tossProps;
    private final ApplicationStatusLogRepository statusLogRepository;

    @Transactional(readOnly = true)
    public PaymentInfoResponse getPaymentInfo(Long userId, Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!app.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return paymentRepository.findByApplicationId(applicationId)
                .map(p -> PaymentInfoResponse.of(p.getServiceAmount(), p.getShippingFee(),
                        p.getTotalAmount(), p.getCurrency(), p.getPaymentStatus()))
                .orElseGet(() -> PaymentInfoResponse.of(serviceAmount, shippingFee,
                        serviceAmount + shippingFee, "KRW", PaymentStatus.PENDING));
    }

    @Transactional
    public VirtualAccountResponse issueVirtualAccount(Long userId, VirtualAccountRequest request) {
        Application app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!app.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Optional<Payment> existing = paymentRepository.findByApplicationId(request.getApplicationId());
        if (existing.isPresent() && existing.get().getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ALREADY_PAID);
        }

        long total = serviceAmount + shippingFee;
        String orderId = generateOrderId();

        TossVirtualAccountResult result = tossPaymentsClient.issueVirtualAccount(
                new TossVirtualAccountRequest(total, orderId, ORDER_NAME,
                        request.getDepositorName(), tossProps.defaultBank(), VIRTUAL_ACCOUNT_VALID_HOURS));

        Payment payment;
        if (existing.isPresent()) {
            payment = existing.get();
            payment.reissue(orderId, result.paymentKey(), request.getDepositorName(), result.dueDate());
        } else {
            payment = paymentRepository.save(Payment.create(
                    request.getApplicationId(), orderId, PaymentMethod.VIRTUAL_ACCOUNT,
                    result.paymentKey(), request.getDepositorName(),
                    serviceAmount, shippingFee, result.dueDate()));
        }

        paymentLogRepository.save(PaymentLog.create(payment.getId(), "VIRTUAL_ACCOUNT_ISSUED",
                "VIRTUAL_ACCOUNT", total, "WAITING_DEPOSIT", null, null));

        return VirtualAccountResponse.of(result.bankName(), result.accountNumber(),
                total, request.getDepositorName(), result.dueDate());
    }

    @Transactional
    public void handleWebhook(TossWebhookRequest webhookRequest, String signature) {
        if (signature != null && !tossProps.webhookSecret().isBlank()) {
            // TODO: implement raw-body HMAC verification via ContentCachingRequestWrapper filter
            log.debug("웹훅 서명 헤더 수신: {}", signature);
        }

        if (webhookRequest == null || webhookRequest.getData() == null) return;

        String eventType = webhookRequest.getEventType();
        TossWebhookRequest.TossPaymentData data = webhookRequest.getData();

        if ("VIRTUAL_ACCOUNT_COMPLETED".equals(eventType)) {
            processVirtualAccountCompleted(data);
        } else if ("VIRTUAL_ACCOUNT_EXPIRED".equals(eventType)) {
            processVirtualAccountExpired(data);
        }
    }

    private void processVirtualAccountCompleted(TossWebhookRequest.TossPaymentData data) {
        Payment payment = paymentRepository.findByOrderId(data.getOrderId()).orElse(null);
        if (payment == null) {
            log.warn("웹훅 수신 — 알 수 없는 orderId: {}", data.getOrderId());
            return;
        }

        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.info("중복 웹훅 무시: orderId={}", data.getOrderId());
            return;
        }

        if (data.getTotalAmount() != null && payment.getTotalAmount() != data.getTotalAmount()) {
            log.warn("결제 금액 불일치: orderId={}, expected={}, actual={}",
                    data.getOrderId(), payment.getTotalAmount(), data.getTotalAmount());
            payment.fail();
            paymentLogRepository.save(PaymentLog.create(payment.getId(),
                    "PAYMENT_FAILED", "VIRTUAL_ACCOUNT", data.getTotalAmount(),
                    "FAILED", "금액 불일치", null));
            return;
        }

        payment.complete(null);
        paymentLogRepository.save(PaymentLog.create(payment.getId(),
                "VIRTUAL_ACCOUNT_COMPLETED", "VIRTUAL_ACCOUNT",
                payment.getTotalAmount(), "COMPLETED", null, null));

        applicationRepository.findById(payment.getApplicationId()).ifPresent(app -> {
            if (app.getStatus() == ApplicationStatus.DRAFT) {
                app.approvePayment();
                statusLogRepository.save(ApplicationStatusLog.create(
                        app.getId(), ApplicationStatus.DRAFT, ApplicationStatus.PENDING,
                        null, "결제 완료"));
            }
        });
    }

    private void processVirtualAccountExpired(TossWebhookRequest.TossPaymentData data) {
        paymentRepository.findByOrderId(data.getOrderId()).ifPresent(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.WAITING_DEPOSIT) {
                payment.fail();
                paymentLogRepository.save(PaymentLog.create(payment.getId(),
                        "PAYMENT_FAILED", "VIRTUAL_ACCOUNT", payment.getTotalAmount(),
                        "FAILED", "가상계좌 입금기한 만료", null));
            }
        });
    }

    private String generateOrderId() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("order_%s_%04d", ts, new Random().nextInt(10000));
    }
}
