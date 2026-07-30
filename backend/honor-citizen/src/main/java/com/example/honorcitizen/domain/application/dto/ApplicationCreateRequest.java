package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ApplicationCreateRequest {

    @NotBlank
    private String nameEn;

    @NotBlank
    private String nationality;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotNull
    private LocalTime birthTime;

    @NotBlank
    private String birthRegion;

    @NotNull
    private Gender gender;

    @NotBlank
    private String photoId;
}
