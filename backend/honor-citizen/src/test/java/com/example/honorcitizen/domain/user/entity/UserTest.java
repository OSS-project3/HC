package com.example.honorcitizen.domain.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 2026-08-19 정책 변경(WITHDRAW-3B): User는 더 이상 탈퇴 상태를 표현하지 않는다(row 존재 자체가
// 활성 계정이라는 뜻 — arch.md §4.1 "탈퇴 정책"). withdraw()는 WITHDRAW-4에서 실제 하드 삭제 로직이
// 채워지기 전까지의 빈 자리라 엔티티 레벨에서 검증할 상태 변화가 없다.
class UserTest {

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
