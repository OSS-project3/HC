package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.EmailType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.dto.IdRecoveryConfirmResponse;
import com.example.honorcitizen.domain.user.dto.IdRecoveryResponse;
import com.example.honorcitizen.domain.user.dto.PasswordRecoveryResponse;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 계정 복구(아이디 찾기·비밀번호 재설정) 흐름만 조정한다 — 사용자 비밀번호를 직접 변경하거나 Redis
 * 명령을 직접 조립하지 않는다(arch.md §4.1 "계정 복구 책임 구조"). HMAC challenge와 원자 rate limit은
 * {@link VerificationChallengeStore}에, 실제 비밀번호 갱신·세션 무효화는 {@link UserService#resetPassword}에
 * 위임한다. Source of Truth: docs/api/auth.md API 7·8.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int TARGET_RATE_LIMIT = 5;
    private static final int IP_RATE_LIMIT = 20;
    private static final int MIN_NORMALIZED_PHONE_DIGITS = 9;
    private static final int MAX_NORMALIZED_PHONE_DIGITS = 15;

    private static final String ID_CHALLENGE_PREFIX = "auth:recovery:id:challenge:";
    private static final String ID_COOLDOWN_PREFIX = "auth:recovery:id:cooldown:";
    private static final String ID_PHONE_COUNT_PREFIX = "auth:recovery:id:count:phone:";

    private static final String PASSWORD_CHALLENGE_PREFIX = "auth:recovery:password:challenge:";
    private static final String PASSWORD_COOLDOWN_PREFIX = "auth:recovery:password:cooldown:";
    private static final String PASSWORD_EMAIL_COUNT_PREFIX = "auth:recovery:password:count:email:";

    // 아이디 찾기·비밀번호 재설정이 같은 IP 한도를 공유한다(한쪽 엔드포인트로 우회해 한도를
    // 두 배로 늘리지 못하게 — docs/api/auth.md API 7 ⑥).
    private static final String IP_COUNT_PREFIX = "auth:recovery:count:ip:";

    private final VerificationChallengeStore challengeStore;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final UserService userService;

    // ── 아이디(이메일) 찾기 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IdRecoveryResponse requestIdRecovery(String rawName, String rawPhone, String clientIp) {
        String name = rawName.trim();
        String normalizedPhone = User.normalizePhone(rawPhone);
        validateNormalizedPhoneDigits(normalizedPhone);

        String requestId = challengeStore.generateUrlSafeToken();
        claimRateLimitOrThrow(
                ID_COOLDOWN_PREFIX + challengeStore.sha256Hex(normalizedPhone),
                ID_PHONE_COUNT_PREFIX + challengeStore.sha256Hex(normalizedPhone),
                challengeStore.sha256Hex(clientIp));

        List<User> candidates = userRepository.findLocalAccountCandidatesByName(name);
        List<User> matched = candidates.stream()
                .filter(candidate -> normalizedPhone.equals(User.normalizePhone(candidate.getPhone())))
                .toList();

        // 정확히 1건일 때만 발송한다 — 0건·복수 건 모두 아래에서 아무 것도 하지 않고 같은 응답만 준다
        // (계정 존재 비노출, docs/api/auth.md API 7 ⑤-1).
        if (matched.size() == 1) {
            User user = matched.get(0);
            String challengeKey = ID_CHALLENGE_PREFIX + challengeStore.sha256Hex(requestId);
            VerificationChallengeStore.IssuedChallenge issued =
                    challengeStore.issue(challengeKey, CODE_TTL, user.getId(), requestId);
            try {
                emailSender.send(user.getEmail(), EmailType.ID_RECOVERY_VERIFICATION, "계정 찾기 확인 코드 안내",
                        buildIdRecoveryHtmlBody(issued.rawCode()), buildIdRecoveryTextBody(issued.rawCode()));
            } catch (CustomException e) {
                // 메일 실패로도 응답이 달라지면 "계정이 있다"는 신호가 새어나간다 — 조용히 롤백만 하고
                // 아래 공통 성공 응답을 그대로 반환한다(docs/api/auth.md API 7 ⑤-2).
                log.warn("계정 복구(아이디 찾기) 확인 메일 발송 실패 — 응답은 동일한 성공 처리", e);
                challengeStore.rollback(challengeKey, requestId);
            }
        }

        return IdRecoveryResponse.of(requestId, CODE_TTL.toSeconds(), RESEND_COOLDOWN.toSeconds());
    }

    @Transactional(readOnly = true)
    public IdRecoveryConfirmResponse confirmIdRecovery(String requestId, String rawCode) {
        String challengeKey = ID_CHALLENGE_PREFIX + challengeStore.sha256Hex(requestId);
        VerificationChallenge challenge = challengeStore.verifyAndConsume(challengeKey, rawCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_CODE));

        User user = userRepository.findById(challenge.userId())
                .filter(candidate -> candidate.getPasswordHash() != null)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_CODE));

        return IdRecoveryConfirmResponse.of(maskEmail(user.getEmail()));
    }

    // ── 비밀번호 재설정 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PasswordRecoveryResponse requestPasswordRecovery(String rawEmail, String clientIp) {
        String normalizedEmail = User.normalizeEmail(rawEmail);

        String requestId = challengeStore.generateUrlSafeToken();
        claimRateLimitOrThrow(
                PASSWORD_COOLDOWN_PREFIX + challengeStore.sha256Hex(normalizedEmail),
                PASSWORD_EMAIL_COUNT_PREFIX + challengeStore.sha256Hex(normalizedEmail),
                challengeStore.sha256Hex(clientIp));

        Optional<User> target = userRepository.findByEmail(normalizedEmail)
                .filter(user -> user.getPasswordHash() != null);

        if (target.isPresent()) {
            User user = target.get();
            String challengeKey = PASSWORD_CHALLENGE_PREFIX + challengeStore.sha256Hex(requestId);
            VerificationChallengeStore.IssuedChallenge issued =
                    challengeStore.issue(challengeKey, CODE_TTL, user.getId(), requestId);
            try {
                emailSender.send(user.getEmail(), EmailType.PASSWORD_RESET, "비밀번호 재설정 코드 안내",
                        buildPasswordResetHtmlBody(issued.rawCode()), buildPasswordResetTextBody(issued.rawCode()));
            } catch (CustomException e) {
                log.warn("계정 복구(비밀번호 재설정) 코드 메일 발송 실패 — 응답은 동일한 성공 처리", e);
                challengeStore.rollback(challengeKey, requestId);
            }
        }

        return PasswordRecoveryResponse.of(requestId, CODE_TTL.toSeconds(), RESEND_COOLDOWN.toSeconds());
    }

    public void confirmPasswordRecovery(String requestId, String rawCode, String newPassword) {
        String challengeKey = PASSWORD_CHALLENGE_PREFIX + challengeStore.sha256Hex(requestId);
        VerificationChallenge challenge = challengeStore.verifyAndConsume(challengeKey, rawCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_CODE));

        User user = userRepository.findById(challenge.userId())
                .filter(candidate -> candidate.getPasswordHash() != null)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_CODE));

        // 코드는 이미 소비됐다 — 아래가 실패해도 코드를 되살리지 않고 새 요청을 받게 한다
        // (docs/api/auth.md API 8 ⑤-1). 비밀번호 저장+전체 세션 무효화는 UserService.resetPassword의
        // 하나의 업무 단위(@Transactional)이며, 세션 무효화 실패 시 비밀번호 변경도 함께 롤백된다.
        userService.resetPassword(user.getId(), newPassword);

        try {
            emailSender.send(user.getEmail(), EmailType.PASSWORD_CHANGED, "비밀번호가 변경되었습니다",
                    buildPasswordChangedHtmlBody(), buildPasswordChangedTextBody());
        } catch (CustomException e) {
            // DB commit 이후 best-effort 알림 — 발송 실패가 이미 완료된 비밀번호 변경을 되돌리지 않는다.
            log.warn("비밀번호 변경 알림 메일 발송 실패(best effort, 무시)", e);
        }
    }

    // ── 공용 ────────────────────────────────────────────────────────────

    private void claimRateLimitOrThrow(String cooldownKey, String targetCountKey, String ipHash) {
        boolean allowed = challengeStore.claimRateLimit(
                cooldownKey, RESEND_COOLDOWN,
                targetCountKey, TARGET_RATE_LIMIT, RATE_LIMIT_WINDOW,
                IP_COUNT_PREFIX + ipHash, IP_RATE_LIMIT, RATE_LIMIT_WINDOW);
        if (!allowed) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private void validateNormalizedPhoneDigits(String normalizedPhone) {
        String digitsOnly = normalizedPhone.startsWith("+") ? normalizedPhone.substring(1) : normalizedPhone;
        if (digitsOnly.length() < MIN_NORMALIZED_PHONE_DIGITS || digitsOnly.length() > MAX_NORMALIZED_PHONE_DIGITS) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // 로컬파트가 3자 이상이면 앞 2자, 2자면 앞 1자, 1자면 전부 가린다. 도메인은 유지한다
    // (docs/api/auth.md API 7 ⑤).
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        int visibleLength = Math.max(0, Math.min(local.length() - 1, 2));
        String visible = local.substring(0, visibleLength);
        return visible + "***" + domain;
    }

    private String buildIdRecoveryTextBody(String code) {
        return """
                계정 찾기 확인 코드: %s
                유효 시간: 10분

                본인이 요청하지 않았다면 이 이메일을 무시하세요.
                """.formatted(code);
    }

    private String buildIdRecoveryHtmlBody(String code) {
        return """
                <p>계정 찾기 확인 코드: <b>%s</b></p>
                <p>유효 시간: 10분</p>
                <p>본인이 요청하지 않았다면 이 이메일을 무시하세요.</p>
                """.formatted(code);
    }

    private String buildPasswordResetTextBody(String code) {
        return """
                비밀번호 재설정 코드: %s
                유효 시간: 10분

                본인이 요청하지 않았다면 이 이메일을 무시하세요.
                """.formatted(code);
    }

    private String buildPasswordResetHtmlBody(String code) {
        return """
                <p>비밀번호 재설정 코드: <b>%s</b></p>
                <p>유효 시간: 10분</p>
                <p>본인이 요청하지 않았다면 이 이메일을 무시하세요.</p>
                """.formatted(code);
    }

    private String buildPasswordChangedTextBody() {
        return """
                비밀번호가 변경되었습니다.

                본인이 요청하지 않았다면 즉시 비밀번호를 다시 재설정하고 고객지원에 문의하세요.
                """;
    }

    private String buildPasswordChangedHtmlBody() {
        return """
                <p>비밀번호가 변경되었습니다.</p>
                <p>본인이 요청하지 않았다면 즉시 비밀번호를 다시 재설정하고 고객지원에 문의하세요.</p>
                """;
    }
}
