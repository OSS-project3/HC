package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.payment.dto.PaymentInfoResponse;
import com.example.honorcitizen.domain.payment.dto.TossWebhookRequest;
import com.example.honorcitizen.domain.payment.dto.VirtualAccountRequest;
import com.example.honorcitizen.domain.payment.dto.VirtualAccountResponse;
import com.example.honorcitizen.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/applications/{applicationId}/payment-info")
    public ResponseEntity<ApiResponse<PaymentInfoResponse>> getPaymentInfo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentInfo(userId, applicationId)));
    }

    @PostMapping("/payments/virtual-account")
    public ResponseEntity<ApiResponse<VirtualAccountResponse>> issueVirtualAccount(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VirtualAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.issueVirtualAccount(userId, request)));
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody TossWebhookRequest webhookRequest,
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature) {
        paymentService.handleWebhook(webhookRequest, signature);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
