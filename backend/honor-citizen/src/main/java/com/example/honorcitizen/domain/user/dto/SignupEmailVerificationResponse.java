package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;

@Getter
public class SignupEmailVerificationResponse {

    private final long expiresInSeconds;
    private final long resendAfterSeconds;

    private SignupEmailVerificationResponse(long expiresInSeconds, long resendAfterSeconds) {
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public static SignupEmailVerificationResponse of(long expiresInSeconds, long resendAfterSeconds) {
        return new SignupEmailVerificationResponse(expiresInSeconds, resendAfterSeconds);
    }
}
