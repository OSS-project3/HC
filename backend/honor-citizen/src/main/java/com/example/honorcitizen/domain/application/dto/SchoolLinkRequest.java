package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 학교 연결(4-A-1) — 직접입력(schoolId=null)으로 접수된 STUDENT 신청을 이미 등록된
// School에 연결한다. 신규 School 생성은 이 API 범위 밖이다(schoolId는 반드시 기존 School PK).
// applicationVersion은 Application.version과 대조해 동시 수정을 감지한다(불일치 시
// APPLICATION_VERSION_CONFLICT, CardNumberBatchAssignRequest와 동일 패턴).
@Getter
@NoArgsConstructor
public class SchoolLinkRequest {

    @NotNull
    private Long schoolId;

    @NotNull
    private Long applicationVersion;
}
