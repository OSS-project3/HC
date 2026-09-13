package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

// 관리자 작명 화면용 — 신청 구성원(개인=1명, 단체=엑셀 행 N명)의 신상/사주 입력 정보와 확정 이름.
// 만세력 계산에 필요한 생년월일·출생시간·출생지역을 노출한다.
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminApplicationMemberResponse {

    private final Long memberId;
    private final String englishName;
    private final String nationality;
    private final Gender gender;
    private final LocalDate birthDate;
    private final LocalTime birthTime;
    private final String birthRegion;
    // 카드 표기용 주소 — 학생증은 항상 null(카드에 주소를 표시하지 않음), 그 외 카드종류는
    // 개인/단체 신청 모두 이 값을 사용한다(admin-saju.md 확정 정책). 배송용 Receiver.address와는
    // 별도 값이라 이 응답에서 별도로 노출한다(2026-09-13, 개인 신청 주소 누락 검증 후속 조치).
    private final String address;
    // 작명 결과(확정 한글/한자 이름) — 아직 지정 전이면 null.
    private final String surname;
    // 성씨 한자 — surname으로부터 자동 유도된 값(10대 성씨만 존재, ApplicationMember.assignKoreanName
    // 참고). 관리자가 입력하는 값이 아니라 참고용 표시 필드.
    private final String surnameHanja;
    private final String assignedName;
    private final String assignedHanja;
    // 확정 이름의 훈음(nameMeaning)·의미(nameInterpretation) — assignKoreanName()에서 이름과 함께
    // 저장되는 값인데도 이 응답에 없어 관리자가 재조회 시 확인할 수 없었다(2026-09-13 추가).
    private final String nameMeaning;
    private final String nameInterpretation;
    // 단체 신청 사진번호(카드번호 일괄 매칭 키) + 관리자가 확정한 카드번호 — 지정 전이면 null.
    private final String photoNumber;
    private final String cardNumber;
    // 카드 생성 성공 시 함께 저장되는 값들 — 관리자가 화면을 새로고침해도 카드 생성 완료 여부와
    // 발급일자를 복원할 수 있도록 노출한다(2026-09-13 추가, 신규 저장 로직 없음). 카드 생성 완료
    // 여부는 별도 컬럼이 없고 cardFrontPath!=null로 판정한다(ApplicationMember.isCardGenerated()와
    // 동일 기준).
    private final LocalDate issueDate;
    private final String cardFrontPath;
    private final String cardBackPath;

    private AdminApplicationMemberResponse(ApplicationMember m) {
        this.memberId = m.getId();
        this.englishName = m.getEnglishName();
        this.nationality = m.getNationality();
        this.gender = m.getGender();
        this.birthDate = m.getBirthDate();
        this.birthTime = m.getBirthTime();
        this.birthRegion = m.getBirthRegion();
        this.address = m.getAddress();
        this.surname = m.getSurname();
        this.surnameHanja = m.getSurnameHanja();
        this.assignedName = m.getName();
        this.assignedHanja = m.getChineseName();
        this.nameMeaning = m.getNameMeaning();
        this.nameInterpretation = m.getNameInterpretation();
        this.photoNumber = m.getPhotoNumber();
        this.cardNumber = m.getCardNumber();
        this.issueDate = m.getIssueDate();
        this.cardFrontPath = m.getCardFrontPath();
        this.cardBackPath = m.getCardBackPath();
    }

    public static AdminApplicationMemberResponse from(ApplicationMember m) {
        return new AdminApplicationMemberResponse(m);
    }
}
