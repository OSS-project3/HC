package com.example.honorcitizen.domain.shipping.dto;

import com.example.honorcitizen.domain.shipping.entity.ShippingAddress;
import lombok.Getter;

@Getter
public class ShippingAddressRegisterResponse {

    private final Long shippingAddressId;
    private final String estimatedDeliveryDays;
    private final int shippingFee;
    private final boolean isLocked;

    private ShippingAddressRegisterResponse(
            Long shippingAddressId,
            String estimatedDeliveryDays,
            int shippingFee,
            boolean isLocked) {
        this.shippingAddressId = shippingAddressId;
        this.estimatedDeliveryDays = estimatedDeliveryDays;
        this.shippingFee = shippingFee;
        this.isLocked = isLocked;
    }

    public static ShippingAddressRegisterResponse of(
            ShippingAddress shippingAddress,
            String estimatedDeliveryDays,
            int shippingFee) {
        return new ShippingAddressRegisterResponse(
                shippingAddress.getId(),
                estimatedDeliveryDays,
                shippingFee,
                shippingAddress.isLocked());
    }
}
