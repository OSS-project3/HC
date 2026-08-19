package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;

@Getter
public class EmailCheckResponse {

    private final boolean exists;

    private EmailCheckResponse(boolean exists) {
        this.exists = exists;
    }

    public static EmailCheckResponse of(boolean exists) {
        return new EmailCheckResponse(exists);
    }
}
