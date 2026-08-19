package com.example.honorcitizen.domain.inquiry.entity;

import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.InquiryStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryTest {

    @Test
    void createSetsAllFieldsAndDefaultsToPending() {
        Inquiry inquiry = Inquiry.create(1L, InquiryCategory.CARD_ISSUANCE, "홍길동",
                "hong@example.com", "010-1111-2222", "카드 발급 문의", "언제 발급되나요?");

        assertThat(inquiry.getUserId()).isEqualTo(1L);
        assertThat(inquiry.getCategory()).isEqualTo(InquiryCategory.CARD_ISSUANCE);
        assertThat(inquiry.getName()).isEqualTo("홍길동");
        assertThat(inquiry.getEmail()).isEqualTo("hong@example.com");
        assertThat(inquiry.getPhone()).isEqualTo("010-1111-2222");
        assertThat(inquiry.getTitle()).isEqualTo("카드 발급 문의");
        assertThat(inquiry.getContent()).isEqualTo("언제 발급되나요?");
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
        assertThat(inquiry.getAnswer()).isNull();
        assertThat(inquiry.getAnsweredAt()).isNull();
    }

    @Test
    void isOwnedByComparesUserId() {
        Inquiry inquiry = Inquiry.create(1L, InquiryCategory.OTHER, "홍길동",
                "hong@example.com", "010-1111-2222", "제목", "내용");

        assertThat(inquiry.isOwnedBy(1L)).isTrue();
        assertThat(inquiry.isOwnedBy(2L)).isFalse();
    }
}
