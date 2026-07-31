package com.example.honorcitizen.domain.user.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String oauthId;

    @Column(nullable = false)
    private String oauthProvider;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private LocalDateTime withdrawalRequestedAt;

    private LocalDateTime anonymizedAt;

    public static User createNewUser(String email, String oauthId, String oauthProvider, String name) {
        User user = new User();
        user.email = email;
        user.oauthId = oauthId;
        user.oauthProvider = oauthProvider;
        user.name = name;
        user.role = UserRole.USER;
        user.termsAgreed = false;
        user.privacyAgreed = false;
        user.imageUploadAgreed = false;
        user.shippingAgreed = false;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawalRequestedAt = LocalDateTime.now();
    }

    public boolean isWithdrawn() {
        return this.status == UserStatus.WITHDRAWN;
    }

    public boolean isRestorable() {
        return isWithdrawn() && this.anonymizedAt == null;
    }

    public void restore() {
        this.status = UserStatus.ACTIVE;
        this.withdrawalRequestedAt = null;
    }

    public void anonymize() {
        String anonymousSuffix = UUID.randomUUID().toString();
        this.email = "withdrawn-" + this.id + "@anonymized.local";
        this.name = "탈퇴한 사용자";
        this.oauthId = "anon-" + anonymousSuffix;
        this.oauthProvider = "ANONYMIZED";
        this.phone = null;
        this.address = null;
        this.anonymizedAt = LocalDateTime.now();
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

    public void updateProfile(String name, String phone, String address) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (address != null) {
            this.address = address;
        }
    }
}
