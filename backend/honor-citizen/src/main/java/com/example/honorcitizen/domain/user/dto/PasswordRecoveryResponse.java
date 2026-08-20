package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;

@Getter
public class PasswordRecoveryResponse {

    private final String requestId;
    private final long expiresInSeconds;
    private final long resendAfterSeconds;

    private PasswordRecoveryResponse(String requestId, long expiresInSeconds, long resendAfterSeconds) {
        this.requestId = requestId;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public static PasswordRecoveryResponse of(String requestId, long expiresInSeconds, long resendAfterSeconds) {
        return new PasswordRecoveryResponse(requestId, expiresInSeconds, resendAfterSeconds);
    }
}
