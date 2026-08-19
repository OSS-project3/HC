package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 로컬 Redis(REDIS_PORT로 지정한 인스턴스)로 검증한다 — 다른 EmailVerificationService 테스트들과
// 동일한 이유(카운터·TTL·잠금이 실제 Redis 명령으로 정확히 동작하는지)로 Mock하지 않는다.
@SpringBootTest
class LoginAttemptLimiterTest {

    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EMAIL = "rate-limit-test@example.com";

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        redisTemplate.delete(List.of(
                "auth:login:fail:" + sha256Hex(EMAIL),
                "auth:login:lock:" + sha256Hex(EMAIL)));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void checkNotLockedPassesWhenNoFailuresRecorded() {
        assertThatCode(() -> loginAttemptLimiter.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void checkNotLockedPassesUnderFourFailures() {
        for (int i = 0; i < 4; i++) {
            loginAttemptLimiter.recordFailure(EMAIL);
        }

        assertThatCode(() -> loginAttemptLimiter.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void checkNotLockedThrowsAfterFifthFailure() {
        for (int i = 0; i < 5; i++) {
            loginAttemptLimiter.recordFailure(EMAIL);
        }

        assertThatThrownBy(() -> loginAttemptLimiter.checkNotLocked(EMAIL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void resetClearsCounterAndLock() {
        for (int i = 0; i < 5; i++) {
            loginAttemptLimiter.recordFailure(EMAIL);
        }
        assertThatThrownBy(() -> loginAttemptLimiter.checkNotLocked(EMAIL)).isInstanceOf(CustomException.class);

        loginAttemptLimiter.reset(EMAIL);

        assertThatCode(() -> loginAttemptLimiter.checkNotLocked(EMAIL)).doesNotThrowAnyException();
        // 카운터도 함께 리셋됐으므로, 리셋 후 한 번 더 실패해도 곧바로 잠기지 않아야 한다(5회 누적 재시작).
        loginAttemptLimiter.recordFailure(EMAIL);
        assertThatCode(() -> loginAttemptLimiter.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void redisKeysDoNotContainRawEmail() {
        loginAttemptLimiter.recordFailure(EMAIL);

        assertThat(redisTemplate.hasKey("auth:login:fail:" + EMAIL)).isFalse();
        assertThat(redisTemplate.hasKey("auth:login:fail:" + sha256Hex(EMAIL))).isTrue();
    }
}
