package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.SchoolType;

// School 검색select(schoolId 있음) 또는 직접입력(schoolId 없음) 중 실제로 저장할 값을 확정한 결과.
// schoolId가 있으면 name/schoolType은 School 엔티티 값(클라이언트 요청값은 무시) — TODO.md "학생증 카드" 4-A.
record ResolvedSchool(Long schoolId, String schoolName, SchoolType schoolType) {
}
