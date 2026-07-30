package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.dto.TermsAgreeRequest;
import com.example.honorcitizen.domain.user.dto.TermsAgreeResponse;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.AuthTokens;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.security.TokenSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenSessionStore tokenSessionStore;

    public TermsAgreeResponse agreeTerms(Long userId, TermsAgreeRequest request) {
        User user = findById(userId);

        if (user.isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_ALREADY_AGREED);
        }

        user.agreeTerms(request.getPrivacyAgreed(), request.getImageUploadAgreed(), request.getShippingAgreed());

        return TermsAgreeResponse.from(user);
    }

    public AuthTokens issueLoginTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenSessionStore.createRefreshToken(user);
        user.updateRefreshToken(refreshToken);
        return new AuthTokens(accessToken, refreshToken);
    }

    public AuthTokens refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = findById(jwtTokenProvider.getUserId(refreshToken));
        String newRefreshToken = tokenSessionStore.rotateRefreshToken(user, refreshToken);
        user.updateRefreshToken(newRefreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        return new AuthTokens(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId, String accessToken) {
        tokenSessionStore.invalidateUserSessions(userId);
        tokenSessionStore.blacklistAccessToken(accessToken);
        findById(userId).updateRefreshToken(null);
        log.info("보안 이벤트: 로그아웃 userId={}", userId);
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public void validateTermsAgreed(Long userId) {
        if (!findById(userId).isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }
    }
}
