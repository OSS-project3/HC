package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.EmailType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.dto.SignupEmailVerificationResponse;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.mail.EmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 실제 로컬 Redis(REDIS_PORT로 지정한 인스턴스)에 대해 동작을 검증한다 — StringRedisTemplate을 Mock하지
// 않고, TTL·쿠폴다운·레이트리밋 카운터가 실제 Redis 명령으로 정확히 동작하는지 확인하는 것이 목적이다.
// 이 클래스는 @Transactional을 걸지 않는다 — Redis 상태는 트랜잭션 롤백 대상이 아니므로 매 테스트
// 종료 후 사용한 키를 직접 정리한다(UserServiceOAuthTest와 동일한 이유의 비-@Transactional 패턴).
@SpringBootTest
class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private EmailSender emailSender;

    private static final String CLIENT_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private void cleanupRedis(String email, String ip) {
        redisTemplate.delete(List.of(
                "auth:signup:code:" + email,
                "auth:signup:code:cooldown:" + email,
                "auth:signup:code:count:email:" + email,
                "auth:signup:code:count:ip:" + ip));
    }

    @Test
    void requestCodeStoresChallengeAndSendsEmailForNewAddress() {
        String email = "new-signup@example.com";
        cleanupRedis(email, CLIENT_IP);

        SignupEmailVerificationResponse response = emailVerificationService.requestCode(email, CLIENT_IP);

        assertThat(response.getExpiresInSeconds()).isEqualTo(600L);
        assertThat(response.getResendAfterSeconds()).isEqualTo(60L);

        String codeKey = "auth:signup:code:" + email;
        assertThat(redisTemplate.hasKey(codeKey)).isTrue();
        Long ttlSeconds = redisTemplate.getExpire(codeKey, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isPositive().isLessThanOrEqualTo(600L);
        assertThat(redisTemplate.hasKey("auth:signup:code:cooldown:" + email)).isTrue();

        verify(emailSender).send(eq(email), eq(EmailType.SIGNUP_VERIFICATION), anyString(), anyString(), anyString());

        cleanupRedis(email, CLIENT_IP);
    }

    @Test
    void requestCodeRejectsAlreadyRegisteredEmail() {
        String email = "existing@example.com";
        cleanupRedis(email, CLIENT_IP);
        userRepository.save(User.createLocalUser(email, "hashed", "Existing", "010-1234-5678"));

        assertThatThrownBy(() -> emailVerificationService.requestCode(email, CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        assertThat(redisTemplate.hasKey("auth:signup:code:" + email)).isFalse();
    }

    @Test
    void requestCodeRejectsWithinResendCooldown() {
        String email = "cooldown@example.com";
        cleanupRedis(email, CLIENT_IP);

        emailVerificationService.requestCode(email, CLIENT_IP);

        assertThatThrownBy(() -> emailVerificationService.requestCode(email, CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);

        cleanupRedis(email, CLIENT_IP);
    }

    @Test
    void requestCodeRejectsWhenEmailHourlyLimitExceeded() {
        String email = "rate-email@example.com";
        cleanupRedis(email, CLIENT_IP);
        redisTemplate.opsForValue().set("auth:signup:code:count:email:" + email, "5", Duration.ofHours(1));

        assertThatThrownBy(() -> emailVerificationService.requestCode(email, CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        cleanupRedis(email, CLIENT_IP);
    }

    @Test
    void requestCodeRejectsWhenIpHourlyLimitExceeded() {
        String email = "rate-ip@example.com";
        String ip = "10.0.0.9";
        cleanupRedis(email, ip);
        redisTemplate.opsForValue().set("auth:signup:code:count:ip:" + ip, "20", Duration.ofHours(1));

        assertThatThrownBy(() -> emailVerificationService.requestCode(email, ip))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        cleanupRedis(email, ip);
    }

    @Test
    void requestCodeDeletesChallengeButNotCooldownWhenEmailDeliveryFails() {
        String email = "delivery-fail@example.com";
        cleanupRedis(email, CLIENT_IP);
        doThrow(new CustomException(ErrorCode.EMAIL_DELIVERY_FAILED))
                .when(emailSender).send(eq(email), any(), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> emailVerificationService.requestCode(email, CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DELIVERY_FAILED);

        assertThat(redisTemplate.hasKey("auth:signup:code:" + email)).isFalse();
        assertThat(redisTemplate.hasKey("auth:signup:code:cooldown:" + email)).isFalse();

        cleanupRedis(email, CLIENT_IP);
    }
}
