package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.application.service.ApplicationDailyLimitService;
import com.example.honorcitizen.domain.user.dto.TermsAgreeRequest;
import com.example.honorcitizen.domain.user.dto.TermsAgreeResponse;
import com.example.honorcitizen.domain.user.dto.UserMeResponse;
import com.example.honorcitizen.domain.user.dto.UserUpdateRequest;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.AuthTokens;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.security.LoginAttemptLimiter;
import com.example.honorcitizen.infra.security.TokenSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenSessionStore tokenSessionStore;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final ApplicationDailyLimitService applicationDailyLimitService;

    public TermsAgreeResponse agreeTerms(Long userId, TermsAgreeRequest request) {
        User user = findById(userId);

        if (user.isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_ALREADY_AGREED);
        }

        user.agreeTerms(request.getPrivacyAgreed(), request.getImageUploadAgreed(), request.getShippingAgreed());

        return TermsAgreeResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        return UserMeResponse.from(findById(userId));
    }

    // AUTH-3. OAuth 계정도 email UNIQUE 제약을 공유하므로 존재 여부만으로 provider 구분 없이 중복 판정된다.
    // 계정 상세는 노출하지 않고 존재 여부(boolean)만 반환한다.
    @Transactional(readOnly = true)
    public boolean checkEmailExists(String rawEmail) {
        return userRepository.existsByEmail(User.normalizeEmail(rawEmail));
    }

    public UserMeResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = findById(userId);

        if (request.getName() == null && request.getPhone() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.getName() != null && request.getName().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        user.updateProfile(request.getName(), request.getPhone());
        return UserMeResponse.from(user);
    }

    /**
     * AUTH-6. OAuth 전용 계정(passwordHash==null)은 차단, 현재 비밀번호 불일치도 거절.
     * 성공 시 다른 기기의 세션까지 포함해 전체 세션을 무효화한다(계정 탈취 가능성에 대한 방어 —
     * withdraw()와 동일한 패턴, 2026-08-19 사용자 확인 완료). 이 요청 자체를 보낸 현재 세션의
     * accessToken을 즉시 블랙리스트하는 것과 별개로, 다른 기기에 남아있는 기존 access token도
     * 전부 거절되도록 사용자 단위 revoke primitive(recordUserAccessRevocation)를 함께 기록한다
     * (RECOVERY-2 — blacklist 하나만으로는 "요청에 쓰인 토큰 1개"만 막혀 전체 세션 무효화 계약을
     * 만족하지 못했다). 프론트는 비밀번호 변경 성공 후 재로그인을 유도해야 한다.
     */
    public void changePassword(Long userId, String accessToken, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (user.getPasswordHash() == null) {
            throw new CustomException(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        user.updateRefreshToken(null);
        tokenSessionStore.invalidateUserSessions(userId);
        tokenSessionStore.blacklistAccessToken(accessToken);
        tokenSessionStore.recordUserAccessRevocation(userId);
        log.info("보안 이벤트: 비밀번호 변경 및 전체 세션 무효화 userId={}", userId);
    }

    /**
     * 계정 복구(비밀번호 재설정) 전용 — RECOVERY-2. 로그인 상태가 아니므로 현재 비밀번호 확인이나
     * 특정 accessToken 블랙리스트는 없다: 비밀번호 저장과 전체 세션(refresh 전부 + 기존 access token
     * 전부) 무효화를 하나의 업무 단위로 처리한다. 세션 무효화 Redis 작업이 실패하면(RuntimeException)
     * 이 메서드의 @Transactional이 비밀번호 변경까지 함께 롤백한다 — 재설정을 "성공"으로 확정하지
     * 않는다. 호출자(AccountRecoveryService)가 challenge 검증·대상 계정 재조회를 먼저 마친 뒤
     * 호출한다는 전제다.
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        user.updateRefreshToken(null);
        tokenSessionStore.invalidateUserSessions(userId);
        tokenSessionStore.recordUserAccessRevocation(userId);
        log.info("보안 이벤트: 비밀번호 재설정(계정 복구) 및 전체 세션 무효화 userId={}", userId);
    }

    /**
     * OAuth 콜백에서 신규 계정을 생성한다. 이메일 UNIQUE 제약 도입 이후 같은 이메일의 다른 계정
     * (다른 provider 또는 일반 이메일 계정)이 이미 있으면 자동으로 연결하지 않고 거절한다.
     *
     * REQUIRES_NEW인 이유: 호출자(OAuth2SuccessHandler#onAuthenticationSuccess)가 이미 트랜잭션
     * 안에 있는데, 여기서 이메일 중복으로 실패하면 그 실패가 호출자의 트랜잭션 전체를 rollback-only로
     * 오염시킨다(REQUIRED로 참여할 경우). 별도 트랜잭션으로 분리해 이 메서드의 실패가 호출자의 나머지
     * 로직(기존 사용자 로그인 처리 등)에 영향을 주지 않도록 격리한다.
     * 반환된 User는 이 메서드의 트랜잭션이 이미 커밋·종료된 뒤라 detached 상태이므로, 호출자가 이후
     * 이 엔티티를 변경(예: updateRefreshToken)해야 한다면 자신의 트랜잭션에서 다시 조회해 managed
     * 상태로 만들어야 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createOAuthUserIfAbsent(String email, String oauthId, String oauthProvider, String name) {
        String normalizedEmail = User.normalizeEmail(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        try {
            return userRepository.save(User.createOAuthUser(email, oauthId, oauthProvider, name));
        } catch (DataIntegrityViolationException e) {
            // 사전조회 이후 동시요청으로 같은 이메일 계정이 먼저 커밋된 경우 — email UNIQUE 위반을 동일하게 처리한다.
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * AUTH-4 3~5·7단계. signupToken 검증(1~2단계)은 이 메서드 호출 전에 {@code EmailVerificationService}가
     * 이미 끝냈다고 가정한다(호출자가 검증된 normalizedEmail을 넘긴다).
     *
     * 이 메서드는 클래스 레벨 {@code @Transactional}을 그대로 물려받아 User 저장과 리프레시 토큰
     * 갱신이 하나의 트랜잭션으로 commit된다 — 호출자(AuthController)는 이 메서드가 반환된 뒤(=DB
     * commit이 실제로 끝난 뒤)에만 Redis 가입 토큰을 삭제해야 한다(6단계, 순서를 이 메서드가 강제하지
     * 않으므로 호출자가 지켜야 함).
     */
    public LocalSignupResult registerLocalUser(String normalizedEmail, String rawPassword, String name, String phone) {
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = User.createLocalUser(normalizedEmail, passwordHash, name, phone);
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 사전조회 이후 동시요청으로 같은 이메일 계정이 먼저 커밋된 경우 — email UNIQUE 위반을 동일하게 처리한다.
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        AuthTokens tokens = issueLoginTokens(user);
        return new LocalSignupResult(user, tokens);
    }

    /**
     * AUTH-5. 정규화 → 잠금 확인 → 자격 검증 → 토큰 발급 순서.
     * 계정없음/비밀번호불일치/OAuth전용계정은 전부 동일한 {@code INVALID_CREDENTIALS}로 응답하고
     * 실패 카운터를 늘린다 — 이메일 존재 여부가 응답 차이로 새어나가지 않게 하기 위함이다.
     * 계정 존재 여부와 무관하게 이 메서드가 실패 케이스마다 {@code recordFailure}를 호출하므로,
     * {@code LoginAttemptLimiter}의 잠금 카운트 자체도 이메일 존재 여부를 노출하지 않는다.
     * 2026-08-19 정책 변경: 탈퇴는 즉시 하드 삭제이므로 별도 탈퇴 상태 체크가 필요 없다 —
     * `findByEmail`이 찾아낸 계정은 그 자체로 탈퇴하지 않은 계정이다(`docs/collab/user.md` §2.1).
     */
    public LoginResult login(String rawEmail, String rawPassword) {
        String normalizedEmail = User.normalizeEmail(rawEmail);
        loginAttemptLimiter.checkNotLocked(normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(normalizedEmail);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        loginAttemptLimiter.reset(normalizedEmail);
        AuthTokens tokens = issueLoginTokens(user);
        return new LoginResult(user, tokens);
    }

    public AuthTokens issueLoginTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenSessionStore.createRefreshToken(user);
        user.updateRefreshToken(refreshToken);
        return new AuthTokens(accessToken, refreshToken);
    }

    public AuthTokens refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = findById(jwtTokenProvider.getUserId(refreshToken));
        String newRefreshToken = tokenSessionStore.rotateRefreshToken(user, refreshToken);
        user.updateRefreshToken(newRefreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        return new AuthTokens(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId, String accessToken) {
        tokenSessionStore.invalidateUserSessions(userId);
        tokenSessionStore.blacklistAccessToken(accessToken);
        findById(userId).updateRefreshToken(null);
        log.info("보안 이벤트: 로그아웃 userId={}", userId);
    }

    // 2026-08-19 정책 변경: 탈퇴는 즉시 하드 삭제다 — 유예기간·복구 없음(`docs/collab/user.md` §2).
    // 순서: 세션 무효화 → 액세스 토큰 블랙리스트 → RefreshTokenSession 하드 삭제 →
    // ApplicationDailyLimit 하드 삭제(다른 모듈 Repository를 직접 쓰지 않고 공개 Service 메서드를
    // 거침, arch.md §5.1) → User row 하드 삭제. findById가 성공했다는 것 자체가 "아직 탈퇴하지 않은
    // 계정"이라는 뜻이므로 별도 ALREADY_WITHDRAWN 재확인은 없다 — 재호출 시 row가 이미 없어
    // USER_NOT_FOUND로 자연히 실패한다.
    public void withdraw(Long userId, String accessToken) {
        User user = findById(userId);
        tokenSessionStore.invalidateUserSessions(userId);
        tokenSessionStore.blacklistAccessToken(accessToken);
        tokenSessionStore.deleteUserSessions(userId);
        applicationDailyLimitService.deleteAllForUser(userId);
        userRepository.delete(user);
        log.info("보안 이벤트: 회원탈퇴(계정 삭제) userId={}", userId);
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User findEligibleApplicationUser(Long userId) {
        // 2026-08-19 정책 변경: 탈퇴 계정은 즉시 하드 삭제되므로 findById가 성공했다는 것 자체가
        // "탈퇴하지 않은 계정"이라는 뜻이다 — 별도 상태 체크가 필요 없다.
        User user = findById(userId);
        if (user.getRole() != UserRole.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!user.isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public void validateTermsAgreed(Long userId) {
        if (!findById(userId).isAllTermsAgreed()) {
            throw new CustomException(ErrorCode.TERMS_NOT_AGREED);
        }
    }

}
