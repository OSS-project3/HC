package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.UserStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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

        user = User.createNewUser("jane@example.com", "oauth-jane", "google", "Jane");
        user = userRepository.save(user);
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
    }

    @Test
    void getMeReturnsCurrentUserInfo() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.name").value("Jane"))
                .andExpect(jsonPath("$.data.email").value("jane@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.phone").isEmpty())
                .andExpect(jsonPath("$.data.address").isEmpty());
    }

    @Test
    void getMeReturnsUnauthorizedWithoutToken() throws Exception {
        // 토큰 자체가 없으면 Spring Security가 컨트롤러/GlobalExceptionHandler 이전에 막아 빈 바디로 401 반환함
        // (JSON 에러 envelope은 CustomException을 던지는 케이스에만 적용됨 — 실제 curl 테스트로 확인된 동작과 동일)
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMeUpdatesPhoneAndAddress() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "010-1234-5678",
                                  "address": "서울특별시 강남구"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Jane"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.address").value("서울특별시 강남구"));
    }

    @Test
    void updateMeReturnsInvalidInputWhenAllFieldsMissing() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void updateMeReturnsInvalidInputWhenNameIsBlank() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void updateMeReturnsInvalidInputWhenPhoneFormatIsWrong() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "not-a-phone!" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void updateMeReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "New Name" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawMarksUserWithdrawnAndBlacklistsAccessToken() throws Exception {
        mockMvc.perform(post("/api/users/me/withdraw")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User withdrawn = userRepository.findById(user.getId()).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(withdrawn.getWithdrawalRequestedAt()).isNotNull();

        // 방금 탈퇴 처리에 쓰인 accessToken은 블랙리스트에 등록되어 더 이상 인증에 쓸 수 없어야 함
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawReturnsAlreadyWithdrawnOnSecondCall() throws Exception {
        mockMvc.perform(post("/api/users/me/withdraw")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        // 첫 호출에 쓴 토큰은 블랙리스트되므로, 이미 탈퇴된 상태에서 재호출하는 상황을 재현하려면 새 토큰 발급
        String secondToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());

        mockMvc.perform(post("/api/users/me/withdraw")
                        .header("Authorization", secondToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ALREADY_WITHDRAWN"));
    }

    @Test
    void withdrawReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/users/me/withdraw"))
                .andExpect(status().isUnauthorized());
    }
}
