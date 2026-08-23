package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.TokenSessionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

/**
 * RECOVERY-2 미검증 항목(d) — "Redis 세션 무효화가 성공한 뒤 DB commit이 실패하면 종료된 세션을
 * 복구하지 않는다"(docs/api/auth.md API 8 ⑤-1)를 검증한다. 진짜 DB 장애를 무작위로 유발할 테스트
 * 인프라는 없으므로, recordUserAccessRevocation은 실제 Redis에 정상 기록되게 두고(callRealMethod)
 * 그 직후 현재 트랜잭션에 beforeCommit 훅을 등록해 Hibernate flush 직전에 실패를 강제한다 — 이 훅은
 * Spring의 AbstractPlatformTransactionManager가 실제 doCommit(및 그 안의 flush)보다 먼저 호출하므로,
 * "Redis는 성공했는데 DB commit만 실패" 순서를 결정론적으로 재현하는 유일한 방법이다.
 */
@SpringBootTest
class UserServiceResetPasswordRollbackTest {

    private static final String OLD_PASSWORD = "correct-horse-battery";

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @MockitoSpyBean
    private TokenSessionStore tokenSessionStore;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        deleteRevokedAfterKeys();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        deleteRevokedAfterKeys();
    }

    private void deleteRevokedAfterKeys() {
        var keys = redisTemplate.keys("auth:access:user-revoked-after:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void passwordChangeRollsBackWhenDbCommitFailsAfterRedisRevocationSucceeds() {
        User user = userRepository.saveAndFlush(User.createLocalUser(
                "rollback@example.com", passwordEncoder.encode(OLD_PASSWORD), "홍길동", "010-1234-5678"));

        doAnswer(invocation -> {
            invocation.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new DataAccessResourceFailureException("simulated DB failure at commit");
                }
            });
            return null;
        }).when(tokenSessionStore).recordUserAccessRevocation(user.getId());

        assertThatThrownBy(() -> userService.resetPassword(user.getId(), "should-not-be-saved-1"))
                .isInstanceOf(DataAccessResourceFailureException.class);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, reloaded.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("should-not-be-saved-1", reloaded.getPasswordHash())).isFalse();

        // Redis revoked-after 키는 DB rollback과 무관하게 실제로 그대로 남아있다 — "세션 미복구"의 실체.
        String revokedAfterKey = "auth:access:user-revoked-after:" + user.getId();
        assertThat(redisTemplate.hasKey(revokedAfterKey)).isTrue();
    }
}
