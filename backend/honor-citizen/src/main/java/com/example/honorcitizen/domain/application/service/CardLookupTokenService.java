package com.example.honorcitizen.domain.application.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 비로그인 공개 카드 조회(lookup) 성공 시 그 조회 건에 한해 카드 다운로드를 허용하는 단기 토큰.
 *
 * 다운로드 API(getCardDownload)는 로그인 세션이 있는 마이페이지 전용이라 userId 소유권 검증으로
 * IDOR을 막지만, 공개 조회 화면은 로그인 자체가 없다. applicationId(순차 정수)를 그대로 다운로드
 * API에 permitAll로 노출하면 값만 바꿔 타인의 카드(얼굴사진 포함)를 내려받을 수 있다 — 그래서
 * "조회를 실제로 거쳤다"는 사실 자체를 인가 근거로 쓰는 1회용 토큰을 발급한다.
 *
 * 원문 토큰은 저장하지 않고 SHA-256 해시만 Redis 키로 사용한다(VerificationChallengeStore의
 * signupToken과 동일한 패턴 — 새 서명 비밀키를 추가하지 않기 위해 같은 방식을 따르되, 용도가
 * 회원가입과 무관해 별도 컴포넌트로 분리했다).
 */
@Component
class CardLookupTokenService {

    private static final String KEY_PREFIX = "auth:lookup:card-token:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    CardLookupTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    String issue(Long applicationId) {
        String token = generateUrlSafeToken();
        redisTemplate.opsForValue().set(KEY_PREFIX + sha256Hex(token), applicationId.toString(), TTL);
        return token;
    }

    /** 성공 시 토큰을 즉시 삭제해 1회용으로 만든다. 실패(불일치/만료/미존재)면 false. */
    boolean verifyAndConsume(Long applicationId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = KEY_PREFIX + sha256Hex(token);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(applicationId.toString())) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }

    private String generateUrlSafeToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("해시 계산에 실패했습니다.", e);
        }
    }
}
