package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ApplicationCreateRequest {

    @NotNull
    private Long cardTypeId;

    @NotNull
    private IssueType issueType;

    @NotNull
    @Valid
    private ApplicantRequest applicant;

    @Valid
    private ReceiverRequest receiver;

    @NotNull
    @Valid
    private MemberRequest member;

    public boolean isReceiverSameAsApplicant() {
        return receiver == null || receiver.isSameAsApplicant();
    }

    @Getter
    @NoArgsConstructor
    public static class ApplicantRequest {
        @NotBlank
        private String name;

        @NotBlank
        private String phone;

        private String email;
    }

    @Getter
    @NoArgsConstructor
    public static class ReceiverRequest {
        private boolean sameAsApplicant;
        private String name;
        private String phone;

        @NotBlank
        private String zipCode;

        @NotBlank
        private String address;

        private String detailAddress;
        private String deliveryRequest;
    }

    @Getter
    @NoArgsConstructor
    public static class MemberRequest {
        @NotBlank
        private String englishName;

        @NotNull
        private LocalDate birthDate;

        @NotBlank
        private String nationality;

        private LocalTime birthTime;

        private String birthRegion;

        @NotNull
        private Gender gender;

        private LocalDate entryDate;

        private String studentId;

        private String department;
    }
}
