package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 십이간지·카드 디자인 선택 이미지 미리보기(2026-09-22 확정) — GET /api/admin/zodiac-designs/{designSet}/preview.
@SpringBootTest
@AutoConfigureMockMvc
class ZodiacDesignControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("zodiac-ctrl-admin@example.com", "oauth-zodiac-ctrl-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(
                User.createOAuthUser("zodiac-ctrl-user@example.com", "oauth-zodiac-ctrl-user", "google", "User"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
    }

    @Test
    void returnsFixedFourAnimalsForValidDesignSet() throws Exception {
        mockMvc.perform(get("/api/admin/zodiac-designs/1/preview")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.designSet").value(1))
                .andExpect(jsonPath("$.data.animals.length()").value(4))
                .andExpect(jsonPath("$.data.animals[0].code").value("RAT"))
                .andExpect(jsonPath("$.data.animals[1].code").value("TIGER"))
                .andExpect(jsonPath("$.data.animals[2].code").value("DRAGON"))
                .andExpect(jsonPath("$.data.animals[3].code").value("PIG"));
    }

    @Test
    void rejectsDesignSetOutOfRange() throws Exception {
        mockMvc.perform(get("/api/admin/zodiac-designs/6/preview")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/zodiac-designs/1/preview")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/zodiac-designs/1/preview"))
                .andExpect(status().isUnauthorized());
    }
}
