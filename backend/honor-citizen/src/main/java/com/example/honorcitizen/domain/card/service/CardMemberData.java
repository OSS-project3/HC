package com.example.honorcitizen.domain.card.service;

import java.time.LocalDate;

// 카드 앞·뒷면 합성에 필요한 값. photo/logo/seal은 원본 바이트(신청 시 업로드된 이미지) — 각 필드
// 자리 크기에 맞춰 합성 시점에 스케일한다. logo/seal이 null이면 해당 요소를 그리지 않는다(개인
// 일반카드처럼 정책상 로고·직인이 없는 신청은 호출부가 null을 넘긴다 — 정책 판단은 이 클래스 밖).
// chineseName이 null/blank면 뒷면은 한자 없는 배치(noHanjaVariant)를 쓴다. zodiacBranch가 null이면
// 띠 이미지를 그리지 않는다(만세력 결과 미확정 등 호출부 판단).
record CardMemberData(
        String surname,
        String name,
        String englishName,
        String chineseName,
        String nameMeaning,
        String nameInterpretation,
        byte[] photo,
        String cardNumber,
        String address,
        LocalDate issueDate,
        String zodiacBranch,
        byte[] logo,
        byte[] seal) {

    String fullName() {
        return (surname == null ? "" : surname) + (name == null ? "" : name);
    }

    boolean hasHanja() {
        return chineseName != null && !chineseName.isBlank();
    }
}
