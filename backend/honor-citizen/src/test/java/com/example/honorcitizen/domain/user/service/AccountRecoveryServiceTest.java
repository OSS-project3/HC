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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    // ── RECOVERY-3 미검증 항목 보강 ──────────────────────────────────────

    // (a) 코드 만료(TTL 경과) confirm 시나리오. verify-and-increment-code.lua는 GET이 false(만료·
    // 미존재 둘 다 동일)일 때 빈 문자열을 반환한다(L6-9) — challenge key를 직접 지우는 것이 실제
    // 10분 TTL이 흐른 뒤와 완전히 같은 코드 경로를 태운다.
    @Test
    void confirmIdRecoveryFailsAfterChallengeKeyExpires() {
        saveLocalUser("id-ttl-expired@example.com", "박태티엘", "010-2222-3333");
        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("박태티엘", "010-2222-3333", CLIENT_IP);
        captureCode();
        expireChallengeKey("auth:recovery:id:challenge:*");

        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), "482193"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void confirmPasswordRecoveryFailsAfterChallengeKeyExpires() {
        saveLocalUser("pw-ttl-expired@example.com", "홍길동", "010-1234-5678");
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("pw-ttl-expired@example.com", CLIENT_IP);
        captureCode();
        expireChallengeKey("auth:recovery:password:challenge:*");

        assertThatThrownBy(() -> accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), "482193", "whatever-1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    private void expireChallengeKey(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        assertThat(keys).hasSize(1);
        redisTemplate.delete(keys);
    }

    // (b) confirm 시점에 대상 계정이 삭제되거나 OAuth 전용으로 전환된 케이스. challenge는
    // userId에 결속되므로, 요청 시점엔 유효했던 계정이 확인 시점엔 조건을 더 이상 만족하지
    // 않으면(row 자체가 없거나 passwordHash == null) 다른 코드 오류와 동일하게 거절해야 한다
    // (docs/api/auth.md API 7 ⑤-2 · API 8 ⑤).
    @Test
    void confirmIdRecoveryFailsWhenAccountDeletedAfterRequest() {
        User user = saveLocalUser("id-deleted@example.com", "삭제됨", "010-6666-7777");
        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("삭제됨", "010-6666-7777", CLIENT_IP);
        String code = captureCode();

        userRepository.delete(user);
        userRepository.flush();

        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void confirmIdRecoveryFailsWhenAccountConvertedToOAuthOnlyAfterRequest() {
        User user = saveLocalUser("id-converted@example.com", "전환됨", "010-8888-9999");
        IdRecoveryResponse response = accountRecoveryService.requestIdRecovery("전환됨", "010-8888-9999", CLIENT_IP);
        String code = captureCode();

        // 이 프로젝트에 로컬→OAuth 전환 API는 아직 없다 — passwordHash를 지워 "일반 계정 조건을
        // 더 이상 만족하지 않는 상태"만 흉내낸다(다른 세션 테스트의 ReflectionTestUtils 패턴과 동일).
        ReflectionTestUtils.setField(user, "passwordHash", null);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> accountRecoveryService.confirmIdRecovery(response.getRequestId(), code))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void confirmPasswordRecoveryFailsWhenAccountDeletedAfterRequest() {
        User user = saveLocalUser("pw-deleted@example.com", "삭제됨", "010-1111-3333");
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("pw-deleted@example.com", CLIENT_IP);
        String code = captureCode();

        userRepository.delete(user);
        userRepository.flush();

        assertThatThrownBy(() -> accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), code, "whatever-1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void confirmPasswordRecoveryFailsWhenAccountConvertedToOAuthOnlyAfterRequest() {
        User user = saveLocalUser("pw-converted@example.com", "전환됨", "010-4444-6666");
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("pw-converted@example.com", CLIENT_IP);
        String code = captureCode();

        ReflectionTestUtils.setField(user, "passwordHash", null);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), code, "whatever-1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_CODE);
    }

    // (c) "재설정과 새 로그인 토큰이 같은 초에 생성돼도 신규 토큰은 허용"(docs/api/auth.md API 8 —
    // 표준 iat가 아니라 ms 단위 authIssuedAtMillis로 비교하는 이유). 정확히 같은 밀리초로 못박는
    // 결정론적 경계값은 TokenSessionStoreSessionValidationTest.rejectsTokenIssuedAtOrBeforeRevocationCutoff가
    // 이미 단위 테스트로 고정하고 있다 — 여기서는 실제 재설정→즉시 재로그인 흐름 전체가 그 경계를
    // 넘어 정상 동작하는지 end-to-end로 확인한다.
    @Test
    void loginImmediatelyAfterPasswordResetIssuesTokenThatPassesSessionValidation() {
        User user = saveLocalUser("same-second@example.com", "홍길동", "010-1234-5678");
        PasswordRecoveryResponse response = accountRecoveryService.requestPasswordRecovery("same-second@example.com", CLIENT_IP);
        String code = captureCode();

        accountRecoveryService.confirmPasswordRecovery(response.getRequestId(), code, "brand-new-password-2");

        LoginResult freshLogin = userService.login("same-second@example.com", "brand-new-password-2");
        String newAccessToken = freshLogin.tokens().accessToken();
        Long authIssuedAtMillis = extractAuthIssuedAtMillis(newAccessToken);

        assertThat(tokenSessionStore.isAccessTokenSessionValid(newAccessToken, user.getId(), authIssuedAtMillis)).isTrue();
    }

    // (e) 동시 코드 요청의 cooldown 우회 방지. claim-recovery-rate-limit.lua는 EXISTS 확인과 SET을
    // 하나의 원자 스크립트로 묶으므로(L8-9, L28-29), 같은 대상(name+phone)으로 동시에 여러 요청이
    // 들어와도 정확히 1건만 쿨다운을 선점해야 한다.
    @Test
    void concurrentIdRecoveryRequestsForSameTargetOnlyAllowExactlyOneToClaimTheCooldown() throws Exception {
        saveLocalUser("concurrent@example.com", "동시성", "010-4444-5555");
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rateLimitedCount = new AtomicInteger();

        try {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        accountRecoveryService.requestIdRecovery("동시성", "010-4444-5555", CLIENT_IP);
                        successCount.incrementAndGet();
                    } catch (CustomException e) {
                        if (e.getErrorCode() != ErrorCode.TOO_MANY_REQUESTS) {
                            throw e;
                        }
                        rateLimitedCount.incrementAndGet();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rateLimitedCount.get()).isEqualTo(threadCount - 1);
    }

    private Long extractAuthIssuedAtMillis(String accessToken) {
        return jwtTokenProvider.getAuthIssuedAtMillis(accessToken);
    }
}
