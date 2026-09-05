package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 통계 — ADMIN만 허용, 집계 로직 자체는 AdminStatsServiceTest가 커버.
@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private InquiryRepository inquiryRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        inquiryRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.createOAuthUser("admin-stats@example.com", "oauth-admin-stats", "google", "Admin");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(User.createOAuthUser("user-stats@example.com", "oauth-user-stats", "google", "User"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), UserRole.USER);

        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-stats-ctrl", null, BigDecimal.valueOf(30000)));
        applicationRepository.save(Application.createIndividual(
                1L, "APP-2026-STATC001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicationRepository.save(Application.createGroup(
                1L, "APP-2026-STATC002", cardType.getId(), IssueType.MOBILE, true, 3, null, null, null));
        inquiryRepository.save(Inquiry.create(
                1L, InquiryCategory.OTHER, "홍길동", "user@example.com", "010-0000-0000", "문의", "내용"));
    }

    @Test
    void adminCanReadStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalApplications").value(2))
                .andExpect(jsonPath("$.data.individualApplications").value(1))
                .andExpect(jsonPath("$.data.groupApplications").value(1))
                .andExpect(jsonPath("$.data.totalInquiries").value(1))
                .andExpect(jsonPath("$.data.pendingInquiries").value(1))
                .andExpect(jsonPath("$.data.completedInquiries").value(0));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }
}
