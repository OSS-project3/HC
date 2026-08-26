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
    // 작명 결과(확정 한글/한자 이름) — 아직 지정 전이면 null.
    private final String assignedName;
    private final String assignedHanja;
    // 단체 신청 사진번호(카드번호 일괄 매칭 키) + 관리자가 확정한 카드번호 — 지정 전이면 null.
    private final String photoNumber;
    private final String cardNumber;

    private AdminApplicationMemberResponse(ApplicationMember m) {
        this.memberId = m.getId();
        this.englishName = m.getEnglishName();
        this.nationality = m.getNationality();
        this.gender = m.getGender();
        this.birthDate = m.getBirthDate();
        this.birthTime = m.getBirthTime();
        this.birthRegion = m.getBirthRegion();
        this.assignedName = m.getName();
        this.assignedHanja = m.getChineseName();
        this.photoNumber = m.getPhotoNumber();
        this.cardNumber = m.getCardNumber();
    }

    public static AdminApplicationMemberResponse from(ApplicationMember m) {
        return new AdminApplicationMemberResponse(m);
    }
}
