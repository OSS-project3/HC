package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IdRecoveryConfirmRequest {

    @NotBlank
    private String requestId;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
