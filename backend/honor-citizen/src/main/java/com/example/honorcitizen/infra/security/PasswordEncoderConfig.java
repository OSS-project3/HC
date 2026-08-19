package com.example.honorcitizen.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

// PasswordEncoder는 별도의 최소 Configuration으로 분리한다. SecurityConfig에 두면
// SecurityConfig(JwtAuthFilter/OAuth2SuccessHandler 등 의존) → UserService → PasswordEncoder →
// (다시) SecurityConfig로 순환 의존이 생겨 컨텍스트 로딩이 BeanCurrentlyInCreationException으로
// 실패한다(AUTH-4에서 UserService가 PasswordEncoder를 직접 주입받기 시작하며 실제로 발생 확인).
@Configuration
public class PasswordEncoderConfig {

    // 일반 이메일 계정의 비밀번호 저장 형식. 접두사({bcrypt})로 알고리즘을 함께 저장해 향후
    // Argon2 등으로 교체해도 기존 해시를 그대로 검증할 수 있다(2026-08-19 확정, 지금은 BCrypt strength 10 기본값).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
