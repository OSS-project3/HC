package com.example.honorcitizen.domain.user.dto;

// 회원가입·회원정보 수정·계정 복구(아이디 찾기) DTO가 공유하는 전화번호 형식 규칙(RECOVERY-1 정책).
// 선택적 선행 +와 숫자·공백·하이픈을 허용하고 raw 길이는 최대 25자다 — 이건 "모양"만 검증하는
// 느슨한 게이트이고, 정규화 후 숫자 9~15자리인지는 실제 비교가 필요한 지점(아이디 찾기 매칭)에서
// User.normalizePhone()로 정규화한 뒤 서비스 계층이 검증한다.
public final class PhoneValidation {

    public static final String PATTERN = "^\\+?[0-9 \\-]{9,25}$";
    public static final String MESSAGE = "전화번호 형식이 올바르지 않습니다.";

    private PhoneValidation() {
    }
}
