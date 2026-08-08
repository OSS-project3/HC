package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.enums.UserStatus;
import com.example.honorcitizen.domain.user.dto.TermsAgreeRequest;
import com.example.honorcitizen.domain.user.dto.TermsAgreeResponse;
import com.example.honorcitizen.domain.user.dto.UserMeResponse;
import com.example.honorcitizen.domain.user.dto.UserUpdateRequest;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.AuthTokens;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.security.TokenSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final int WITHDRAWAL_GRACE_PERIOD_DAYS = 7;

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

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        return UserMeResponse.from(findById(userId));
    }

    public UserMeResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = findById(userId);

        if (request.getName() == null && request.getPhone() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.getName() != null && request.getName().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        user.updateProfile(request.getName(), request.getPhone());
        return UserMeResponse.from(user);
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

    public void withdraw(Long userId, String accessToken) {
        User user = findById(userId);
        if (user.isWithdrawn()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }

        user.withdraw();
        user.updateRefreshToken(null);
        tokenSessionStore.invalidateUserSessions(userId);
        tokenSessionStore.blacklistAccessToken(accessToken);
        log.info("보안 이벤트: 회원탈퇴(소프트) userId={}", userId);
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User findEligibleApplicationUser(Long userId) {
        User user = findById(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }
        if (user.getRole() != UserRole.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!user.isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public void validateTermsAgreed(Long userId) {
        if (!findById(userId).isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }
    }

    public int anonymizeExpiredWithdrawnUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(WITHDRAWAL_GRACE_PERIOD_DAYS);
        List<User> targets = userRepository.findByStatusAndAnonymizedAtIsNullAndWithdrawalRequestedAtBefore(
                UserStatus.WITHDRAWN, threshold);

        targets.forEach(User::anonymize);

        if (!targets.isEmpty()) {
            log.info("보안 이벤트: 완전탈퇴(익명화) 처리 {}건", targets.size());
        }
        return targets.size();
    }
}
