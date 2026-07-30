package com.example.honorcitizen.domain.user.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private String refreshToken;

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
}
