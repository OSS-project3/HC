package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 관리자 대시보드 인앱 작명 확정 요청 — 선택한 추천 이름을 구성원에 저장한다.
@Getter
public class NameAssignRequest {

    @NotBlank
    private String name;   // 한글 이름

    private String hanja;  // 한자(없을 수 있음)
    private String reading; // 훈음
    private String meaning; // 뜻풀이
}
