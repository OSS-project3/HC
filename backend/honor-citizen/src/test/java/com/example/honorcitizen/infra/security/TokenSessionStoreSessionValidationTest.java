package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// RECOVERY-2: access token 세션 검증(블랙리스트+revoked-after)이 Redis 장애 시 fail-open이 아니라
// fail-closed(AUTH_SESSION_VALIDATION_UNAVAILABLE)로 응답하는지 검증한다. StringRedisTemplate 전체를
// Mock으로 교체해 이 테스트 클래스 안에서만 장애 상황을 재현한다.
@SuppressWarnings("unchecked")
@SpringBootTest
class TokenSessionStoreSessionValidationTest {

    @Autowired
    private TokenSessionStore tokenSessionStore;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Test
    void throwsSessionValidationUnavailableWhenBlacklistLookupFails() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        String accessToken = jwtTokenProvider.generateAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> tokenSessionStore.isAccessTokenSessionValid(accessToken, 1L, System.currentTimeMillis()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_SESSION_VALIDATION_UNAVAILABLE);
    }

    @Test
    void throwsSessionValidationUnavailableWhenRevokedAfterLookupFails() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        String accessToken = jwtTokenProvider.generateAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> tokenSessionStore.isAccessTokenSessionValid(accessToken, 1L, System.currentTimeMillis()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_SESSION_VALIDATION_UNAVAILABLE);
    }

    @Test
    void allowsTokenWhenNoRevokedAfterKeyExistsForUser() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        String accessToken = jwtTokenProvider.generateAccessToken(1L, UserRole.USER);
        Long authIssuedAtMillis = jwtTokenProvider.getAuthIssuedAtMillis(accessToken);

        assertThat(tokenSessionStore.isAccessTokenSessionValid(accessToken, 1L, authIssuedAtMillis)).isTrue();
    }

    @Test
    void rejectsClaimlessTokenWhenRevokedAfterKeyExistsForUser() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(String.valueOf(System.currentTimeMillis()));
        String accessToken = jwtTokenProvider.generateAccessToken(1L, UserRole.USER);

        // revoked-after 키가 있는 사용자인데 토큰에 iam 클레임이 없다 = 재설정 이전 구버전 토큰.
        assertThat(tokenSessionStore.isAccessTokenSessionValid(accessToken, 1L, null)).isFalse();
    }

    @Test
    void rejectsTokenIssuedAtOrBeforeRevocationCutoff() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        long revokedAfterMillis = System.currentTimeMillis();
        when(valueOps.get(anyString())).thenReturn(String.valueOf(revokedAfterMillis));
        String accessToken = jwtTokenProvider.generateAccessToken(1L, UserRole.USER);

        assertThat(tokenSessionStore.isAccessTokenSessionValid(accessToken, 1L, revokedAfterMillis)).isFalse();
        assertThat(tokenSessionStore.isAccessTokenSessionValid(accessToken, 1L, revokedAfterMillis + 1)).isTrue();
    }
}
