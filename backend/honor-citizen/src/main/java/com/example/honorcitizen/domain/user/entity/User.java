package com.example.honorcitizen.domain.user.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"oauth_id", "oauth_provider"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // 일반 이메일 계정은 OAuth 정보가 없는 게 도메인상 정확한 상태라 null 허용(2026-08-19 확정).
    // 두 컬럼은 항상 함께 null이거나 함께 값이 있어야 하며, 이 불변조건은 createLocalUser/createOAuthUser가 보장한다.
    private String oauthId;

    private String oauthProvider;

    // 일반 이메일 계정만 값이 있다(OAuth 계정은 null). 평문은 저장하지 않고 해시만 저장한다.
    @Column(length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean termsAgreed;

    @Column(nullable = false)
    private boolean privacyAgreed;

    @Column(nullable = false)
    private boolean imageUploadAgreed;

    @Column(nullable = false)
    private boolean shippingAgreed;

    private LocalDateTime termsAgreedAt;

    @Column(length = 2048)
    private String refreshToken;

    private String phone;

    private String address;

    // 로그인 아이디로 쓰이는 모든 이메일은 저장 전 이 메서드로 정규화한다(trim+소문자) —
    // 일반 가입/로그인/중복확인/비밀번호 재설정/아이디찾기/OAuth 콜백 전부 동일 규칙을 써야
    // "같은 이메일인데 대소문자·공백 차이로 다른 계정" 문제가 안 생긴다.
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static User createOAuthUser(String email, String oauthId, String oauthProvider, String name) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.oauthId = oauthId;
        user.oauthProvider = oauthProvider;
        user.name = name;
        user.role = UserRole.USER;
        user.termsAgreed = false;
        user.privacyAgreed = false;
        user.imageUploadAgreed = false;
        user.shippingAgreed = false;
        return user;
    }

    // 일반 이메일 회원가입 전용 — oauthId/oauthProvider는 항상 null(약관 동의는 /terms에서 별도 처리).
    public static User createLocalUser(String email, String passwordHash, String name) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.name = name;
        user.role = UserRole.USER;
        user.termsAgreed = false;
        user.privacyAgreed = false;
        user.imageUploadAgreed = false;
        user.shippingAgreed = false;
        return user;
    }

    public void agreeTerms(boolean privacy, boolean imageUpload, boolean shipping) {
        this.termsAgreed = true;
        this.privacyAgreed = privacy;
        this.imageUploadAgreed = imageUpload;
        this.shippingAgreed = shipping;
        this.termsAgreedAt = LocalDateTime.now();
    }

    public boolean isAllTermsAgreed() {
        return termsAgreed && privacyAgreed && imageUploadAgreed && shippingAgreed;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // address는 이 경로로 수정하지 않는다(확정 정책, 2026-08-08) — 이름·전화번호만 갱신 대상이다.
    public void updateProfile(String name, String phone) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    // AUTH-6 전용 — 호출 전 현재 비밀번호 확인·OAuth 전용 계정 여부 확인은 UserService가 담당한다.
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
