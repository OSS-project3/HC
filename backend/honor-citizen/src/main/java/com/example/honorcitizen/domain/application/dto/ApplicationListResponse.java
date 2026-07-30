package com.example.honorcitizen.domain.application.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ApplicationListResponse {

    private final List<ApplicationSummaryResponse> applications;

    public ApplicationListResponse(List<ApplicationSummaryResponse> applications) {
        this.applications = applications;
    }
}
