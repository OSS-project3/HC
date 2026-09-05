package com.example.honorcitizen.domain.stats.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.domain.stats.dto.AdminStatsResponse;
import com.example.honorcitizen.domain.user.service.AdminAuthorizationService;
import com.example.honorcitizen.domain.user.service.AdminAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// 관리자 통계(2026-09-05 정책) — 전체/개인/단체 신청 수 + 문의 총/답변대기/답변완료 수를 정확한
// DB 집계로 반환하는지 검증. AdminPage.tsx의 기존 size=100 페이지 슬라이스 계산과 달리 100건을
// 넘겨도 정확해야 한다는 게 이 기능의 핵심이라, 개인 신청을 다수 만들어 그 경계도 함께 확인한다.
@SpringBootTest
class AdminStatsServiceTest {

    private static final Long ADMIN_USER_ID = 1L;

    @Autowired
    private AdminStatsService adminStatsService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private InquiryRepository inquiryRepository;
    @MockitoBean
    private AdminAuthorizationService adminAuthorizationService;

    private CardType cardType;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        inquiryRepository.deleteAll();
        cardTypeRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-stats", null, BigDecimal.valueOf(30000)));
    }

    private void saveIndividual(String applicationNumber) {
        applicationRepository.save(Application.createIndividual(
                1L, applicationNumber, cardType.getId(), IssueType.MOBILE, true, null, null));
    }

    private void saveGroup(String applicationNumber) {
        applicationRepository.save(Application.createGroup(
                1L, applicationNumber, cardType.getId(), IssueType.MOBILE, true, 5, null, null, null));
    }

    private Inquiry saveInquiry(String title) {
        return inquiryRepository.save(Inquiry.create(
                1L, InquiryCategory.OTHER, "홍길동", "user@example.com", "010-0000-0000", title, "내용"));
    }

    @Test
    void countsApplicationsByTypeAndTotalAccurately() {
        saveIndividual("APP-2026-STAT001");
        saveIndividual("APP-2026-STAT002");
        saveGroup("APP-2026-STAT003");

        AdminStatsResponse stats = adminStatsService.getStats(ADMIN_USER_ID);

        assertThat(stats.getTotalApplications()).isEqualTo(3);
        assertThat(stats.getIndividualApplications()).isEqualTo(2);
        assertThat(stats.getGroupApplications()).isEqualTo(1);
    }

    @Test
    void countIsAccurateBeyondTheHundredRowPageSliceTheFrontendUsedToRelyOn() {
        for (int i = 0; i < 105; i++) {
            saveIndividual("APP-2026-STAT1" + String.format("%04d", i));
        }
        saveGroup("APP-2026-STAT20000");

        AdminStatsResponse stats = adminStatsService.getStats(ADMIN_USER_ID);

        assertThat(stats.getTotalApplications()).isEqualTo(106);
        assertThat(stats.getIndividualApplications()).isEqualTo(105);
        assertThat(stats.getGroupApplications()).isEqualTo(1);
    }

    @Test
    void countsInquiriesByStatusAndTotal() {
        saveInquiry("문의1");
        saveInquiry("문의2");
        Inquiry answered = saveInquiry("문의3");
        answered.answer("답변입니다");
        inquiryRepository.save(answered);

        AdminStatsResponse stats = adminStatsService.getStats(ADMIN_USER_ID);

        assertThat(stats.getTotalInquiries()).isEqualTo(3);
        assertThat(stats.getPendingInquiries()).isEqualTo(2);
        assertThat(stats.getCompletedInquiries()).isEqualTo(1);
    }

    @Test
    void returnsAllZerosWhenNoDataExists() {
        AdminStatsResponse stats = adminStatsService.getStats(ADMIN_USER_ID);

        assertThat(stats.getTotalApplications()).isZero();
        assertThat(stats.getIndividualApplications()).isZero();
        assertThat(stats.getGroupApplications()).isZero();
        assertThat(stats.getTotalInquiries()).isZero();
        assertThat(stats.getPendingInquiries()).isZero();
        assertThat(stats.getCompletedInquiries()).isZero();
    }
}
