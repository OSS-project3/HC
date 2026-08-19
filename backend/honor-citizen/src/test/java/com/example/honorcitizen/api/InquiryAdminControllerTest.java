package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INQUIRY-3: GET /api/admin/inquiries, GET /api/admin/inquiries/{id}.
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
}
