package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 관리자 대시보드 인앱 작명 확정 요청 — 선택한 추천 이름을 구성원에 저장한다.
@Getter
public class NameAssignRequest {

    // 성씨 — NAME_EDITING 중에는 선택 입력(값을 안 보내면 기존/미정 상태 유지), completeNaming() 집계 검증 시 필수.
    private String surname;

    @NotBlank
    private String name;   // 한글 이름(성씨 제외)

    private String hanja;  // 한자(없을 수 있음)
    private String reading; // 훈음

    @NotBlank
    private String meaning; // 뜻풀이 — 추천 이름은 사전 의미, 수동 입력은 관리자가 반드시 입력
}
