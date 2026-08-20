package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.entity.RefreshTokenSession;
import com.example.honorcitizen.domain.user.entity.RefreshTokenStatus;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.RefreshTokenSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenSessionStore {

    private static final String REFRESH_SESSION_PREFIX = "auth:refresh:session:";
    private static final String USER_SESSIONS_PREFIX = "auth:refresh:user:";
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:access:blacklist:";
    private static final String USER_REVOKED_AFTER_PREFIX = "auth:access:user-revoked-after:";
    // access token 최대 수명(15분) + clock skew 여유(RECOVERY-2 정책, 기본 TTL 16분).
    private static final Duration REVOKED_AFTER_CLOCK_SKEW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiryMillis;

    public String createRefreshToken(User user) {
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getRole(),
                sessionId
        );

        saveRefreshSession(user.getId(), sessionId, jwtTokenProvider.getTokenId(refreshToken), refreshToken);
        log.info("보안 이벤트: 로그인 성공 userId={}", user.getId());
        return refreshToken;
    }

    public String rotateRefreshToken(User user, String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long tokenUserId = jwtTokenProvider.getUserId(refreshToken);
        String sessionId = jwtTokenProvider.getSessionId(refreshToken);
        String presentedJti = jwtTokenProvider.getTokenId(refreshToken);
        if (!user.getId().equals(tokenUserId) || sessionId == null || presentedJti == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String sessionKey = refreshSessionKey(sessionId);
        String currentJti = redisTemplate.opsForValue().get(sessionKey);
        if (currentJti == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!presentedJti.equals(currentJti)) {
            invalidateUserSessions(user.getId());
            revokePresentedRefreshToken(presentedJti);
            log.warn("보안 이벤트: Refresh Token 재사용 감지 userId={} sessionId={}", user.getId(), sessionId);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        refreshTokenSessionRepository.findByTokenId(presentedJti)
                .ifPresent(RefreshTokenSession::rotate);

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getRole(),
                sessionId
        );
        saveRefreshSession(user.getId(), sessionId, jwtTokenProvider.getTokenId(newRefreshToken), newRefreshToken);
        log.info("보안 이벤트: 토큰 재발급 userId={} sessionId={}", user.getId(), sessionId);
        return newRefreshToken;
    }

    public void invalidateUserSessions(Long userId) {
        String userSessionsKey = userSessionsKey(userId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            redisTemplate.delete(sessionIds.stream()
                    .map(this::refreshSessionKey)
                    .toList());
        }
        redisTemplate.delete(userSessionsKey);
        refreshTokenSessionRepository.findByUserIdAndStatus(userId, RefreshTokenStatus.ACTIVE)
                .forEach(RefreshTokenSession::revoke);
    }

    // 회원탈퇴(하드 삭제) 전용 — 로그아웃/비밀번호 변경은 세션을 revoke 상태로만 남겨 감사 추적을
    // 유지하지만(invalidateUserSessions), 탈퇴는 계정 자체가 사라지므로 토큰 문자열까지 완전히
    // 지운다(2026-08-19 정책, `docs/collab/user.md` §3). invalidateUserSessions로 이미 Redis 세션
    // 참조는 정리된 뒤 호출한다는 전제 — 여기서는 DB row 삭제만 수행한다.
    public void deleteUserSessions(Long userId) {
        refreshTokenSessionRepository.deleteByUserId(userId);
    }

    public void blacklistAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }

        if (!jwtTokenProvider.validateToken(accessToken) || !jwtTokenProvider.isAccessToken(accessToken)) {
            return;
        }

        long remainingMillis = jwtTokenProvider.getRemainingMillis(accessToken);
        if (remainingMillis <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                accessBlacklistKey(jwtTokenProvider.getTokenId(accessToken)),
                "1",
                Duration.ofMillis(remainingMillis)
        );
    }

    // 사용자 단위 access token 무효화 기준 시각을 기록한다(비밀번호 변경·재설정 공용 revoke primitive,
    // RECOVERY-2 정책). 표준 JWT `iat`는 초 단위라 재설정과 새 로그인이 같은 초에 겹치면 새 토큰까지
    // 오거절할 수 있어 millisecond 정밀도의 별도 값을 쓴다. TTL은 access token 최대 수명+clock skew —
    // 그 이후엔 어차피 모든 구버전 토큰이 자연 만료되므로 키를 계속 들고 있을 필요가 없다.
    public void recordUserAccessRevocation(Long userId) {
        long revokedAfterMillis = System.currentTimeMillis();
        Duration ttl = Duration.ofMillis(accessTokenExpiryMillis).plus(REVOKED_AFTER_CLOCK_SKEW);
        redisTemplate.opsForValue().set(userRevokedAfterKey(userId), String.valueOf(revokedAfterMillis), ttl);
    }

    /**
     * access token 블랙리스트와 사용자 단위 revoked-after를 하나의 세션 검증 책임으로 통합한다
     * (RECOVERY-2). Redis 조회 자체가 실패하면 미확인 토큰을 통과시키지 않는 fail-closed로
     * {@link ErrorCode#AUTH_SESSION_VALIDATION_UNAVAILABLE}을 던진다 — 예전의 "장애 시 blacklist
     * 아님으로 간주" fail-open은 세션 무효화를 무력화할 수 있어 폐기한다.
     *
     * @param authIssuedAtMillis 토큰의 `iam` 커스텀 클레임(없으면 null — 배포 전 발급된 구버전 토큰)
     */
    public boolean isAccessTokenSessionValid(String accessToken, Long userId, Long authIssuedAtMillis) {
        try {
            String jti = jwtTokenProvider.getTokenId(accessToken);
            if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(accessBlacklistKey(jti)))) {
                return false;
            }

            String revokedAfterRaw = redisTemplate.opsForValue().get(userRevokedAfterKey(userId));
            if (revokedAfterRaw == null) {
                // revoked-after 키가 아예 없는 사용자 — 재설정/비밀번호 변경을 한 번도 겪지 않은
                // 배포 전 토큰까지 포함해 만료 전까지는 허용한다.
                return true;
            }
            if (authIssuedAtMillis == null) {
                // revoked-after 키가 있는데 클레임이 없는 토큰 = 재설정 이전에 발급된 구버전 토큰.
                return false;
            }
            long revokedAfterMillis = Long.parseLong(revokedAfterRaw);
            return authIssuedAtMillis > revokedAfterMillis;
        } catch (DataAccessException e) {
            log.warn("Redis 세션 검증 실패(blacklist/revoked-after 조회): {}", e.getMessage());
            throw new CustomException(ErrorCode.AUTH_SESSION_VALIDATION_UNAVAILABLE);
        }
    }

    private void saveRefreshSession(Long userId, String sessionId, String refreshJti, String refreshToken) {
        Duration ttl = Duration.ofMillis(jwtTokenProvider.getRemainingMillis(refreshToken));
        String userSessionsKey = userSessionsKey(userId);

        redisTemplate.opsForValue().set(refreshSessionKey(sessionId), refreshJti, ttl);
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, ttl);

        refreshTokenSessionRepository.save(RefreshTokenSession.active(
                userId,
                sessionId,
                refreshJti,
                refreshToken,
                jwtTokenProvider.getExpirationDateTime(refreshToken)
        ));
    }

    private void revokePresentedRefreshToken(String tokenId) {
        refreshTokenSessionRepository.findByTokenId(tokenId)
                .ifPresent(RefreshTokenSession::revoke);
    }

    private String refreshSessionKey(String sessionId) {
        return REFRESH_SESSION_PREFIX + sessionId;
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSIONS_PREFIX + userId + ":sessions";
    }

    private String accessBlacklistKey(String tokenId) {
        return ACCESS_BLACKLIST_PREFIX + tokenId;
    }

    private String userRevokedAfterKey(Long userId) {
        return USER_REVOKED_AFTER_PREFIX + userId;
    }
}
