package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthCookieManager authCookieManager;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String oauthId;
        String email;
        String name;

        if ("google".equals(provider)) {
            oauthId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if ("naver".equals(provider)) {
            Map<String, Object> naverResponse = oAuth2User.getAttribute("response");
            oauthId = (String) naverResponse.get("id");
            email = (String) naverResponse.get("email");
            name = (String) naverResponse.get("name");
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        }

        Optional<User> existingOAuthUser = userRepository.findByOauthIdAndOauthProvider(oauthId, provider);
        boolean isNewUser = existingOAuthUser.isEmpty();

        User user;
        if (existingOAuthUser.isPresent()) {
            user = existingOAuthUser.get();
        } else {
            User created;
            try {
                // email UNIQUE 제약 도입 이후: 같은 이메일로 이미 다른 계정(다른 provider 또는 일반 이메일
                // 계정)이 있으면 자동 연결하지 않고 로그인을 거절한다(계정 병합 정책 없음).
                created = userService.createOAuthUserIfAbsent(email, oauthId, provider, name);
            } catch (CustomException e) {
                log.warn("보안 이벤트: OAuth 로그인 거절(이메일 중복) provider={} errorCode={}", provider, e.getErrorCode());
                response.sendRedirect(frontendUrl + "/login?error=oauth");
                return;
            }
            // createOAuthUserIfAbsent는 REQUIRES_NEW로 별도 트랜잭션에서 실행돼 이미 커밋·종료됐으므로
            // 반환된 엔티티는 detached 상태다. 이후 updateRefreshToken() 변경이 이 메서드의
            // (외부) 트랜잭션에 반영되도록 다시 조회해 managed 상태로 만든다.
            user = userRepository.findById(created.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }

        AuthTokens tokens = userService.issueLoginTokens(user);
        authCookieManager.addAccessTokenCookie(response, tokens.accessToken());
        authCookieManager.addRefreshTokenCookie(response, tokens.refreshToken());

        String redirectPath = isNewUser ? "/terms" : "/";
        response.sendRedirect(frontendUrl + redirectPath);
    }
}
