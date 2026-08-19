package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.mail.EmailSender;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INQUIRY-3: GET /api/admin/inquiries, GET /api/admin/inquiries/{id}.
// INQUIRY-4: PATCH /api/admin/inquiries/{id}/answer.
@SpringBootTest
@AutoConfigureMockMvc
class InquiryAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InquiryRepository inquiryRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private EmailSender emailSender;

    private String adminToken;
    private String userToken;
    private Inquiry inquiry;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.createOAuthUser("inquiry-admin@example.com", "oauth-inquiry-admin", "google", "관리자"));
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(User.createOAuthUser("inquiry-admin-user@example.com", "oauth-inquiry-admin-user", "google", "일반사용자"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), UserRole.USER);

        inquiry = inquiryRepository.save(Inquiry.create(user.getId(), InquiryCategory.CARD_ISSUANCE,
                "홍길동", "hong@example.com", "010-1111-2222", "카드 발급 문의", "언제 발급되나요?"));
    }

    @Test
    void listReturnsAllInquiries() throws Exception {
        mockMvc.perform(get("/api/admin/inquiries")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(inquiry.getId()));
    }

    @Test
    void listWithUserTokenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/inquiries")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailReturnsInquiryRegardlessOfOwner() throws Exception {
        mockMvc.perform(get("/api/admin/inquiries/" + inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inquiry.getId()));
    }

    @Test
    void detailForMissingInquiryReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/inquiries/999999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void answerReturnsOkAndTransitionsToCompleted() throws Exception {
        mockMvc.perform(patch("/api/admin/inquiries/" + inquiry.getId() + "/answer")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":"영업일 기준 5일 이내 발급됩니다."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Inquiry updated = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getAnswer()).isEqualTo("영업일 기준 5일 이내 발급됩니다.");
    }

    @Test
    void answerWithBlankAnswerReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/inquiries/" + inquiry.getId() + "/answer")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void answerWithUserTokenReturnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/inquiries/" + inquiry.getId() + "/answer")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":"답변"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void statusChangesToCompletedWithoutAnswer() throws Exception {
        mockMvc.perform(patch("/api/admin/inquiries/" + inquiry.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Inquiry updated = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getStatus()).isEqualTo(com.example.honorcitizen.common.enums.InquiryStatus.COMPLETED);
        org.assertj.core.api.Assertions.assertThat(updated.getAnswer()).isNull();
    }

    @Test
    void statusForMissingInquiryReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/admin/inquiries/999999/status")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusWithUserTokenReturnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/inquiries/" + inquiry.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isForbidden());
    }
}
