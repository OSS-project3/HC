package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;

@Getter
public class IdRecoveryConfirmResponse {

    private final String maskedEmail;

    private IdRecoveryConfirmResponse(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public static IdRecoveryConfirmResponse of(String maskedEmail) {
        return new IdRecoveryConfirmResponse(maskedEmail);
    }
}
