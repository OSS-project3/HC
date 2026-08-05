package com.example.honorcitizen.domain.user.entity;

import com.example.honorcitizen.common.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void newUserIsActiveByDefault() {
        User user = User.createNewUser("test@example.com", "oauth-1", "google", "Test");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isWithdrawn()).isFalse();
        assertThat(user.isRestorable()).isFalse();
    }

    @Test
    void withdrawMarksUserWithdrawnAndRestorable() {
        User user = User.createNewUser("test@example.com", "oauth-1", "google", "Test");

        user.withdraw();

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getWithdrawalRequestedAt()).isNotNull();
        assertThat(user.isRestorable()).isTrue();
    }

    @Test
    void restoreClearsWithdrawnStateWithinGracePeriod() {
        User user = User.createNewUser("test@example.com", "oauth-1", "google", "Test");
        user.withdraw();

        user.restore();

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isWithdrawn()).isFalse();
        assertThat(user.getWithdrawalRequestedAt()).isNull();
    }

    @Test
    void activeUserIsNotRestorable() {
        User user = User.createNewUser("test@example.com", "oauth-1", "google", "Test");

        assertThat(user.isRestorable()).isFalse();
    }
}
