package com.example.honorcitizen.common.enums;

// 공지/FAQ 서버 검색 대상 컬럼(ReviewSearchType과 같은 목적). Board는 작성자 표시명 같은 필드가
// 없어(관리자만 작성하는 콘텐츠) AUTHOR는 두지 않는다 — Review와 컬럼 구성이 달라 그대로 재사용할 수
// 없는 이유(2026-09-05 정책 확인).
public enum BoardSearchType {
    ALL,
    TITLE,
    CONTENT
}
