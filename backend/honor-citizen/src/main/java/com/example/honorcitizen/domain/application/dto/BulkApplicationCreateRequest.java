package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.IssueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @Size(max = 200)
        private String organizationName;

        @Size(max = 100)
        private String department;

        @NotBlank
        @Size(max = 100)
        private String name;

        @NotBlank
        private String phone;

        @Email
        @Size(max = 255)
        private String email;
    }

    @Getter
    @NoArgsConstructor
    public static class ReceiverRequest {
        private boolean sameAsApplicant;

        @Size(max = 200)
        private String organizationName;

        @Size(max = 100)
        private String department;

        @Size(max = 100)
        private String name;

        private String phone;

        @NotBlank
        @Size(max = 10)
        private String zipCode;

        @NotBlank
        @Size(max = 255)
        private String address;

        @Size(max = 255)
        private String detailAddress;

        @Size(max = 255)
        private String deliveryRequest;
    }
}
