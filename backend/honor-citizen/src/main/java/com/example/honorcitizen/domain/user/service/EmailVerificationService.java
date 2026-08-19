package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.EmailType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationConfirmResponse;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationResponse;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 이메일 가입 인증 코드의 요청·검증을 전담한다. User row는 코드 검증(AUTH-4 가입 시점)이 끝나기
 * 전까지 절대 생성하지 않는다 — 미인증 계정이 DB에 남지 않도록 상태를 전부 Redis에 둔다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final Duration SIGNUP_TOKEN_TTL = Duration.ofMinutes(30);
    private static final int EMAIL_RATE_LIMIT = 5;
    private static final int IP_RATE_LIMIT = 20;
    private static final int SIGNUP_TOKEN_BYTES = 32;

    private static final String CODE_KEY_PREFIX = "auth:signup:code:";
    private static final String COOLDOWN_KEY_PREFIX = "auth:signup:code:cooldown:";
    private static final String EMAIL_COUNT_KEY_PREFIX = "auth:signup:code:count:email:";
    private static final String IP_COUNT_KEY_PREFIX = "auth:signup:code:count:ip:";
    private static final String TOKEN_KEY_PREFIX = "auth:signup:token:";

    private static final RedisScript<Long> COMPARE_AND_DELETE_SCRIPT =
            loadScript("/redis/compare-and-delete-challenge.lua");
    private static final RedisScript<Long> VERIFY_AND_INCREMENT_SCRIPT =
            loadScript("/redis/verify-and-increment-code.lua");

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.email-code-secret}")
    private String emailCodeSecret;

    public SignupEmailVerificationResponse requestCode(String rawEmail, String clientIp) {
        String normalizedEmail = User.normalizeEmail(rawEmail);

        // 3. 기존 가입 이메일 조회
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 4. 재전송 및 발송 횟수 제한 확인
        String cooldownKey = COOLDOWN_KEY_PREFIX + normalizedEmail;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
        enforceRateLimit(EMAIL_COUNT_KEY_PREFIX + normalizedEmail);
        enforceRateLimit(IP_COUNT_KEY_PREFIX + clientIp, IP_RATE_LIMIT);

        // 5. SecureRandom으로 숫자 6자리 코드 생성
        String code = generateCode();
        String challengeId = UUID.randomUUID().toString();
        // 6. 코드는 서버 Secret을 사용한 HMAC으로 변환하여 Redis에 저장 / 7. 유효시간 10분
        String codeKey = CODE_KEY_PREFIX + normalizedEmail;
        String challengeJson = objectMapper.writeValueAsString(new SignupCodeChallenge(challengeId, hmac(code), 0));
        // Redis에 challenge를 먼저 저장한 뒤에 메일을 발송한다(정책 순서).
        redisTemplate.opsForValue().set(codeKey, challengeJson, CODE_TTL);

        try {
            // 8. SMTP로 인증 메일 동기 발송
            emailSender.send(normalizedEmail, EmailType.SIGNUP_VERIFICATION, "이메일 인증 코드 안내",
                    buildHtmlBody(code), buildTextBody(code));
        } catch (CustomException e) {
            // 발송 실패 시, 이 요청이 방금 저장한 challenge와 지금 저장된 값이 같을 때만 지운다(compare-and-delete) —
            // 그 사이 재전송 요청이 새 코드를 이미 저장했다면 그건 건드리지 않는다.
            redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(codeKey), challengeId);
            throw e;
        }

        // 발송 실패 시에는 재전송 cooldown을 시작하지 않는다(정책) — 성공했을 때만 설정.
        redisTemplate.opsForValue().set(cooldownKey, "1", RESEND_COOLDOWN);

        // 9. SMTP 접수 성공 후 응답
        return SignupEmailVerificationResponse.of(CODE_TTL.toSeconds(), RESEND_COOLDOWN.toSeconds());
    }

    public SignupEmailVerificationConfirmResponse confirmCode(String rawEmail, String rawCode) {
        String normalizedEmail = User.normalizeEmail(rawEmail);
        String codeKey = CODE_KEY_PREFIX + normalizedEmail;

        // 코드 확인과 실패 횟수 증가를 Lua 스크립트 안에서 원자적으로 처리한다(SIGNUP-2 정책).
        Long result = redisTemplate.execute(VERIFY_AND_INCREMENT_SCRIPT, List.of(codeKey), hmac(rawCode));
        if (result == null || result != 1L) {
            // 불일치/만료/이미 사용됨/시도 초과를 전부 동일한 오류로 응답한다 — 남은 시도 횟수도 노출하지 않는다.
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        String signupToken = generateSignupToken();
        String tokenKey = TOKEN_KEY_PREFIX + sha256Hex(signupToken);
        redisTemplate.opsForValue().set(tokenKey, normalizedEmail, SIGNUP_TOKEN_TTL);

        return SignupEmailVerificationConfirmResponse.of(signupToken, SIGNUP_TOKEN_TTL.toSeconds());
    }

    // AUTH-4 1~2단계: signupToken 해시로 Redis 조회 후 요청 이메일과 정규화 값을 비교한다.
    // 토큰이 없거나(만료·오타) 이메일이 다르면 이메일 존재 여부를 노출하지 않는 동일한 오류로 처리한다.
    public String consumeSignupToken(String rawSignupToken, String rawEmail) {
        String normalizedEmail = User.normalizeEmail(rawEmail);
        String storedEmail = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + sha256Hex(rawSignupToken));
        if (storedEmail == null || !storedEmail.equals(normalizedEmail)) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }
        return normalizedEmail;
    }

    // AUTH-4 6단계: User 저장이 DB에 실제로 commit된 뒤에만 호출해야 한다(호출 순서는 컨트롤러가 보장).
    public void deleteSignupToken(String rawSignupToken) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + sha256Hex(rawSignupToken));
    }

    private String generateSignupToken() {
        byte[] bytes = new byte[SIGNUP_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("가입 토큰 해시 계산에 실패했습니다.", e);
        }
    }

    private void enforceRateLimit(String key) {
        enforceRateLimit(key, EMAIL_RATE_LIMIT);
    }

    private void enforceRateLimit(String key, int max) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW);
        }
        if (count != null && count > max) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String hmac(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(emailCodeSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("인증 코드 HMAC 계산에 실패했습니다.", e);
        }
    }

    private String buildTextBody(String code) {
        return """
                인증 코드: %s
                유효 시간: 10분

                본인이 요청하지 않았다면 이 이메일을 무시하세요.
                """.formatted(code);
    }

    private String buildHtmlBody(String code) {
        return """
                <p>인증 코드: <b>%s</b></p>
                <p>유효 시간: 10분</p>
                <p>본인이 요청하지 않았다면 이 이메일을 무시하세요.</p>
                """.formatted(code);
    }

    private static RedisScript<Long> loadScript(String classpathLocation) {
        try (InputStream is = EmailVerificationService.class.getResourceAsStream(classpathLocation)) {
            if (is == null) {
                throw new IllegalStateException(classpathLocation + "를 찾을 수 없습니다.");
            }
            return new DefaultRedisScript<>(new String(is.readAllBytes(), StandardCharsets.UTF_8), Long.class);
        } catch (IOException e) {
            throw new IllegalStateException("Redis Lua 스크립트 로드에 실패했습니다: " + classpathLocation, e);
        }
    }
}
