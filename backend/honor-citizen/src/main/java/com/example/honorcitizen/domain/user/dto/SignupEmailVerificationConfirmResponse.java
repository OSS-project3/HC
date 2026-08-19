package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;

@Getter
public class SignupEmailVerificationConfirmResponse {

    private final String signupToken;
    private final long expiresInSeconds;

    private SignupEmailVerificationConfirmResponse(String signupToken, long expiresInSeconds) {
        this.signupToken = signupToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    public static SignupEmailVerificationConfirmResponse of(String signupToken, long expiresInSeconds) {
        return new SignupEmailVerificationConfirmResponse(signupToken, expiresInSeconds);
    }
}
