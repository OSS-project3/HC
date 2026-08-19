package com.example.honorcitizen.api;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.domain.user.service.EmailVerificationService;
import com.example.honorcitizen.infra.mail.EmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AUTH-4(POST /api/auth/signup) — permitAll 라우트·쿠키 발급·가입 토큰 소비 순서는 서비스 단위 테스트로
// 확인할 수 없는 HTTP 계층이라 컨트롤러 테스트로 검증한다(RULES.md §8). SIGNUP-1/2 자체의 세부 분기는
// EmailVerificationServiceTest/EmailVerificationServiceConfirmTest가 이미 검증했으므로, 여기서는
// 유효한 signupToken을 얻는 과정을 헬퍼로만 재사용하고 AUTH-4 자체의 로직만 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSignupTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @MockitoBean
    private EmailSender emailSender;

    private static final String CLIENT_IP = "127.0.0.1";
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private void cleanupRedis(String email) {
        redisTemplate.delete(List.of(
                "auth:signup:code:" + email,
                "auth:signup:code:cooldown:" + email,
                "auth:signup:code:count:email:" + email,
                "auth:signup:code:count:ip:" + CLIENT_IP));
    }

    private String issueSignupToken(String email) {
        var textBodyCaptor = ArgumentCaptor.forClass(String.class);
        emailVerificationService.requestCode(email, CLIENT_IP);
        verify(emailSender).send(eq(email), any(), anyString(), anyString(), textBodyCaptor.capture());
        Matcher matcher = CODE_PATTERN.matcher(textBodyCaptor.getValue());
        assertThat(matcher.find()).isTrue();
        return emailVerificationService.confirmCode(email, matcher.group()).getSignupToken();
    }

    private String signupRequestJson(String email, String signupToken, String password, String name) {
        return """
                {"email":"%s","signupToken":"%s","password":"%s","name":"%s"}
                """.formatted(email, signupToken, password, name);
    }

    @Test
    void signupCreatesUserAndIssuesCookiesWithoutAnyAuthHeader() throws Exception {
        String email = "auth4-success@example.com";
        cleanupRedis(email);
        String signupToken = issueSignupToken(email);

        // Authorization 헤더/쿠키를 전혀 싣지 않는다 — permitAll 라우트임을 이 자체로 검증한다.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupRequestJson(email, signupToken, "correct-horse-battery", "홍길동")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotBlank().isNotEqualTo("correct-horse-battery");
        assertThat(saved.isAllTermsAgreed()).isFalse();
        assertThat(saved.getOauthId()).isNull();

        // 가입 토큰은 1회성이라 성공 직후 재사용이 거절돼야 한다.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupRequestJson(email, signupToken, "another-password", "다른이름")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNUP_TOKEN"));
    }

    @Test
    void signupRejectsUnknownOrExpiredSignupToken() throws Exception {
        String email = "auth4-unknown-token@example.com";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupRequestJson(email, "never-issued-token", "some-password", "홍길동")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNUP_TOKEN"));
    }

    @Test
    void signupRejectsWhenTokenBelongsToDifferentEmail() throws Exception {
        String tokenOwnerEmail = "auth4-token-owner@example.com";
        String otherEmail = "auth4-other@example.com";
        cleanupRedis(tokenOwnerEmail);
        String signupToken = issueSignupToken(tokenOwnerEmail);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupRequestJson(otherEmail, signupToken, "some-password", "홍길동")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNUP_TOKEN"));

        assertThat(userRepository.findByEmail(otherEmail)).isEmpty();
    }

    @Test
    void signupRejectsDuplicateEmailAndKeepsTokenIntact() throws Exception {
        String email = "auth4-duplicate@example.com";
        cleanupRedis(email);
        String signupToken = issueSignupToken(email);
        // 코드 요청~가입 사이에 다른 경로로 같은 이메일 계정이 먼저 생겼다고 가정.
        userRepository.save(User.createLocalUser(email, "already-hashed", "선점계정"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupRequestJson(email, signupToken, "some-password", "홍길동")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));

        // DB 저장이 실패했으니 가입 토큰을 지우지 않았어야 한다(정책: DB commit 성공 후에만 삭제).
        assertThat(userRepository.findByEmail(email)).hasValueSatisfying(
                u -> assertThat(u.getName()).isEqualTo("선점계정"));
    }
}
