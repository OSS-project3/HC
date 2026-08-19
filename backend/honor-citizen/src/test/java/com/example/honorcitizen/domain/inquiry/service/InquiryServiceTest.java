package com.example.honorcitizen.domain.inquiry.service;

import com.example.honorcitizen.common.enums.EmailType;
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
import com.example.honorcitizen.infra.mail.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
class InquiryServiceTest {

    @Autowired
    private InquiryService inquiryService;
    @Autowired
    private InquiryRepository inquiryRepository;
    @MockitoBean
    private EmailSender emailSender;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        reset(emailSender);
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

    @Test
    void answerSavesAnswerAndTransitionsToCompleted() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());

        inquiryService.answer(created.getId(), "영업일 기준 5일 이내 발급됩니다.");

        Inquiry updated = inquiryRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getAnswer()).isEqualTo("영업일 기준 5일 이내 발급됩니다.");
        assertThat(updated.getAnsweredAt()).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
    }

    @Test
    void answerSendsEmailOnFirstAnswer() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());

        inquiryService.answer(created.getId(), "영업일 기준 5일 이내 발급됩니다.");

        verify(emailSender).send(eq("hong@example.com"), eq(EmailType.INQUIRY_ANSWERED),
                anyString(), anyString(), anyString());
    }

    @Test
    void answerDoesNotResendEmailWhenAlreadyAnswered() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());
        inquiryService.answer(created.getId(), "1차 답변");
        reset(emailSender);

        inquiryService.answer(created.getId(), "수정된 답변");

        Inquiry updated = inquiryRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getAnswer()).isEqualTo("수정된 답변");
        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void answerKeepsSavedAnswerEvenWhenEmailSendFails() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());
        doThrow(new CustomException(ErrorCode.EMAIL_DELIVERY_FAILED))
                .when(emailSender).send(anyString(), any(), anyString(), anyString(), anyString());

        inquiryService.answer(created.getId(), "영업일 기준 5일 이내 발급됩니다.");

        Inquiry updated = inquiryRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getAnswer()).isEqualTo("영업일 기준 5일 이내 발급됩니다.");
        assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
    }

    @Test
    void answerForMissingInquiryThrowsNotFound() {
        assertThatThrownBy(() -> inquiryService.answer(999999L, "답변"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }

    @Test
    void changeStatusAllowsCompletedWithoutAnswer() {
        InquiryCreateResponse created = inquiryService.create(USER_ID, request());

        inquiryService.changeStatus(created.getId(), InquiryStatus.COMPLETED);

        Inquiry updated = inquiryRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
        assertThat(updated.getAnswer()).isNull();
        verify(emailSender, never()).send(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void changeStatusForMissingInquiryThrowsNotFound() {
        assertThatThrownBy(() -> inquiryService.changeStatus(999999L, InquiryStatus.COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }
}
