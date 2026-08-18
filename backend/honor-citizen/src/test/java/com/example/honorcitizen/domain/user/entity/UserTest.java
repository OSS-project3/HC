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
        assertThat(user.isRestorable()).isFalse();
    }

    @Test
    void withdrawMarksUserWithdrawnAndRestorable() {
        User user = User.createOAuthUser("test@example.com", "oauth-1", "google", "Test");

        user.withdraw();

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getWithdrawalRequestedAt()).isNotNull();
        assertThat(user.isRestorable()).isTrue();
    }

    @Test
    void restoreClearsWithdrawnStateWithinGracePeriod() {
        User user = User.createOAuthUser("test@example.com", "oauth-1", "google", "Test");
        user.withdraw();

        user.restore();

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isWithdrawn()).isFalse();
        assertThat(user.getWithdrawalRequestedAt()).isNull();
    }

    @Test
    void activeUserIsNotRestorable() {
        User user = User.createOAuthUser("test@example.com", "oauth-1", "google", "Test");

        assertThat(user.isRestorable()).isFalse();
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

    @Test
    void anonymizeClearsPasswordHash() {
        User user = User.createLocalUser("test@example.com", "hashed-value", "Test");

        user.anonymize();

        assertThat(user.getPasswordHash()).isNull();
    }
}
