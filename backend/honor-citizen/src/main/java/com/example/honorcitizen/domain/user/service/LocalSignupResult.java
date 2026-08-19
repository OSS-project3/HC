package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.infra.security.AuthTokens;

public record LocalSignupResult(User user, AuthTokens tokens) {
}
