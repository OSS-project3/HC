package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.dto.validation.ValidNationality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
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

    @Getter
    @NoArgsConstructor
    public static class MemberRequest {
        @NotBlank
        @Size(max = 100)
        private String englishName;

        @NotNull
        @Past
        private LocalDate birthDate;

        @NotBlank
        @ValidNationality
        private String nationality;

        private LocalTime birthTime;

        @Size(max = 200)
        private String birthRegion;

        @NotNull
        private Gender gender;

        private LocalDate entryDate;

        private String studentId;

        @Size(max = 100)
        private String department;
    }
}
