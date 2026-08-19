package com.example.honorcitizen.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// 값은 frontend/src/pages/InquiryPage/InquiryPage.tsx의 SelectField 옵션(한글 고정 5개)과
// 1:1 대응한다 — 프론트가 한글 문자열을 그대로 보내므로 @JsonValue/@JsonCreator로 매핑해
// 프론트 수정 없이 백엔드만 enum으로 강제한다(LookupMethod와 동일 패턴).
public enum InquiryCategory {
    PRODUCTION("제작 신청"),
    PAYMENT_AND_SHIPPING("결제 및 배송"),
    CARD_ISSUANCE("카드 발급"),
    EVENT_COLLABORATION("행사·단체 협업"),
    OTHER("기타");

    private final String value;

    InquiryCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InquiryCategory from(String value) {
        for (InquiryCategory category : values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown inquiry category: " + value);
    }
}
