package com.example.honorcitizen.api;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AUTH-6(PATCH /api/users/me/password). 실제 로컬 Redis(세션 무효화)를 사용하므로 REDIS_PORT로
// 지정한 인스턴스가 필요하다 — TokenSessionStore/LoginAttemptLimiter를 쓰는 다른 auth 테스트들과 동일.
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerChangePasswordTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CURRENT_PASSWORD = "old-horse-battery";

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = userRepository.save(
                User.createLocalUser("change-password@example.com", passwordEncoder.encode(CURRENT_PASSWORD), "홍길동"));
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String requestJson(String currentPassword, String newPassword) {
        return """
                {"currentPassword":"%s","newPassword":"%s"}
                """.formatted(currentPassword, newPassword);
    }

    @Test
    void changePasswordSucceedsAndBlacklistsCurrentAccessToken() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(CURRENT_PASSWORD, "new-horse-battery")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("new-horse-battery", updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, updated.getPasswordHash())).isFalse();
        assertThat(updated.getRefreshToken()).isNull();

        // 방금 비밀번호 변경에 쓰인 accessToken은 전체 세션 무효화로 블랙리스트되어 더 이상 인증에 쓸 수 없어야 함
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordAndLeavesPasswordUnchanged() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("wrong-current-password", "new-horse-battery")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CURRENT_PASSWORD_MISMATCH"));

        User unchanged = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, unchanged.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordRejectsOAuthOnlyAccount() throws Exception {
        User oauthUser = userRepository.save(
                User.createOAuthUser("oauth-only@example.com", "oauth-sub", "google", "OAuth유저"));
        String oauthToken = "Bearer " + jwtTokenProvider.generateAccessToken(oauthUser.getId(), oauthUser.getRole());

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", oauthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("anything", "new-horse-battery")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void changePasswordReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(CURRENT_PASSWORD, "new-horse-battery")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordReturnsInvalidInputWhenNewPasswordTooShort() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(CURRENT_PASSWORD, "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }
}
