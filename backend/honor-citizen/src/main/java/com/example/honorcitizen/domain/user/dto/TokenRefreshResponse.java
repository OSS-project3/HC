package com.example.honorcitizen.domain.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenRefreshResponse {

    private final String accessToken;
}
