package com.example.honorcitizen.infra.security;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenSessionStore tokenSessionStore;
    private final UserRepository userRepository;

    // GlobalExceptionHandler(@RestControllerAdvice)로 그대로 위임해 필터 단계 예외도 컨트롤러와
    // 동일한 JSON 오류 포맷으로 응답한다(RECOVERY-2 — HTML/빈 503 응답 금지).
    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)
                && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isAccessToken(token)) {
            Claims claims = jwtTokenProvider.getClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);

            boolean sessionValid;
            try {
                sessionValid = tokenSessionStore.isAccessTokenSessionValid(
                        token, userId, jwtTokenProvider.getAuthIssuedAtMillis(token));
            } catch (CustomException e) {
                // Redis 장애로 세션 유효성을 확인할 수 없음 — 미확인 토큰을 통과시키지 않는 fail-closed.
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            }

            if (sessionValid && userRepository.existsById(userId) && isValidRole(role)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            String cookieToken = Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            if (StringUtils.hasText(cookieToken)) {
                return cookieToken;
            }
        }

        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isValidRole(String role) {
        try {
            UserRole.valueOf(role);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
}
