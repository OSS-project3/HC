package com.example.honorcitizen.api;

import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INQUIRY-1: POST /api/inquiries.
@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InquiryRepository inquiryRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.createOAuthUser("inquiry-user@example.com", "oauth-inquiry-user", "google", "홍길동"));
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
    }

    private String requestJson(boolean privacyConsent) {
        return """
                {"category":"카드 발급","name":"홍길동","email":"hong@example.com","phone":"010-1111-2222",
                "title":"카드 발급 문의","content":"언제 발급되나요?","privacyConsent":%s}
                """.formatted(privacyConsent);
    }

    @Test
    void createReturnsCreatedAndSavesInquiryOwnedByCaller() throws Exception {
        mockMvc.perform(post("/api/inquiries")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());

        var saved = inquiryRepository.findAll();
        org.assertj.core.api.Assertions.assertThat(saved).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(saved.get(0).getUserId()).isEqualTo(user.getId());
    }

    @Test
    void createWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithoutPrivacyConsentReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/inquiries")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void createWithBlankTitleReturnsBadRequest() throws Exception {
        String json = """
                {"category":"카드 발급","name":"홍길동","email":"hong@example.com","phone":"010-1111-2222",
                "title":"","content":"언제 발급되나요?","privacyConsent":true}
                """;
        mockMvc.perform(post("/api/inquiries")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }
}
