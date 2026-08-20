package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.dto.IdRecoveryConfirmResponse;
import com.example.honorcitizen.domain.user.dto.IdRecoveryResponse;
import com.example.honorcitizen.domain.user.dto.PasswordRecoveryResponse;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.RefreshTokenSessionRepository;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.domain.user.entity.RefreshTokenStatus;
import com.example.honorcitizen.infra.mail.EmailSender;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.security.TokenSessionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// EmailVerificationServiceTest와 동일한 이유(Redis 상태는 트랜잭션 롤백 대상 아님)로 @Transactional을
// 걸지 않고 매 테스트 종료 후 User row와 사용한 Redis 키를 직접 정리한다.
@SpringBootTest
class AccountRecoveryServiceTest {

    @Autowired
    private AccountRecoveryService accountRecoveryService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Autowired
    private TokenSessionStore tokenSessionStore;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private EmailSender emailSender;

    private static final String CLIENT_IP = "127.0.0.1";
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        flushRedisAuthKeys();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        flushRedisAuthKeys();
    }

    private void flushRedisAuthKeys() {
        deleteKeysMatching("auth:recovery:*");
        deleteKeysMatching("auth:access:user-revoked-after:*");
    }

    private void deleteKeysMatching(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private User saveLocalUser(String email, String name, String phone) {
        return userRepository.saveAndFlush(
                User.createLocalUser(email, passwordEncoder.encode("correct-horse-battery"), name, phone));
    }

    private String captureCode() {
        var textBodyCaptor = forClass(String.class);
        verify(emailSender).send(anyString(), any(), anyString(), anyString(), textBodyCaptor.capture());
        Matcher matcher = CODE_PATTERN.matcher(textBodyCaptor.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    // ── 아이디(이메일) 찾기 ──────────────────────────────────────────────

    @Test
    void requestAndConfirmIdRecoverySucceedsForExactlyOneLocalMatch() {
        User user = saveLocalUser("id-recovery-one@example.com", "홍길동", "010-1234-5678");

        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("홍길동", "010-1234-5678", CLIENT_IP);
        assertThat(response.getRequestId()).isNotBlank();
        assertThat(response.getExpiresInSeconds()).isEqualTo(600L);

        String code = captureCode();
        IdRecoveryConfirmResponse confirmed = accountRecoveryService.confirmIdRecovery(response.getRequestId(), code);

        assertThat(confirmed.getMaskedEmail()).isEqualTo("id***@example.com");
        assertThat(user.getId()).isNotNull();
    }

    @Test
    void requestIdRecoveryNormalizesInternationalPhoneFormatForMatching() {
        saveLocalUser("intl-phone@example.com", "김철수", "+82 10-9876-5432");

        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("김철수", "+821098765432", CLIENT_IP);
        String code = captureCode();

        IdRecoveryConfirmResponse confirmed = accountRecoveryService.confirmIdRecovery(response.getRequestId(), code);
        assertThat(confirmed.getMaskedEmail()).isEqualTo("in***@example.com");
    }

    @Test
    void requestIdRecoverySendsNoEmailAndConfirmFailsWhenNoAccountMatches() {
        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("존재안함", "010-0000-0000", CLIENT_IP);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        assertThat(response.getRequestId()).isNotBlank();

        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), "123456"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void requestIdRecoverySendsNoEmailWhenMultipleAccountsShareNameAndPhone() {
        saveLocalUser("dup-one@example.com", "이영희", "010-5555-5555");
        saveLocalUser("dup-two@example.com", "이영희", "010-5555-5555");

        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("이영희", "010-5555-5555", CLIENT_IP);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), "123456"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void requestIdRecoveryExcludesOAuthOnlyAccountEvenOnNameAndPhoneMatch() {
        User oauthUser = User.createOAuthUser("oauth-only@example.com", "sub-1", "google", "박민수");
        oauthUser.updateProfile(null, "010-7777-7777");
        userRepository.saveAndFlush(oauthUser);

        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("박민수", "010-7777-7777", CLIENT_IP);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), "123456"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void requestIdRecoveryRejectsPhoneThatNormalizesToTooFewDigits() {
        assertThatThrownBy(() -> accountRecoveryService.requestIdRecovery("홍길동", "123456", CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void confirmIdRecoveryRejectsWrongCodeButKeepsChallengeForRetry() {
        saveLocalUser("id-wrong-code@example.com", "정다은", "010-1111-2222");
        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("정다은", "010-1111-2222", CLIENT_IP);
        String correctCode = captureCode();
        String wrongCode = "000000".equals(correctCode) ? "111111" : "000000";

        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), wrongCode))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);

        // 챌린지가 아직 살아있어 정답 코드로는 여전히 통과해야 한다.
        IdRecoveryConfirmResponse confirmed = accountRecoveryService.confirmIdRecovery(response.getRequestId(), correctCode);
        assertThat(confirmed.getMaskedEmail()).isNotBlank();
    }

    @Test
    void confirmIdRecoveryRejectsReuseAfterSuccess() {
        saveLocalUser("id-reuse@example.com", "최수진", "010-3333-4444");
        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("최수진", "010-3333-4444", CLIENT_IP);
        String code = captureCode();

        accountRecoveryService.confirmIdRecovery(response.getRequestId(), code);

        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void requestIdRecoveryRejectsWithinResendCooldown() {
        accountRecoveryService.requestIdRecovery("쿨다운", "010-9999-0000", CLIENT_IP);

        assertThatThrownBy(() -> accountRecoveryService.requestIdRecovery("쿨다운", "010-9999-0000", CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void requestIdRecoveryAndRequestPasswordRecoveryShareTheSameIpRateLimit() {
        String ip = "10.9.9.9";
        // ID 찾기로 IP 한도(20/시간)를 다 채운다 — 이름/전화는 매번 바꿔 쿨다운에 안 걸리게 한다.
        for (int i = 0; i < 20; i++) {
            accountRecoveryService.requestIdRecovery("사용자" + i, "010-1000-" + String.format("%04d", i), ip);
        }

        assertThatThrownBy(() -> accountRecoveryService.requestPasswordRecovery("shared-ip@example.com", ip))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    // ── 비밀번호 재설정 ──────────────────────────────────────────────────

    @Test
    void requestAndConfirmPasswordRecoverySucceedsAndInvalidatesExistingSessions() {
        User user = saveLocalUser("pw-reset@example.com", "홍길동", "010-1234-5678");
        LoginResult loginResult = userService.login("pw-reset@example.com", "correct-horse-battery");
        String oldAccessToken = loginResult.tokens().accessToken();
        assertThat(refreshTokenSessionRepository.findByUserIdAndStatus(user.getId(),
                RefreshTokenStatus.ACTIVE)).isNotEmpty();

        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("pw-reset@example.com", CLIENT_IP);
        String code = captureCode();

        accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), code, "brand-new-password-1");

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-password-1", reloaded.getPasswordHash())).isTrue();
        assertThat(refreshTokenSessionRepository.findByUserIdAndStatus(user.getId(),
                RefreshTokenStatus.ACTIVE)).isEmpty();

        // 재설정 전에 발급된 access token은 authIssuedAtMillis가 revokedAfter보다 이르므로 거절돼야 한다.
        Long authIssuedAtMillis = extractAuthIssuedAtMillis(oldAccessToken);
        assertThat(tokenSessionStore.isAccessTokenSessionValid(oldAccessToken, user.getId(), authIssuedAtMillis)).isFalse();
    }

    @Test
    void requestPasswordRecoveryAllowsNewPasswordSameAsOldPassword() {
        User user = saveLocalUser("pw-same@example.com", "홍길동", "010-1234-5678");
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("pw-same@example.com", CLIENT_IP);
        String code = captureCode();

        accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), code, "correct-horse-battery");

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("correct-horse-battery", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void requestPasswordRecoverySendsNoEmailForOAuthOnlyAccount() {
        userRepository.saveAndFlush(User.createOAuthUser("oauth-pw@example.com", "sub-2", "google", "박민수"));

        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("oauth-pw@example.com", CLIENT_IP);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        assertThatThrownBy(() -> accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), "123456", "whatever-1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void requestPasswordRecoverySendsNoEmailForUnknownEmail() {
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("never-signed-up@example.com", CLIENT_IP);

        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
        assertThat(response.getRequestId()).isNotBlank();
    }

    @Test
    void confirmPasswordRecoveryRejectsWrongCode() {
        saveLocalUser("pw-wrong@example.com", "홍길동", "010-1234-5678");
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("pw-wrong@example.com", CLIENT_IP);
        String correctCode = captureCode();
        String wrongCode = "000000".equals(correctCode) ? "111111" : "000000";

        assertThatThrownBy(() -> accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), wrongCode, "whatever-1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void requestPasswordRecoveryRejectsWithinResendCooldown() {
        saveLocalUser("pw-cooldown@example.com", "홍길동", "010-1234-5678");
        accountRecoveryService.requestPasswordRecovery("pw-cooldown@example.com", CLIENT_IP);

        assertThatThrownBy(() -> accountRecoveryService.requestPasswordRecovery("pw-cooldown@example.com", CLIENT_IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void changePasswordAlsoInvalidatesOtherDeviceAccessTokensViaRevokedAfter() {
        User user = saveLocalUser("pw-change@example.com", "홍길동", "010-1234-5678");
        LoginResult firstDevice = userService.login("pw-change@example.com", "correct-horse-battery");
        LoginResult secondDevice = userService.login("pw-change@example.com", "correct-horse-battery");

        userService.changePassword(user.getId(), secondDevice.tokens().accessToken(),
                "correct-horse-battery", "changed-password-1");

        // 요청에 쓰인 두 번째 기기 토큰뿐 아니라, blacklist되지 않은 첫 번째 기기 토큰도 revoked-after로 거절돼야 한다.
        Long authIssuedAtMillis = extractAuthIssuedAtMillis(firstDevice.tokens().accessToken());
        assertThat(tokenSessionStore.isAccessTokenSessionValid(
                firstDevice.tokens().accessToken(), user.getId(), authIssuedAtMillis)).isFalse();
    }

    private Long extractAuthIssuedAtMillis(String accessToken) {
        return jwtTokenProvider.getAuthIssuedAtMillis(accessToken);
    }
}
