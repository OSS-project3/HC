package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final String issuer;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(Long userId, UserRole role) {
        // 표준 iat는 초 단위라, 재설정과 새 로그인이 같은 초에 겹치면 새 토큰까지 오거절할 수 있다
        // (RECOVERY-2 정책) — millisecond 정밀도의 커스텀 클레임을 별도로 심어 무효화 비교에 쓴다.
        return buildToken(userId, role, "access", null, accessTokenExpiry, System.currentTimeMillis());
    }

    public String generateAccessToken(Long userId, String email, UserRole role) {
        return generateAccessToken(userId, role);
    }

    public String generateRefreshToken(Long userId, UserRole role, String sessionId) {
        return buildToken(userId, role, "refresh", sessionId, refreshTokenExpiry, null);
    }

    public String generateRefreshToken(Long userId, String email, UserRole role, String sessionId) {
        return generateRefreshToken(userId, role, sessionId);
    }

    private String buildToken(Long userId, UserRole role, String type, String sessionId, long expiry, Long authIssuedAtMillis) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .claim("role", role.name())
                .claim("typ", type)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiry))
                .signWith(secretKey, SIG.HS256);

        if (sessionId != null) {
            builder.claim("sid", sessionId);
        }
        if (authIssuedAtMillis != null) {
            builder.claim("iam", authIssuedAtMillis);
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            var jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            if (!SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) {
                log.warn("JWT 허용되지 않은 alg: {}", jws.getHeader().getAlgorithm());
                return false;
            }
            if (jws.getPayload().getSubject() == null) {
                log.warn("JWT sub 누락");
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("보안 이벤트: 만료 토큰 사용");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public String getSessionId(String token) {
        return getClaims(token).get("sid", String.class);
    }

    // 배포 전 발급된 구버전 토큰이거나 refresh token이면 null(RECOVERY-2 — revoked-after 비교용).
    public Long getAuthIssuedAtMillis(String token) {
        return getClaims(token).get("iam", Long.class);
    }

    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public LocalDateTime getExpirationDateTime(String token) {
        return LocalDateTime.ofInstant(getExpiration(token).toInstant(), ZoneId.systemDefault());
    }

    public long getRemainingMillis(String token) {
        return Math.max(0, getExpiration(token).getTime() - System.currentTimeMillis());
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getClaims(token).get("typ", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaims(token).get("typ", String.class));
    }
}
