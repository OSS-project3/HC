package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupEmailVerificationRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
