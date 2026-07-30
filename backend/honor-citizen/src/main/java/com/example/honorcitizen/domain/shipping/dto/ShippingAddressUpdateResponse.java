package com.example.honorcitizen.domain.shipping.dto;

import com.example.honorcitizen.domain.shipping.entity.ShippingAddress;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ShippingAddressUpdateResponse {

    private final Long shippingAddressId;
    private final LocalDateTime updatedAt;
    private final boolean isLocked;

    private ShippingAddressUpdateResponse(Long shippingAddressId, LocalDateTime updatedAt, boolean isLocked) {
        this.shippingAddressId = shippingAddressId;
        this.updatedAt = updatedAt;
        this.isLocked = isLocked;
    }

    public static ShippingAddressUpdateResponse from(ShippingAddress shippingAddress) {
        return new ShippingAddressUpdateResponse(
                shippingAddress.getId(),
                shippingAddress.getUpdatedAt(),
                shippingAddress.isLocked());
    }
}
