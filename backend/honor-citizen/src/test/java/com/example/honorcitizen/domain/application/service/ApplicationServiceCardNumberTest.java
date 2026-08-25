package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.CardNumberBatchAssignRequest;
import com.example.honorcitizen.domain.application.dto.CardNumberBatchAssignResponse;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 관리자 카드번호 개별·일괄 저장(1-C) — 서버 채번 없음, 사진 번호 매칭, all-or-nothing, 잠금 이후 변경 금지.
@SpringBootTest
class ApplicationServiceCardNumberTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private Long adminId;
    private Long applicationId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("card-number-admin@example.com", "oauth-card-number-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("card-number-owner@example.com", "oauth-card-number-owner", "google", "Owner"));
        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-cardnum", null, BigDecimal.valueOf(30000)));
        Application application = applicationRepository.save(Application.createIndividual(
                owner.getId(), "APP-2026-960001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicationId = application.getId();

        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                applicationId, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        memberId = member.getId();
    }

    // ── 개인/단일 Member ──────────────────────────────────────────────

    @Test
    void assignsValidCardNumber() {
        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-12345-6789");

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getCardNumber()).isEqualTo("ROK-12345-6789");
    }

    @Test
    void reassigningSameValueIsIdempotent() {
        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-12345-6789");
        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-12345-6789");

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getCardNumber()).isEqualTo("ROK-12345-6789");
    }

    @Test
    void allowsChangingToADifferentValueBeforeCardGenerated() {
        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-12345-6789");
        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-99999-0000");

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getCardNumber()).isEqualTo("ROK-99999-0000");
    }

    @Test
    void rejectsChangeAfterCardAlreadyGenerated() {
        ApplicationMember member = applicationMemberRepository.findById(memberId).orElseThrow();
        member.assignCardNumber("ROK-12345-6789");
        ReflectionTestUtils.setField(member, "cardFrontPath", "cards/front.png");
        applicationMemberRepository.saveAndFlush(member);

        assertThatThrownBy(() -> applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-99999-0000"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_LOCKED);
    }

    @Test
    void allowsIdempotentSameValueAfterCardGenerated() {
        ApplicationMember member = applicationMemberRepository.findById(memberId).orElseThrow();
        member.assignCardNumber("ROK-12345-6789");
        ReflectionTestUtils.setField(member, "cardFrontPath", "cards/front.png");
        applicationMemberRepository.saveAndFlush(member);

        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-12345-6789");

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getCardNumber()).isEqualTo("ROK-12345-6789");
    }

    @Test
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> applicationService.assignCardNumber(adminId, applicationId, memberId, "12345-6789"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsWhenMemberDoesNotBelongToApplication() {
        Application other = applicationRepository.save(Application.createIndividual(
                applicationRepository.findById(applicationId).orElseThrow().getUserId(),
                "APP-2026-960002", cardTypeRepository.findAll().get(0).getId(), IssueType.MOBILE, true, null, null));

        assertThatThrownBy(() -> applicationService.assignCardNumber(adminId, other.getId(), memberId, "ROK-12345-6789"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsDuplicateCardNumberAcrossMembers() {
        ApplicationMember other = applicationMemberRepository.save(ApplicationMember.createIndividual(
                applicationId, "Other Person", LocalDate.of(1991, 1, 1), "US",
                null, null, Gender.FEMALE, null, null, null, "photos/b.jpg"));
        applicationService.assignCardNumber(adminId, applicationId, memberId, "ROK-12345-6789");

        assertThatThrownBy(() -> applicationService.assignCardNumber(adminId, applicationId, other.getId(), "ROK-12345-6789"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_ALREADY_USED);
    }

    @Test
    void rejectsForNonAdminCaller() {
        User user = userRepository.save(
                User.createOAuthUser("card-number-plain@example.com", "oauth-card-number-plain", "google", "User"));

        assertThatThrownBy(() -> applicationService.assignCardNumber(user.getId(), applicationId, memberId, "ROK-12345-6789"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // ── 단체 일괄 ──────────────────────────────────────────────────

    private ApplicationMember addMemberWithPhotoNumber(String photoNumber) {
        ApplicationMember member = ApplicationMember.createGroupRow(
                applicationId, "Member " + photoNumber, LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, "member" + photoNumber + "@example.com", "010-0000-0000",
                "Seoul", null, null, "photos/" + photoNumber + ".jpg", photoNumber);
        return applicationMemberRepository.save(member);
    }

    private CardNumberBatchAssignRequest batchRequest(long version, String json) throws Exception {
        String full = "{\"applicationVersion\":" + version + ",\"items\":" + json + "}";
        return objectMapper.readValue(full, CardNumberBatchAssignRequest.class);
    }

    @Test
    void batchAssignsBySpanningPhotoNumberNotMemberIdOrder() throws Exception {
        ApplicationMember m1 = addMemberWithPhotoNumber("001");
        ApplicationMember m2 = addMemberWithPhotoNumber("002");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();

        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"002\",\"cardNumber\":\"ROK-00000-0002\"},"
                        + "{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"}]");

        CardNumberBatchAssignResponse response = applicationService.assignCardNumbersBatch(adminId, applicationId, request);

        assertThat(response.getUpdatedCount()).isEqualTo(2);
        assertThat(applicationMemberRepository.findById(m1.getId()).orElseThrow().getCardNumber()).isEqualTo("ROK-00000-0001");
        assertThat(applicationMemberRepository.findById(m2.getId()).orElseThrow().getCardNumber()).isEqualTo("ROK-00000-0002");
    }

    @Test
    void batchAllowsPartialMemberListInOneRequest() throws Exception {
        addMemberWithPhotoNumber("001");
        addMemberWithPhotoNumber("002");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();

        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"}]");

        CardNumberBatchAssignResponse response = applicationService.assignCardNumbersBatch(adminId, applicationId, request);

        assertThat(response.getUpdatedCount()).isEqualTo(1);
    }

    @Test
    void batchRejectsVersionConflict() throws Exception {
        addMemberWithPhotoNumber("001");
        long staleVersion = applicationRepository.findById(applicationId).orElseThrow().getVersion() + 999;

        CardNumberBatchAssignRequest request = batchRequest(staleVersion,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"}]");

        assertThatThrownBy(() -> applicationService.assignCardNumbersBatch(adminId, applicationId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_VERSION_CONFLICT);
    }

    @Test
    void batchRejectsDuplicatePhotoNumberWithinRequestAndWritesNothing() throws Exception {
        addMemberWithPhotoNumber("001");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();

        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"},"
                        + "{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0002\"}]");

        assertThatThrownBy(() -> applicationService.assignCardNumbersBatch(adminId, applicationId, request))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_VALIDATION_FAILED);
        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(applicationId);
        assertThat(members).allSatisfy(m -> assertThat(m.getCardNumber()).isNull());
    }

    @Test
    void batchRejectsUnknownPhotoNumber() throws Exception {
        addMemberWithPhotoNumber("001");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();

        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"999\",\"cardNumber\":\"ROK-00000-0001\"}]");

        assertThatThrownBy(() -> applicationService.assignCardNumbersBatch(adminId, applicationId, request))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_VALIDATION_FAILED);
    }

    @Test
    void batchRejectsInvalidCardNumberFormat() throws Exception {
        addMemberWithPhotoNumber("001");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();

        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"bad-format\"}]");

        assertThatThrownBy(() -> applicationService.assignCardNumbersBatch(adminId, applicationId, request))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_VALIDATION_FAILED);
    }

    @Test
    void batchRejectsEntireRequestWhenOneMemberIsLockedAndValueDiffers() throws Exception {
        ApplicationMember m1 = addMemberWithPhotoNumber("001");
        addMemberWithPhotoNumber("002");
        m1.assignCardNumber("ROK-00000-0001");
        ReflectionTestUtils.setField(m1, "cardFrontPath", "cards/front.png");
        applicationMemberRepository.saveAndFlush(m1);
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();

        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-99999-9999\"},"
                        + "{\"photoNumber\":\"002\",\"cardNumber\":\"ROK-00000-0002\"}]");

        assertThatThrownBy(() -> applicationService.assignCardNumbersBatch(adminId, applicationId, request))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_VALIDATION_FAILED);
        // 잠기지 않은 002도 이번 요청에 함께 있었으므로 전체 거절과 함께 반영되지 않아야 한다.
        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(applicationId);
        ApplicationMember reloaded002 = members.stream().filter(m -> "002".equals(m.getPhotoNumber())).findFirst().orElseThrow();
        assertThat(reloaded002.getCardNumber()).isNull();
    }

    @Test
    void batchReassigningSameValuesIsIdempotent() throws Exception {
        addMemberWithPhotoNumber("001");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();
        CardNumberBatchAssignRequest first = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"}]");
        applicationService.assignCardNumbersBatch(adminId, applicationId, first);

        CardNumberBatchAssignRequest second = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"}]");
        CardNumberBatchAssignResponse response = applicationService.assignCardNumbersBatch(adminId, applicationId, second);

        assertThat(response.getUpdatedCount()).isEqualTo(1);
    }

    @Test
    void batchRejectsForNonAdminCaller() throws Exception {
        addMemberWithPhotoNumber("001");
        long version = applicationRepository.findById(applicationId).orElseThrow().getVersion();
        User user = userRepository.save(
                User.createOAuthUser("card-number-batch-plain@example.com", "oauth-card-number-batch-plain", "google", "User"));
        CardNumberBatchAssignRequest request = batchRequest(version,
                "[{\"photoNumber\":\"001\",\"cardNumber\":\"ROK-00000-0001\"}]");

        assertThatThrownBy(() -> applicationService.assignCardNumbersBatch(user.getId(), applicationId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
