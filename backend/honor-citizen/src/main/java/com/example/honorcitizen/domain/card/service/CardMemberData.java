package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.SchoolType;

import java.time.LocalDate;

// 카드 앞·뒷면 합성에 필요한 값. photo/logo/seal은 원본 바이트(신청 시 업로드된 이미지) — 각 필드
// 자리 크기에 맞춰 합성 시점에 스케일한다. logo/seal이 null이면 해당 요소를 그리지 않는다(개인
// 일반카드처럼 정책상 로고·직인이 없는 신청은 호출부가 null을 넘긴다 — 정책 판단은 이 클래스 밖).
// chineseName이 null/blank면 뒷면은 한자 없는 배치(noHanjaVariant)를 쓴다. zodiacBranch가 null이면
// 띠 이미지를 그리지 않는다(만세력 결과 미확정 등 호출부 판단).
//
// schoolType/studentOrientation/studentId/department/birthDate/templateFront/templateBack은
// 학생증(STUDENT) 전용이다(4-C) — 그 외 카드종류는 전부 null. schoolType으로 앞면 필드 표시를
// 분기한다(UNIVERSITY=학번+학과 표시·생년월일 숨김, HIGH_SCHOOL=반대). studentOrientation은
// CardDesignOrientation을 그대로 쓴다(Application.orientation과 값은 같지만 별개 enum이라 호출부가
// 변환해서 넘긴다). templateFront/templateBack은 다른 3종처럼 classpath가 아니라 S3에서 내려받은
// 원본 템플릿 바이트다(호출부 CardRenderPreparation이 UploadFile 경유로 미리 받아서 넘긴다, 4-D).
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
        byte[] seal,
        SchoolType schoolType,
        CardDesignOrientation studentOrientation,
        String studentId,
        String department,
        LocalDate birthDate,
        byte[] templateFront,
        byte[] templateBack,
        int zodiacDesignSet) {

    // 기존 호출부(십이간지 디자인 세트 도입 이전, 학생증 포함) 하위 호환용 — 1번 세트로 그린다.
    // 실제 렌더링 경로(CardRenderPreparation)는 항상 Application.zodiacDesignSet을 명시적으로
    // 넘기므로 이 기본값은 테스트 편의용일 뿐이다(2026-09-06 신규 필드).
    CardMemberData(String surname, String name, String englishName, String chineseName, String nameMeaning,
            String nameInterpretation, byte[] photo, String cardNumber, String address, LocalDate issueDate,
            String zodiacBranch, byte[] logo, byte[] seal, SchoolType schoolType,
            CardDesignOrientation studentOrientation, String studentId, String department, LocalDate birthDate,
            byte[] templateFront, byte[] templateBack) {
        this(surname, name, englishName, chineseName, nameMeaning, nameInterpretation, photo, cardNumber,
                address, issueDate, zodiacBranch, logo, seal, schoolType, studentOrientation, studentId,
                department, birthDate, templateFront, templateBack, 1);
    }

    // 기존 호출부(학생증 아닌 카드종류) 하위 호환용 — 학생증 전용 필드 없이 호출하면 전부 null로 생성한다.
    CardMemberData(String surname, String name, String englishName, String chineseName, String nameMeaning,
            String nameInterpretation, byte[] photo, String cardNumber, String address, LocalDate issueDate,
            String zodiacBranch, byte[] logo, byte[] seal) {
        this(surname, name, englishName, chineseName, nameMeaning, nameInterpretation, photo, cardNumber,
                address, issueDate, zodiacBranch, logo, seal, null, null, null, null, null, null, null, 1);
    }

    String fullName() {
        return (surname == null ? "" : surname) + (name == null ? "" : name);
    }

    boolean hasHanja() {
        return chineseName != null && !chineseName.isBlank();
    }

    boolean isUniversity() {
        return schoolType == SchoolType.UNIVERSITY;
    }
}
