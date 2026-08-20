package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;

@Getter
public class IdRecoveryResponse {

    private final String requestId;
    private final long expiresInSeconds;
    private final long resendAfterSeconds;

    private IdRecoveryResponse(String requestId, long expiresInSeconds, long resendAfterSeconds) {
        this.requestId = requestId;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public static IdRecoveryResponse of(String requestId, long expiresInSeconds, long resendAfterSeconds) {
        return new IdRecoveryResponse(requestId, expiresInSeconds, resendAfterSeconds);
    }
}
