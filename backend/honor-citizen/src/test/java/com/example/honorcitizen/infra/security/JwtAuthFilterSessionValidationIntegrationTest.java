package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RECOVERY-2 미검증 항목(f) — "Redis 장애 시 실제 HTTP 요청이 503 JSON을 반환하고 SecurityContext가
 * 안 만들어지는지"를 실제 필터 체인(JwtAuthFilter → HandlerExceptionResolver → GlobalExceptionHandler)을
 * MockMvc로 관통시켜 확인한다. TokenSessionStoreSessionValidationTest는 TokenSessionStore 단위에서
 * 예외가 올바르게 던져지는 것만 확인했을 뿐, 그 예외가 실제 HTTP 응답으로 어떻게 나가는지는
 * 검증하지 않았다 — 이 클래스가 그 간극을 메운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthFilterSessionValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private Long userId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(
                User.createOAuthUser("filter-redis-outage@example.com", "oauth-filter-redis-outage", "google", "필터통합"));
        userId = user.getId();
        accessToken = jwtTokenProvider.generateAccessToken(userId, UserRole.USER);

        // 세션 검증 자체가 Redis를 못 보게 만든다 — TokenSessionStoreSessionValidationTest와 동일한
        // 장애 재현 방식(StringRedisTemplate 전체를 Mock으로 교체).
        when(redisTemplate.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("down"));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void returnsServiceUnavailableJsonAndNeverReachesControllerWhenRedisIsDown() throws Exception {
        // 정상이라면 /api/users/me는 200과 본인 프로필을 반환한다 — 여기선 컨트롤러까지 아예 못
        // 들어가고 필터에서 바로 503 JSON으로 끊겨야 한다(성공 시 200이 나오는 것과 대비해, 이
        // 응답이 200이 아니라는 사실 자체가 필터가 체인을 중단시켰다는 증거다).
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_SESSION_VALIDATION_UNAVAILABLE"));
    }
}
