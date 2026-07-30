package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.shipping.dto.ShippingAddressRegisterResponse;
import com.example.honorcitizen.domain.shipping.dto.ShippingAddressRequest;
import com.example.honorcitizen.domain.shipping.dto.ShippingAddressUpdateResponse;
import com.example.honorcitizen.domain.shipping.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShippingAddressRegisterResponse>> register(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId,
            @Valid @RequestBody ShippingAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.register(applicationId, userId, request)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ShippingAddressUpdateResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId,
            @Valid @RequestBody ShippingAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.update(applicationId, userId, request)));
    }
}
