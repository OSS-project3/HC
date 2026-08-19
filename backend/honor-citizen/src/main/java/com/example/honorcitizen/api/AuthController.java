package com.example.honorcitizen.api;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationConfirmRequest;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationConfirmResponse;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationRequest;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationResponse;
import com.example.honorcitizen.domain.user.dto.SignupRequest;
import com.example.honorcitizen.domain.user.dto.TermsAgreeRequest;
import com.example.honorcitizen.domain.user.dto.TermsAgreeResponse;
import com.example.honorcitizen.domain.user.dto.UserMeResponse;
import com.example.honorcitizen.domain.user.service.EmailVerificationService;
import com.example.honorcitizen.domain.user.service.LocalSignupResult;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.security.AuthCookieManager;
import com.example.honorcitizen.infra.security.AuthTokens;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup/email-verification/request")
    public ResponseEntity<ApiResponse<SignupEmailVerificationResponse>> requestSignupEmailVerification(
            @Valid @RequestBody SignupEmailVerificationRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.success(
                emailVerificationService.requestCode(request.getEmail(), clientIp)));
    }

    @PostMapping("/signup/email-verification/confirm")
    public ResponseEntity<ApiResponse<SignupEmailVerificationConfirmResponse>> confirmSignupEmailVerification(
            @Valid @RequestBody SignupEmailVerificationConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                emailVerificationService.confirmCode(request.getEmail(), request.getCode())));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserMeResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response) {
        // 1~2단계: signupToken 검증 + 이메일 일치 확인(불일치/만료는 동일한 오류로 처리됨).
        String normalizedEmail = emailVerificationService.consumeSignupToken(request.getSignupToken(), request.getEmail());

        // 3~5·7단계: 중복 재조회 → 비밀번호 해시 → User 생성/저장(DB commit) → 로그인 토큰 발급.
        // registerLocalUser는 UserService의 트랜잭션 프록시를 거치므로, 이 호출이 반환된 시점엔
        // DB commit이 이미 끝나 있다 — 그 다음에만 Redis 가입 토큰을 지운다(6단계, 순서 중요).
        LocalSignupResult result = userService.registerLocalUser(normalizedEmail, request.getPassword(), request.getName());
        emailVerificationService.deleteSignupToken(request.getSignupToken());

        authCookieManager.addAccessTokenCookie(response, result.tokens().accessToken());
        authCookieManager.addRefreshTokenCookie(response, result.tokens().refreshToken());

        // 8단계: 약관 동의는 기존 POST /api/auth/terms 흐름 그대로(이 API에 포함하지 않음).
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(UserMeResponse.from(result.user())));
    }

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
