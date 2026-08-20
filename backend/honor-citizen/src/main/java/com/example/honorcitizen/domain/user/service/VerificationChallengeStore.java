package com.example.honorcitizen.domain.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 가입 인증·아이디 찾기·비밀번호 재설정이 공통으로 쓰는 HMAC 인증 코드 challenge의 발급·롤백·원자
 * 검증, 그리고 계정 존재 여부와 무관하게 먼저 통과해야 하는 원자적 쿨다운·시간당 횟수 제한 게이트를
 * 담당한다(RECOVERY-1/2, arch.md §4.1 "계정 복구 책임 구조"). 세 흐름 모두 이 클래스만 거치고
 * Redis 명령을 직접 조립하지 않는다.
 */
@Component
public class VerificationChallengeStore {

    private static final int CHALLENGE_ID_BYTES = 32;

    private static final RedisScript<Long> COMPARE_AND_DELETE_SCRIPT =
            loadScript(Long.class, "/redis/compare-and-delete-challenge.lua");
    private static final RedisScript<String> VERIFY_AND_CONSUME_SCRIPT =
            loadScript(String.class, "/redis/verify-and-increment-code.lua");
    private static final RedisScript<Long> CLAIM_RATE_LIMIT_SCRIPT =
            loadScript(Long.class, "/redis/claim-recovery-rate-limit.lua");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.email-code-secret}")
    private String emailCodeSecret;

    public VerificationChallengeStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public record IssuedChallenge(String challengeId, String rawCode) {
    }

    /** 내부 전용 challengeId를 새로 발급해 challenge를 저장한다(가입 인증처럼 공개 식별자가 없는 흐름용). */
    public IssuedChallenge issue(String key, Duration ttl, Long userId) {
        return issue(key, ttl, userId, generateUrlSafeToken());
    }

    /**
     * 호출자가 지정한 challengeId로 challenge를 저장한다 — 계정 복구는 클라이언트에 돌려주는 공개
     * requestId를 그대로 challengeId로 써서, Redis key 자체가 요청마다 달라지므로(각 requestId가
     * 곧 key의 일부) 별도 값을 만들 필요가 없다.
     */
    public IssuedChallenge issue(String key, Duration ttl, Long userId, String challengeId) {
        String code = generateCode();
        String json = objectMapper.writeValueAsString(new VerificationChallenge(challengeId, hmac(code), 0, userId));
        redisTemplate.opsForValue().set(key, json, ttl);
        return new IssuedChallenge(challengeId, code);
    }

    /** 메일 발송 실패 시 "이 요청이 방금 저장한 challenge와 지금 저장된 값이 같을 때만" 지운다. */
    public void rollback(String key, String challengeId) {
        redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(key), challengeId);
    }

    /** 성공 시 challenge에 결속된 값(userId 포함)을 반환, 실패(불일치/만료/재사용/5회초과/미존재)면 empty. */
    public Optional<VerificationChallenge> verifyAndConsume(String key, String rawCode) {
        String raw = redisTemplate.execute(VERIFY_AND_CONSUME_SCRIPT, List.of(key), hmac(rawCode));
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(raw, VerificationChallenge.class));
    }

    /**
     * 계정 존재 여부와 무관하게 매 요청이 먼저 통과해야 하는 원자적 쿨다운 선점 + 시간당 횟수 제한.
     * true면 쿨다운이 선점되고 두 카운터가 증가된 상태(=요청 진행 가능), false면 아무 것도 바뀌지
     * 않은 상태(=쿨다운 중이거나 한도 초과, TOO_MANY_REQUESTS로 응답).
     */
    public boolean claimRateLimit(String cooldownKey, Duration cooldownTtl,
            String targetCountKey, int targetLimit, Duration targetWindow,
            String ipCountKey, int ipLimit, Duration ipWindow) {
        Long result = redisTemplate.execute(CLAIM_RATE_LIMIT_SCRIPT,
                List.of(cooldownKey, targetCountKey, ipCountKey),
                String.valueOf(cooldownTtl.toSeconds()),
                String.valueOf(targetLimit), String.valueOf(targetWindow.toSeconds()),
                String.valueOf(ipLimit), String.valueOf(ipWindow.toSeconds()));
        return result != null && result == 1L;
    }

    public String hmac(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(emailCodeSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("인증 코드 HMAC 계산에 실패했습니다.", e);
        }
    }

    public String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    /** 32-byte URL-safe 난수 — 계정 복구 requestId, 가입 인증 내부 challengeId에 공용으로 쓴다. */
    public String generateUrlSafeToken() {
        byte[] bytes = new byte[CHALLENGE_ID_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String sha256Hex(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("해시 계산에 실패했습니다.", e);
        }
    }

    private static <T> RedisScript<T> loadScript(Class<T> resultType, String classpathLocation) {
        try (InputStream is = VerificationChallengeStore.class.getResourceAsStream(classpathLocation)) {
            if (is == null) {
                throw new IllegalStateException(classpathLocation + "를 찾을 수 없습니다.");
            }
            return new DefaultRedisScript<>(new String(is.readAllBytes(), StandardCharsets.UTF_8), resultType);
        } catch (IOException e) {
            throw new IllegalStateException("Redis Lua 스크립트 로드에 실패했습니다: " + classpathLocation, e);
        }
    }
}
