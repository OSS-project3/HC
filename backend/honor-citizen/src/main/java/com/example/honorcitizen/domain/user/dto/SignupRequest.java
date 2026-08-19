package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    // BCrypt는 72바이트를 초과하는 입력을 안전하게 처리하지 못하므로 상한을 둔다(길이 정책 자체는 미확정 — AUTH-4 보고 시 확인 필요).
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String name;
}
