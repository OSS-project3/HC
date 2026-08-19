package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
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

    // 학생증(STUDENT)일 때만 사용 — 신청서 전체에 1개(개인 DTO와 동일 위치·의미).
    private Orientation orientation;

    // 학생증(STUDENT)일 때만 사용. 학번·학과는 이 필드와 무관하게 여전히 첨부 엑셀로만 받는다(BulkExcelParser).
    private SchoolType schoolType;

    // 학생증(STUDENT)일 때만 사용 — 신청서 전체에 1개(개인 DTO와 동일 위치·의미). 단체 신청은 항상 한 학교 단위로 접수된다는 전제.
    private String schoolName;

    @NotNull
    @Valid
    private ApplicantRequest applicant;

    @Valid
    private ReceiverRequest receiver;

    // schoolName은 저장·검증 전에 항상 트림된 값으로 취급한다(정책: 앞뒤 공백 트림 후 5~20자 검사).
    public String getSchoolName() {
        return schoolName == null ? null : schoolName.trim();
    }

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
