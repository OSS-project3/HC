package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

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

        boolean isNewUser = !userRepository.existsByOauthIdAndOauthProvider(oauthId, provider);
        User user = userRepository.findByOauthIdAndOauthProvider(oauthId, provider)
                .orElseGet(() -> userRepository.save(
                        User.createNewUser(email, oauthId, provider, name)
                ));

        AuthTokens tokens = userService.issueLoginTokens(user);
        authCookieManager.addAccessTokenCookie(response, tokens.accessToken());
        authCookieManager.addRefreshTokenCookie(response, tokens.refreshToken());

        String redirectPath = isNewUser ? "/terms" : "/";
        response.sendRedirect(frontendUrl + redirectPath);
    }
}
