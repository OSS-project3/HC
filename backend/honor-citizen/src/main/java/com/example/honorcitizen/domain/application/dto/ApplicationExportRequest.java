package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationExportRequest {

    @NotEmpty
    private List<Long> applicationIds;

    @NotNull
    private ApplicationType type;
}
