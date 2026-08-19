package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.UserStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// User 저장/복구는 @Transactional로 매 테스트 후 자동 롤백한다(UserServiceTest와 동일 패턴).
// LoginAttemptLimiter가 건드리는 Redis 키는 JPA 트랜잭션과 무관하므로 직접 정리한다.
@SpringBootTest
@Transactional
class UserServiceLoginTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private EntityManager entityManager;

    private static final String EMAIL = "login-test@example.com";
    private static final String PASSWORD = "correct-horse-battery";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        cleanupRedis();
    }

    @AfterEach
    void tearDown() {
        cleanupRedis();
    }

    private void cleanupRedis() {
        redisTemplate.delete(List.of("auth:login:fail:" + sha256Hex(EMAIL), "auth:login:lock:" + sha256Hex(EMAIL)));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private User saveLocalUser() {
        return userRepository.saveAndFlush(
                User.createLocalUser(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동"));
    }

    @Test
    void loginSucceedsForValidCredentials() {
        saveLocalUser();

        LoginResult result = userService.login(EMAIL, PASSWORD);

        assertThat(result.user().getEmail()).isEqualTo(EMAIL);
        assertThat(result.restored()).isFalse();
        assertThat(result.tokens().accessToken()).isNotBlank();
        assertThat(result.tokens().refreshToken()).isNotBlank();
    }

    @Test
    void loginRejectsUnknownEmail() {
        assertThatThrownBy(() -> userService.login("never-signed-up@example.com", PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginRejectsWrongPasswordWithSameErrorAsUnknownEmail() {
        saveLocalUser();

        assertThatThrownBy(() -> userService.login(EMAIL, "wrong-password"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginRejectsOAuthOnlyAccountWithSameError() {
        userRepository.saveAndFlush(User.createOAuthUser(EMAIL, "oauth-sub", "google", "홍길동"));

        assertThatThrownBy(() -> userService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginLocksAfterFiveFailuresEvenWithCorrectPassword() {
        saveLocalUser();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> userService.login(EMAIL, "wrong-password"))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
        }

        assertThatThrownBy(() -> userService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void loginResetsFailureCounterOnSuccess() {
        saveLocalUser();

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> userService.login(EMAIL, "wrong-password"));
        }
        userService.login(EMAIL, PASSWORD);

        assertThat(redisTemplate.hasKey("auth:login:fail:" + sha256Hex(EMAIL))).isFalse();
        assertThat(redisTemplate.hasKey("auth:login:lock:" + sha256Hex(EMAIL))).isFalse();
    }

    @Test
    void loginAutoRestoresAccountWithinGracePeriod() {
        User user = withdrawnUserBackdatedBy(1);

        LoginResult result = userService.login(EMAIL, PASSWORD);

        assertThat(result.restored()).isTrue();
        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void loginRejectsAccountPastGracePeriodEvenWithCorrectPassword() {
        User user = withdrawnUserBackdatedBy(8);

        assertThatThrownBy(() -> userService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    private User withdrawnUserBackdatedBy(int daysAgo) {
        User user = saveLocalUser();
        user.withdraw();
        user = userRepository.saveAndFlush(user);

        entityManager.createQuery("UPDATE User u SET u.withdrawalRequestedAt = :ts WHERE u.id = :id")
                .setParameter("ts", LocalDateTime.now().minusDays(daysAgo))
                .setParameter("id", user.getId())
                .executeUpdate();
        entityManager.clear();

        return userRepository.findById(user.getId()).orElseThrow();
    }
}
