package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 십이간지·카드 디자인 선택 이미지 미리보기(2026-09-22 확정) — GET /api/admin/card-designs/{id}/preview.
// 기존 list API(GET /api/admin/card-designs)는 CardDesignServiceTest에서 이미 커버되므로 여기선
// 새 preview 엔드포인트만 다룬다.
@SpringBootTest
@AutoConfigureMockMvc
class CardDesignControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;

    private String adminToken;
    private String userToken;
    private Long honorKoreanTypeId;

    @BeforeEach
    void setUp() {
        cardDesignRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("carddesign-ctrl-admin@example.com", "oauth-carddesign-ctrl-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(
                User.createOAuthUser("carddesign-ctrl-user@example.com", "oauth-carddesign-ctrl-user", "google", "User"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());

        honorKoreanTypeId = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-carddesign-ctrl", null, BigDecimal.ZERO)).getId();
    }

    @Test
    void returnsBothSidesForRegularCardDesign() throws Exception {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인1-ctrl", 1, CardDesignOrientation.LANDSCAPE, null, null, true));

        mockMvc.perform(get("/api/admin/card-designs/" + design.getId() + "/preview")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardDesignId").value(design.getId()))
                .andExpect(jsonPath("$.data.frontImageBase64").isNotEmpty())
                .andExpect(jsonPath("$.data.backImageBase64").isNotEmpty());
    }

    @Test
    void returnsNotFoundForUnknownDesign() throws Exception {
        mockMvc.perform(get("/api/admin/card-designs/999999/preview")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForInactiveDesign() throws Exception {
        CardDesign inactive = CardDesign.create(
                honorKoreanTypeId, "디자인-비활성-ctrl", 9, CardDesignOrientation.LANDSCAPE, null, null, false);
        inactive.deactivate();
        CardDesign saved = cardDesignRepository.save(inactive);

        mockMvc.perform(get("/api/admin/card-designs/" + saved.getId() + "/preview")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsNonAdmin() throws Exception {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인-권한-ctrl", 2, CardDesignOrientation.LANDSCAPE, null, null, true));

        mockMvc.perform(get("/api/admin/card-designs/" + design.getId() + "/preview")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWithoutToken() throws Exception {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인-토큰없음-ctrl", 3, CardDesignOrientation.LANDSCAPE, null, null, true));

        mockMvc.perform(get("/api/admin/card-designs/" + design.getId() + "/preview"))
                .andExpect(status().isUnauthorized());
    }
}
