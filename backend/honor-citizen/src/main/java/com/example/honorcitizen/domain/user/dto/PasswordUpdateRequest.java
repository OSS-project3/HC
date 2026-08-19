package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordUpdateRequest {

    @NotBlank
    private String currentPassword;

    // AUTH-4와 동일한 정책(2026-08-19 확정): 최소 8자·최대 72자, 복잡도 규칙 없음.
    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
