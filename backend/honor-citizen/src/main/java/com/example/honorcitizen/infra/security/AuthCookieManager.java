package com.example.honorcitizen.infra.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieManager {

    private final JwtTokenProvider jwtTokenProvider;
    private final boolean secure;
    private final String sameSite;

    public AuthCookieManager(
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie("refreshToken", refreshToken)
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRemainingMillis(refreshToken)))
                .build()
                .toString());
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie("accessToken", accessToken)
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRemainingMillis(accessToken)))
                .build()
                .toString());
    }

    public void expireRefreshTokenCookie(HttpServletResponse response) {
        expireCookie(response, "refreshToken");
    }

    public void expireLegacyAccessTokenCookie(HttpServletResponse response) {
        expireCookie(response, "accessToken");
    }

    private void expireCookie(HttpServletResponse response, String name) {
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie(name, "")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite);
    }
}
