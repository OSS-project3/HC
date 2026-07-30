package com.example.honorcitizen.infra.card;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CardImageData {
    private String fullNameKo;      // 한국이름 (예: 이소연)
    private String nameOrigin;      // 한자 (예: 李昭延), nullable
    private String fullNameEn;      // 영어이름 (예: LEE SO YEON)
    private String cardNumber;      // 일련번호 (예: HN-KR-2609-0001)
    private String birthRegion;     // 주소 (예: New York, USA)
    private String issuerName;      // 발행처 (예: 전북특별자치도 전주시)
    private String meaning;         // 이름 의미
    private LocalDate issuedDate;   // 발급일자
    private LocalDate birthDate;    // 띠 계산용
}
