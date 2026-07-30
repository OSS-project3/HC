package com.example.honorcitizen.domain.koreanname.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class KoreanNameRegisterRequest {

    @NotBlank
    private String familyName;

    @NotBlank
    private String givenName;

    @NotBlank
    private String fullNameEn;

    @NotBlank
    private String meaning;

    private String nameOrigin;
}
