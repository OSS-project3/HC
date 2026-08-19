package com.example.honorcitizen.domain.inquiry.service;

import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateRequest;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InquiryServiceTest {

    @Autowired
    private InquiryService inquiryService;
    @Autowired
    private InquiryRepository inquiryRepository;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
    }

    private InquiryCreateRequest request() {
        return new InquiryCreateRequest(InquiryCategory.CARD_ISSUANCE, "홍길동",
                "hong@example.com", "010-1111-2222", "카드 발급 문의", "언제 발급되나요?", true);
    }

    @Test
    void createSavesInquiryOwnedByCallerWithPendingStatus() {
        InquiryCreateResponse response = inquiryService.create(USER_ID, request());

        Inquiry saved = inquiryRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getCategory()).isEqualTo(InquiryCategory.CARD_ISSUANCE);
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(saved.getEmail()).isEqualTo("hong@example.com");
        assertThat(saved.getPhone()).isEqualTo("010-1111-2222");
        assertThat(saved.getTitle()).isEqualTo("카드 발급 문의");
        assertThat(saved.getContent()).isEqualTo("언제 발급되나요?");
        assertThat(saved.getStatus()).isEqualTo(InquiryStatus.PENDING);
        assertThat(saved.getAnswer()).isNull();
    }

    @Test
    void listMineReturnsOnlyOwnInquiriesNewestFirst() throws InterruptedException {
        inquiryService.create(USER_ID, request());
        Thread.sleep(5);
        InquiryCreateResponse second = inquiryService.create(USER_ID, request());
        inquiryService.create(2L, request());

        List<InquiryListItemResponse> mine = inquiryService.listMine(USER_ID);

        assertThat(mine).hasSize(2);
        assertThat(mine.get(0).getId()).isEqualTo(second.getId());
    }

    @Test
    void getMineDetailReturnsOwnInquiry() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());

        InquiryDetailResponse detail = inquiryService.getMineDetail(USER_ID, created.getId());

        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getTitle()).isEqualTo("카드 발급 문의");
    }

    @Test
    void getMineDetailForMissingInquiryThrowsNotFound() {
        assertThatThrownBy(() -> inquiryService.getMineDetail(USER_ID, 999999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }

    @Test
    void getMineDetailForOtherUsersInquiryThrowsForbidden() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());

        assertThatThrownBy(() -> inquiryService.getMineDetail(2L, created.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void listAdminReturnsAllInquiriesNewestFirst() throws InterruptedException {
        inquiryService.create(USER_ID, request());
        Thread.sleep(5);
        InquiryCreateResponse second = inquiryService.create(2L, request());

        List<InquiryListItemResponse> all = inquiryService.listAdmin();

        assertThat(all).hasSize(2);
        assertThat(all.get(0).getId()).isEqualTo(second.getId());
    }

    @Test
    void getAdminDetailReturnsAnyUsersInquiry() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());

        InquiryDetailResponse detail = inquiryService.getAdminDetail(created.getId());

        assertThat(detail.getId()).isEqualTo(created.getId());
    }

    @Test
    void getAdminDetailForMissingInquiryThrowsNotFound() {
        assertThatThrownBy(() -> inquiryService.getAdminDetail(999999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }
}
