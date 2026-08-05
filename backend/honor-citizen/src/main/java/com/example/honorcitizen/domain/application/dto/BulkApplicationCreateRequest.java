package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.IssueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BulkApplicationCreateRequest {

    @NotNull
    private Long cardTypeId;

    @NotNull
    private IssueType issueType;

    @NotNull
    @Valid
    private ApplicantRequest applicant;

    @Valid
    private ReceiverRequest receiver;

    @Getter
    @NoArgsConstructor
    public static class ApplicantRequest {
        private String organizationName;
        private String department;

        @NotBlank
        private String name;

        @NotBlank
        private String phone;
    }

    @Getter
    @NoArgsConstructor
    public static class ReceiverRequest {
        private boolean sameAsApplicant;
        private String organizationName;
        private String department;
        private String name;
        private String phone;
        private String zipCode;
        private String address;
        private String detailAddress;
        private String deliveryRequest;
    }
}
