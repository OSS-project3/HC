package com.example.honorcitizen.api;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AUTH-5(POST /api/auth/login) — 세부 분기(계정없음/OAuth전용/탈퇴복구 등)는 UserServiceLoginTest가
// 이미 검증했으므로, 여기서는 permitAll 라우트·쿠키 발급·에러 envelope 등 HTTP 계층만 확인한다(RULES.md §8).
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EMAIL = "auth5-login@example.com";
    private static final String PASSWORD = "correct-horse-battery";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        cleanupRedis();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
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

    private String loginRequestJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    @Test
    void loginSucceedsAndIssuesCookiesWithoutAnyAuthHeader() throws Exception {
        userRepository.save(User.createLocalUser(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginRequestJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.restored").value(false))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void loginRejectsWrongPasswordWithInvalidCredentialsEnvelope() throws Exception {
        userRepository.save(User.createLocalUser(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginRequestJson(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginLocksAfterFiveFailuresAndReturnsAccountLocked() throws Exception {
        userRepository.save(User.createLocalUser(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(loginRequestJson(EMAIL, "wrong-password")));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginRequestJson(EMAIL, PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }
}
