package com.example.honorcitizen.domain.user.entity;

import com.example.honorcitizen.common.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void newUserIsActiveByDefault() {
        User user = User.createOAuthUser("test@example.com", "oauth-1", "google", "Test");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isWithdrawn()).isFalse();
    }

    @Test
    void withdrawMarksUserWithdrawn() {
        User user = User.createOAuthUser("test@example.com", "oauth-1", "google", "Test");

        user.withdraw();

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getWithdrawalRequestedAt()).isNotNull();
    }

    @Test
    void normalizeEmailTrimsAndLowercases() {
        assertThat(User.normalizeEmail("  Test@Example.COM  ")).isEqualTo("test@example.com");
    }

    @Test
    void normalizeEmailReturnsNullForNull() {
        assertThat(User.normalizeEmail(null)).isNull();
    }

    @Test
    void createOAuthUserNormalizesEmailAndLeavesPasswordHashNull() {
        User user = User.createOAuthUser("  Test@Example.COM  ", "oauth-1", "google", "Test");

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getOauthId()).isEqualTo("oauth-1");
        assertThat(user.getOauthProvider()).isEqualTo("google");
        assertThat(user.getPasswordHash()).isNull();
    }

    @Test
    void createLocalUserNormalizesEmailAndLeavesOAuthFieldsNull() {
        User user = User.createLocalUser("  Test@Example.COM  ", "hashed-value", "Test");

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-value");
        assertThat(user.getOauthId()).isNull();
        assertThat(user.getOauthProvider()).isNull();
    }
}
