package com.example.honorcitizen.api;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// QA 체크리스트 4번: 기존 테스트 전부가 User.agreeTerms()를 엔티티 메서드로 직접 호출해 상태만
// 세팅했을 뿐, 실제 POST /api/auth/terms 엔드포인트(AuthController.agreeTerms → UserService.agreeTerms)를
// 통과하는 테스트가 없었다. 이 파일이 그 공백을 메운다.
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTermsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user = User.createOAuthUser("terms-user@example.com", "oauth-terms", "google", "약관테스트");
        user = userRepository.save(user);
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
    }

    private String termsRequestJson(boolean privacy, boolean imageUpload, boolean shipping) {
        return """
                {"privacyAgreed": %s, "imageUploadAgreed": %s, "shippingAgreed": %s}
                """.formatted(privacy, imageUpload, shipping);
    }

    @Test
    void agreeTermsWithAllTruePersistsAgreementAndReturnsAgreedState() throws Exception {
        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(termsRequestJson(true, true, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.termsAgreed").value(true))
                .andExpect(jsonPath("$.data.privacyAgreed").value(true))
                .andExpect(jsonPath("$.data.imageUploadAgreed").value(true))
                .andExpect(jsonPath("$.data.shippingAgreed").value(true))
                .andExpect(jsonPath("$.data.agreedAt").isNotEmpty());

        User persisted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persisted.isTermsAgreed()).isTrue();
        assertThat(persisted.isPrivacyAgreed()).isTrue();
        assertThat(persisted.isImageUploadAgreed()).isTrue();
        assertThat(persisted.isShippingAgreed()).isTrue();
        assertThat(persisted.getTermsAgreedAt()).isNotNull();
    }

    @Test
    void agreeTermsWithOneFieldFalsePersistsPartialAgreementAndAllowsResubmission() throws Exception {
        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(termsRequestJson(true, false, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUploadAgreed").value(false));

        User persisted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persisted.isImageUploadAgreed()).isFalse();
        assertThat(persisted.isAllTermsAgreed()).isFalse();

        // 전체 동의가 아니므로 다시 제출할 수 있어야 한다(TERMS_ALREADY_AGREED로 막히면 안 됨).
        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(termsRequestJson(true, true, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUploadAgreed").value(true));
    }

    @Test
    void agreeTermsTwiceAfterFullAgreementReturnsTermsAlreadyAgreed() throws Exception {
        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(termsRequestJson(true, true, true)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(termsRequestJson(true, true, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TERMS_ALREADY_AGREED"));
    }

    @Test
    void agreeTermsReturnsInvalidInputWhenFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"privacyAgreed": true, "imageUploadAgreed": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void agreeTermsReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(termsRequestJson(true, true, true)))
                .andExpect(status().isUnauthorized());
    }
}
