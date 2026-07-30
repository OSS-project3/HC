package com.example.honorcitizen.api;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.user.dto.TermsAgreeRequest;
import com.example.honorcitizen.domain.user.dto.TermsAgreeResponse;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.security.AuthCookieManager;
import com.example.honorcitizen.infra.security.AuthTokens;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/terms")
    public ResponseEntity<ApiResponse<TermsAgreeResponse>> agreeTerms(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TermsAgreeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.agreeTerms(userId, request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        AuthTokens tokens = userService.refreshToken(refreshToken);
        authCookieManager.addAccessTokenCookie(response, tokens.accessToken());
        authCookieManager.addRefreshTokenCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        userService.logout(userId, resolveBearerToken(request));
        authCookieManager.expireLegacyAccessTokenCookie(response);
        authCookieManager.expireRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private String resolveBearerToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            String cookieToken = Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            if (StringUtils.hasText(cookieToken)) {
                return cookieToken;
            }
        }

        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
