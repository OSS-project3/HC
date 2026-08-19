package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationConfirmResponse;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.mail.EmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

// 실제 로컬 Redis(REDIS_PORT로 지정한 인스턴스)에 대해 동작을 검증한다 — EmailVerificationServiceTest와
// 동일한 이유로 @Transactional을 걸지 않고 매 테스트 종료 후 사용한 키를 직접 정리한다.
@SpringBootTest
class EmailVerificationServiceConfirmTest {

    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private EmailSender emailSender;

    private static final String CLIENT_IP = "127.0.0.1";
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private void cleanupRedis(String email) {
        redisTemplate.delete(List.of(
                "auth:signup:code:" + email,
                "auth:signup:code:cooldown:" + email,
                "auth:signup:code:count:email:" + email,
                "auth:signup:code:count:ip:" + CLIENT_IP));
    }

    private String requestCodeAndCapture(String email) {
        var textBodyCaptor = forClass(String.class);
        emailVerificationService.requestCode(email, CLIENT_IP);
        verify(emailSender).send(eq(email), any(), anyString(), anyString(), textBodyCaptor.capture());
        Matcher matcher = CODE_PATTERN.matcher(textBodyCaptor.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void confirmCodeIssuesSignupTokenAndDeletesChallengeOnSuccess() throws Exception {
        String email = "confirm-success@example.com";
        cleanupRedis(email);
        String code = requestCodeAndCapture(email);

        SignupEmailVerificationConfirmResponse response = emailVerificationService.confirmCode(email, code);

        assertThat(response.getSignupToken()).isNotBlank();
        assertThat(response.getExpiresInSeconds()).isEqualTo(1800L);
        assertThat(redisTemplate.hasKey("auth:signup:code:" + email)).isFalse();

        String tokenKey = "auth:signup:token:" + sha256Hex(response.getSignupToken());
        assertThat(redisTemplate.opsForValue().get(tokenKey)).isEqualTo(email);
        Long ttlSeconds = redisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isPositive().isLessThanOrEqualTo(1800L);

        redisTemplate.delete(tokenKey);
        cleanupRedis(email);
    }

    @Test
    void confirmCodeNormalizesEmailBeforeLookup() throws Exception {
        String email = "confirm-normalize@example.com";
        cleanupRedis(email);
        String code = requestCodeAndCapture(email);

        SignupEmailVerificationConfirmResponse response =
                emailVerificationService.confirmCode("  Confirm-Normalize@Example.COM  ", code);

        String tokenKey = "auth:signup:token:" + sha256Hex(response.getSignupToken());
        assertThat(redisTemplate.opsForValue().get(tokenKey)).isEqualTo(email);

        redisTemplate.delete(tokenKey);
        cleanupRedis(email);
    }

    @Test
    void confirmCodeRejectsWrongCodeButKeepsChallengeForRetry() {
        String email = "confirm-wrong@example.com";
        cleanupRedis(email);
        requestCodeAndCapture(email);

        assertThatThrownBy(() -> emailVerificationService.confirmCode(email, "000000"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);

        assertThat(redisTemplate.hasKey("auth:signup:code:" + email)).isTrue();

        cleanupRedis(email);
    }

    @Test
    void confirmCodeDiscardsChallengeAfterFiveFailures() {
        String email = "confirm-exhausted@example.com";
        cleanupRedis(email);
        String correctCode = requestCodeAndCapture(email);
        String wrongCode = "000000".equals(correctCode) ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> emailVerificationService.confirmCode(email, wrongCode))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
        }

        assertThat(redisTemplate.hasKey("auth:signup:code:" + email)).isFalse();

        // challenge가 이미 폐기됐으므로 원래 정답 코드로도 더 이상 통과할 수 없다.
        assertThatThrownBy(() -> emailVerificationService.confirmCode(email, correctCode))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);

        cleanupRedis(email);
    }

    @Test
    void confirmCodeRejectsReuseAfterSuccess() {
        String email = "confirm-reuse@example.com";
        cleanupRedis(email);
        String code = requestCodeAndCapture(email);

        SignupEmailVerificationConfirmResponse first = emailVerificationService.confirmCode(email, code);
        assertThat(first.getSignupToken()).isNotBlank();

        assertThatThrownBy(() -> emailVerificationService.confirmCode(email, code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);

        cleanupRedis(email);
    }

    @Test
    void confirmCodeRejectsWhenNoChallengeWasEverRequested() {
        String email = "confirm-never-requested@example.com";
        cleanupRedis(email);

        assertThatThrownBy(() -> emailVerificationService.confirmCode(email, "123456"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }
}
