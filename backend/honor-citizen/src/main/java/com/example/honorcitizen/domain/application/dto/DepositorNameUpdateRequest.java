package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DepositorNameUpdateRequest {

    @NotBlank
    @Size(max = 60)
    private String depositorName;
}
