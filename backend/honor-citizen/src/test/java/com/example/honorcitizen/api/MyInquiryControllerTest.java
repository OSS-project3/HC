package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.InquiryCategory;
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

// INQUIRY-2: GET /api/my/inquiries, GET /api/my/inquiries/{id}.
@SpringBootTest
@AutoConfigureMockMvc
class MyInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InquiryRepository inquiryRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String ownerToken;
    private String otherToken;
    private Inquiry inquiry;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(User.createOAuthUser("myinquiry-owner@example.com", "oauth-myinquiry-owner", "google", "Owner"));
        ownerToken = "Bearer " + jwtTokenProvider.generateAccessToken(owner.getId(), owner.getRole());

        User other = userRepository.save(User.createOAuthUser("myinquiry-other@example.com", "oauth-myinquiry-other", "google", "Other"));
        otherToken = "Bearer " + jwtTokenProvider.generateAccessToken(other.getId(), other.getRole());

        inquiry = inquiryRepository.save(Inquiry.create(owner.getId(), InquiryCategory.CARD_ISSUANCE,
                "홍길동", "hong@example.com", "010-1111-2222", "카드 발급 문의", "언제 발급되나요?"));
    }

    @Test
    void listReturnsOnlyOwnInquiries() throws Exception {
        mockMvc.perform(get("/api/my/inquiries")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(inquiry.getId()));
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/my/inquiries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detailReturnsOwnInquiry() throws Exception {
        mockMvc.perform(get("/api/my/inquiries/" + inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inquiry.getId()))
                .andExpect(jsonPath("$.data.title").value("카드 발급 문의"));
    }

    @Test
    void detailForNonOwnerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/my/inquiries/" + inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailForMissingInquiryReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/my/inquiries/999999")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNotFound());
    }
}
