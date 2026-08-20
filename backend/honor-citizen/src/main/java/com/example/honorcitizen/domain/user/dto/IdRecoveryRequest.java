package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IdRecoveryRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    // 국제번호 대응(RECOVERY-1 정책, 2026-08-21): 회원가입·회원정보 수정과 같은 규칙을 쓴다.
    @NotBlank
    @Pattern(regexp = PhoneValidation.PATTERN, message = PhoneValidation.MESSAGE)
    private String phone;
}
