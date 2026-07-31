package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.LookupMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicationLookupRequest {

    @NotNull
    private LookupMethod method;

    @NotBlank
    private String keyValue;

    private String phone;

    private String email;
}
