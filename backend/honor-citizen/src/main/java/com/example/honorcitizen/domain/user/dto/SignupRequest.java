package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    private String signupToken;

    // 비밀번호 정책 확정(2026-08-19): 최소 8자·최대 72자(BCrypt 입력 상한), 복잡도 조합 규칙 없음.
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String name;

    // 프론트 회원가입 화면(SignupPage.tsx)이 필수값으로 입력받는 값이라 가입 시점에 함께 받는다(2026-08-19 확인).
    // 국제번호 대응(RECOVERY-1 정책, 2026-08-21): 회원가입·회원정보 수정·계정 복구가 같은 규칙을 쓴다.
    @NotBlank
    @Pattern(regexp = PhoneValidation.PATTERN, message = PhoneValidation.MESSAGE)
    private String phone;
}
