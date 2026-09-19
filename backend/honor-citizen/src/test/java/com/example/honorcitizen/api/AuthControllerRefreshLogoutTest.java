package com.example.honorcitizen.api;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// QA 체크리스트 5번: 리프레시/로그아웃 엔드포인트 자체의 실제 왕복(로그인→리프레시→재사용 방지,
// 로그아웃→세션 무효화)을 검증하는 테스트가 없었다 — 기존 TokenSessionStoreSessionValidationTest는
// isAccessTokenSessionValid()의 fail-open/closed 분기만 Mock Redis로 검증했을 뿐, 실제 컨트롤러
// 경로(rotateRefreshToken의 1회용/재사용 감지 포함)는 통과한 적이 없었다.
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRefreshLogoutTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "auth-refresh-logout@example.com";
    private static final String PASSWORD = "correct-horse-battery";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.createLocalUser(EMAIL, passwordEncoder.encode(PASSWORD), "홍길동", "010-1234-5678"));
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void refreshRotatesTokensAndNewAccessTokenWorksOnProtectedRoute() throws Exception {
        MvcResult loginResult = login();
        Cookie oldRefreshToken = loginResult.getResponse().getCookie("refreshToken");
        Cookie oldAccessToken = loginResult.getResponse().getCookie("accessToken");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(oldRefreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Cookie newAccessToken = refreshResult.getResponse().getCookie("accessToken");
        Cookie newRefreshToken = refreshResult.getResponse().getCookie("refreshToken");
        assertThat(newAccessToken.getValue()).isNotEqualTo(oldAccessToken.getValue());
        assertThat(newRefreshToken.getValue()).isNotEqualTo(oldRefreshToken.getValue());

        mockMvc.perform(get("/api/users/me").cookie(newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    @Test
    void reusingRotatedOutRefreshTokenIsRejectedAndInvalidatesTheWholeSession() throws Exception {
        MvcResult loginResult = login();
        Cookie oldRefreshToken = loginResult.getResponse().getCookie("refreshToken");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(oldRefreshToken))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotatedRefreshToken = refreshResult.getResponse().getCookie("refreshToken");

        // 이미 회전되어 폐기된 구 refresh token 재사용 → 탈취 의심으로 거부되어야 한다.
        mockMvc.perform(post("/api/auth/refresh").cookie(oldRefreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_REUSE_DETECTED"));

        // 재사용 감지는 해당 사용자의 전체 세션을 무효화해야 하므로, 방금 정상 발급된 새 refresh
        // token도 더 이상 쓸 수 없어야 한다.
        mockMvc.perform(post("/api/auth/refresh").cookie(rotatedRefreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshWithoutCookieReturnsInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutBlacklistsAccessTokenAndInvalidatesRefreshToken() throws Exception {
        MvcResult loginResult = login();
        Cookie accessToken = loginResult.getResponse().getCookie("accessToken");
        Cookie refreshToken = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/auth/logout").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 로그아웃에 쓰인 accessToken은 블랙리스트되어 더 이상 인증에 쓸 수 없어야 한다.
        mockMvc.perform(get("/api/users/me").cookie(accessToken))
                .andExpect(status().isUnauthorized());

        // 세션 자체가 무효화되므로 로그아웃 전 발급된 refreshToken으로도 재발급받을 수 없어야 한다.
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }
}
