package com.example.honorcitizen.domain.shipping.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShippingAddressRequest {

    @NotBlank
    private String recipientName;

    @NotBlank
    private String zipCode;

    @NotBlank
    private String address;

    private String addressDetail;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;
}
