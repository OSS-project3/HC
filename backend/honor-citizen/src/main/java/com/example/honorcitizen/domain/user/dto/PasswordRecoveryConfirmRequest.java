package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 2026-08-21 정책 확정: 이 API는 이메일을 다시 받지 않는다 — 8-1 요청 응답의 requestId로 challenge를
// 특정한다(email은 그 challenge에 결속된 userId를 통해 서버가 재조회한다).
@Getter
@NoArgsConstructor
public class PasswordRecoveryConfirmRequest {

    @NotBlank
    private String requestId;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;

    // 비밀번호 정책은 회원가입(SignupRequest)과 동일: 최소 8자·최대 72자, 복잡도 조합 규칙 없음.
    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
