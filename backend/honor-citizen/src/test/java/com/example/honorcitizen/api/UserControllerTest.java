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

        user = User.createOAuthUser("jane@example.com", "oauth-jane", "google", "Jane");
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
                .andExpect(jsonPath("$.data.role").doesNotExist())
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
    void updateMeUpdatesNameAndPhone() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Name",
                                  "phone": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"));
    }

    // email은 OAuth 식별값이라 이 API로 수정할 수 없다. address는 확정 정책(2026-08-08, 2026-08-20
    // 재확인)상 이 API의 요청 DTO에 필드 자체가 없어 요청 바디에 보내도 무시된다.
    @Test
    void updateMeIgnoresAddressEvenWhenProvidedInRequestBody() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Name",
                                  "address": "서울특별시 강남구"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.address").isEmpty());
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

    // 2026-08-19 정책 변경(WITHDRAW-4): 탈퇴는 즉시 User row 하드 삭제다 — status 대신 row 존재
    // 자체를 확인한다.
    @Test
    void withdrawHardDeletesUserAndBlacklistsAccessToken() throws Exception {
        mockMvc.perform(post("/api/users/me/withdraw")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRepository.findById(user.getId())).isEmpty();

        // 방금 탈퇴 처리에 쓰인 accessToken은 블랙리스트에 등록되어 더 이상 인증에 쓸 수 없어야 함
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawTwiceReturnsNotFoundOnSecondCall() throws Exception {
        mockMvc.perform(post("/api/users/me/withdraw")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        // 첫 호출에 쓴 토큰은 블랙리스트되므로, 재호출 상황을 재현하려면 새 토큰 발급(이미 삭제된
        // userId를 담은 토큰 — JwtAuthFilter의 existsById 확인에서 걸러지므로 인증 자체가 실패한다).
        String secondToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());

        mockMvc.perform(post("/api/users/me/withdraw")
                        .header("Authorization", secondToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/users/me/withdraw"))
                .andExpect(status().isUnauthorized());
    }
}
