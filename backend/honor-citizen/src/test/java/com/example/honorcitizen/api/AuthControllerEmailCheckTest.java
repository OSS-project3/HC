package com.example.honorcitizen.api;

import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AUTH-3(POST /api/auth/email/check) — permitAll 라우트 여부는 서비스 단위 테스트로 확인할 수 없는
// HTTP 계층이라 컨트롤러 테스트로 검증한다(RULES.md §8). 서비스 로직 자체가 존재 여부 확인 하나뿐이라
// 별도 서비스 계층 테스트는 두지 않는다.
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerEmailCheckTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String emailCheckRequestJson(String email) {
        return """
                {"email":"%s"}
                """.formatted(email);
    }

    @Test
    void checkEmailReturnsTrueForExistingLocalAccountWithoutAnyAuthHeader() throws Exception {
        userRepository.save(User.createLocalUser("local-account@example.com", "hashed-value", "홍길동"));

        mockMvc.perform(post("/api/auth/email/check")
                        .contentType("application/json")
                        .content(emailCheckRequestJson("local-account@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    void checkEmailReturnsTrueForExistingOAuthAccount() throws Exception {
        userRepository.save(User.createOAuthUser("oauth-account@example.com", "oauth-sub", "google", "홍길동"));

        mockMvc.perform(post("/api/auth/email/check")
                        .contentType("application/json")
                        .content(emailCheckRequestJson("oauth-account@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    void checkEmailReturnsFalseForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/email/check")
                        .contentType("application/json")
                        .content(emailCheckRequestJson("never-signed-up@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false));
    }

    @Test
    void checkEmailMatchesRegardlessOfCase() throws Exception {
        userRepository.save(User.createLocalUser("normalize-check@example.com", "hashed-value", "홍길동"));

        mockMvc.perform(post("/api/auth/email/check")
                        .contentType("application/json")
                        .content(emailCheckRequestJson("Normalize-Check@Example.COM")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true));
    }
}
