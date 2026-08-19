package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * 로그인 실패 횟수 제한(RATE-1). 정규화 이메일 기준 15분 내 5회 실패 시 15분 잠근다.
 * 존재하지 않는 이메일로 시도해도 동일하게 카운트해 계정 존재 여부가 잠금 발생으로 새어나가지 않는다.
 * Redis 키에는 원문 이메일 대신 SHA-256 해시만 쓴다.
 *
 * 비밀번호 재설정(계정복구) 흐름은 이 클래스를 호출하지 않는다 — 잠금 중에도 재설정은 허용해야 하므로
 * {@link #checkNotLocked}는 로그인 API에서만 호출한다.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String FAIL_KEY_PREFIX = "auth:login:fail:";
    private static final String LOCK_KEY_PREFIX = "auth:login:lock:";

    private final StringRedisTemplate redisTemplate;

    public void checkNotLocked(String normalizedEmail) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(normalizedEmail)))) {
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    public void recordFailure(String normalizedEmail) {
        String failKey = failKey(normalizedEmail);
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, FAILURE_WINDOW);
        }
        if (count != null && count >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(lockKey(normalizedEmail), "1", LOCK_DURATION);
        }
    }

    public void reset(String normalizedEmail) {
        redisTemplate.delete(List.of(failKey(normalizedEmail), lockKey(normalizedEmail)));
    }

    private String failKey(String normalizedEmail) {
        return FAIL_KEY_PREFIX + sha256Hex(normalizedEmail);
    }

    private String lockKey(String normalizedEmail) {
        return LOCK_KEY_PREFIX + sha256Hex(normalizedEmail);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("로그인 시도 제한 키 해시 계산에 실패했습니다.", e);
        }
    }
}
