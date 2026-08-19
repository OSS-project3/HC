package com.example.honorcitizen.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    // 일반 이메일 계정의 비밀번호 저장 형식. 접두사({bcrypt})로 알고리즘을 함께 저장해 향후
    // Argon2 등으로 교체해도 기존 해시를 그대로 검증할 수 있다(2026-08-19 확정, 지금은 BCrypt strength 10 기본값).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                // 이메일 회원가입 인증 코드 요청/확인은 로그인 전 단계이므로 비로그인 공개 API다(SIGNUP-1/2 정책).
                .requestMatchers("/api/auth/signup/**").permitAll()
                .requestMatchers("/api/applications/lookup").permitAll()
                // 후기 목록/단건 조회는 비로그인 공개 조회다(등록/수정/삭제는 아래 hasAnyRole 규칙 그대로 적용).
                // api.md §API 2·§API 3 참고 — 단건 조회는 로그인 여부에 따라 canEdit/canDelete만 달라진다.
                .requestMatchers(HttpMethod.GET, "/api/reviews", "/api/reviews/**").permitAll()
                // 공지사항/FAQ 목록·단건 조회도 비로그인 공개 조회다(board/api.md §API 1·2).
                .requestMatchers(HttpMethod.GET, "/api/boards", "/api/boards/**").permitAll()
                // 행사(부스 운영/법인·단체 협업) 목록·단건 조회도 비로그인 공개 조회다(events/api.md §API 1·2).
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()
                // 관리자 전용 쓰기 API(board/api.md "관리자 권한 강제 방식") — /api/** 공통 규칙보다 먼저 와야
                // ADMIN이 아닌 로그인 사용자가 USER 권한만으로 통과하는 것을 막는다.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint ->
                    endpoint.baseUri("/oauth2/authorization"))
                .redirectionEndpoint(redirect ->
                    redirect.baseUri("/login/oauth2/code/*"))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'"))
            )
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.setStatus(HttpStatus.FORBIDDEN.value())))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
