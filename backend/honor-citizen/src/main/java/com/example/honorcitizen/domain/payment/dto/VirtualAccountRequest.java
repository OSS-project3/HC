package com.example.honorcitizen.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class VirtualAccountRequest {

    @NotNull
    private Long applicationId;

    @NotBlank
    private String depositorName;
}
