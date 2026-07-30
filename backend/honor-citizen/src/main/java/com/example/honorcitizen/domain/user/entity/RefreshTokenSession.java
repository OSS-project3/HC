package com.example.honorcitizen.domain.user.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenId;

    @Column(nullable = false, length = 2048)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefreshTokenStatus status;

    public static RefreshTokenSession active(Long userId,
                                             String sessionId,
                                             String tokenId,
                                             String refreshToken,
                                             LocalDateTime expiresAt) {
        RefreshTokenSession session = new RefreshTokenSession();
        session.userId = userId;
        session.sessionId = sessionId;
        session.tokenId = tokenId;
        session.refreshToken = refreshToken;
        session.expiresAt = expiresAt;
        session.status = RefreshTokenStatus.ACTIVE;
        return session;
    }

    public void rotate() {
        this.status = RefreshTokenStatus.ROTATED;
    }

    public void revoke() {
        this.status = RefreshTokenStatus.REVOKED;
    }
}
