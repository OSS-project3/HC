package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// createOAuthUserIfAbsent는 REQUIRES_NEW로 별도 트랜잭션(별도 커넥션)에서 실행되므로, 클래스 레벨
// @Transactional(테스트 종료 시 자동 롤백)로 감싸면 이 테스트가 만든 선행 데이터가 그 별도 커넥션에서
// 안 보여 충돌 감지 자체가 성립하지 않는다 — 그래서 이 클래스는 의도적으로 @Transactional을 걸지 않고
// 매 테스트 실제 커밋 후 deleteAll()로 직접 정리한다.
@SpringBootTest
class UserServiceOAuthTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createOAuthUserIfAbsentCreatesNewUserWhenNoCollision() {
        User created = userService.createOAuthUserIfAbsent("New@Example.com", "oauth-new", "google", "New User");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getOauthId()).isEqualTo("oauth-new");
        assertThat(created.getOauthProvider()).isEqualTo("google");
        assertThat(created.getPasswordHash()).isNull();
    }

    @Test
    void createOAuthUserIfAbsentRejectsWhenEmailUsedByDifferentOAuthProvider() {
        userRepository.save(User.createOAuthUser("shared@example.com", "oauth-google", "google", "Existing"));

        assertThatThrownBy(() ->
                userService.createOAuthUserIfAbsent("shared@example.com", "oauth-naver", "naver", "New"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void createOAuthUserIfAbsentRejectsWhenEmailUsedByLocalAccount() {
        userRepository.save(User.createLocalUser("shared@example.com", "hashed-value", "Existing", "010-1234-5678"));

        assertThatThrownBy(() ->
                userService.createOAuthUserIfAbsent("shared@example.com", "oauth-google", "google", "New"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void createOAuthUserIfAbsentDetectsCollisionRegardlessOfEmailCaseOrWhitespace() {
        userRepository.save(User.createOAuthUser("shared@example.com", "oauth-google", "google", "Existing"));

        assertThatThrownBy(() ->
                userService.createOAuthUserIfAbsent("  Shared@Example.COM  ", "oauth-naver", "naver", "New"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
