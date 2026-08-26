package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
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

    // 학생증(STUDENT)일 때만 사용 — 신청서 전체에 1개(개인·단체 공통). 비학생증이면 null이어야 한다.
    // 서비스 레벨에서 isStudent 기준으로 조건부 필수 검증(validateStudentFields).
    private Orientation orientation;

    // 학생증(STUDENT)일 때만 사용 — 개인 신청도 단체 신청과 동일하게 Application 레벨 단일 필드.
    // UNIVERSITY일 때만 MemberRequest.studentId/department가 필수가 된다.
    private SchoolType schoolType;

    // 학생증(STUDENT)일 때만 사용 — 신청서 전체에 1개(개인·단체 공통). UNIVERSITY/HIGH_SCHOOL 둘 다 필수.
    // 서비스 레벨에서 isStudent 기준으로 조건부 필수 검증(validateStudentFields).
    private String schoolName;

    // 학교 검색select로 등록된 School을 선택했을 때만 값이 있다(School.id). 이 값이 있으면 서버가
    // schoolName/schoolType을 School 엔티티 값으로 강제 확정하고 위 두 필드의 요청값은 무시한다 —
    // 클라이언트가 schoolType을 임의로 바꿔 보내는 걸 원천 차단하기 위함(TODO.md 4-A).
    // null이면 기존처럼 schoolName/schoolType을 직접입력 값 그대로 사용한다.
    private Long schoolId;

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

    // schoolName은 저장·검증 전에 항상 트림된 값으로 취급한다(정책: 앞뒤 공백 트림 후 5~20자 검사).
    public String getSchoolName() {
        return schoolName == null ? null : schoolName.trim();
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

        @NotBlank
        @Size(max = 200)
        private String birthRegion;

        @NotNull
        private Gender gender;

        private LocalDate entryDate;

        private String studentId;

        @Size(max = 100)
        private String department;

        // 카드에 인쇄되는 주소 — 학생증을 제외한 카드종류에서 필수(서비스 레벨 조건부 검증).
        // 학생증은 카드에 주소를 표시하지 않으므로 값이 있으면 거절한다.
        @Size(max = 255)
        private String address;
    }
}
