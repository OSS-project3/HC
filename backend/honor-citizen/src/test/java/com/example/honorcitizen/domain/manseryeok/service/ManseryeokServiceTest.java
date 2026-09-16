package com.example.honorcitizen.domain.manseryeok.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokConfirmRequest;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokResolveRequest;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokResolveResponse;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.geocoding.BirthRegionLookupClient;
import com.example.honorcitizen.infra.geocoding.RegionCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

// 1-D: 관리자 만세력 확정 결과 저장 계약 — resolve(미리보기, DB 무변경)와 confirm(이력 보존 저장) 검증.
// BirthRegionLookupClient는 실제 Google 호출 없이 Mockito로 대체한다(키 없이도 테스트 가능).
@SpringBootTest
class ManseryeokServiceTest {

    @Autowired
    private ManseryeokService manseryeokService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private ManseryeokResultRepository manseryeokResultRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BirthRegionLookupClient birthRegionLookupClient;

    private Long adminId;
    private Long applicationId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        manseryeokResultRepository.deleteAll();
        adminActivityLogRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("manseryeok-admin@example.com", "oauth-manseryeok-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("manseryeok-owner@example.com", "oauth-manseryeok-owner", "google", "Owner"));
        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-manseryeok", null, BigDecimal.valueOf(30000)));
        Application application = applicationRepository.save(Application.createIndividual(
                owner.getId(), "APP-2026-970001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicationId = application.getId();

        // 2000-10-29 01:30 America/New_York — admin-saju.md 예시와 동일한 AMBIGUOUS_LOCAL_TIME 케이스.
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                applicationId, "Hong Gildong", LocalDate.of(2000, 10, 29), "US",
                LocalTime.of(1, 30), "New York", Gender.MALE, null, null, null, "photos/a.jpg"));
        memberId = member.getId();
    }

    @Test
    void resolveReturnsAmbiguousCandidatesWithoutTouchingDb() {
        ManseryeokResolveRequest request = resolveRequest(40.7128, -74.0060, "America/New_York", null);

        ManseryeokResolveResponse response = manseryeokService.resolveBirthTime(adminId, applicationId, memberId, request);

        assertThat(response.getStatus()).isEqualTo(BirthTimeResolutionStatus.AMBIGUOUS_LOCAL_TIME);
        assertThat(response.getCandidates()).hasSize(2);
        assertThat(manseryeokResultRepository.count()).isZero();
    }

    @Test
    void resolveCallsGeocodingClientWhenTimezoneIdMissing() {
        when(birthRegionLookupClient.resolveTimezoneId(anyDouble(), anyDouble(), any(Instant.class)))
                .thenReturn("America/New_York");
        ManseryeokResolveRequest request = resolveRequest(40.7128, -74.0060, null, null);

        ManseryeokResolveResponse response = manseryeokService.resolveBirthTime(adminId, applicationId, memberId, request);

        assertThat(response.getTimezoneId()).isEqualTo("America/New_York");
        assertThat(response.getStatus()).isEqualTo(BirthTimeResolutionStatus.AMBIGUOUS_LOCAL_TIME);
    }

    @Test
    void resolveConfirmsOffsetWhenSelectedOffsetProvided() {
        ManseryeokResolveRequest request = resolveRequest(40.7128, -74.0060, "America/New_York", "-04:00");

        ManseryeokResolveResponse response = manseryeokService.resolveBirthTime(adminId, applicationId, memberId, request);

        assertThat(response.getStatus()).isEqualTo(BirthTimeResolutionStatus.EXACT);
        assertThat(response.getUtcInstant()).isEqualTo(Instant.parse("2000-10-29T05:30:00Z"));
    }

    @Test
    void confirmStoresExactResultWhenIntegrityMatches() throws Exception {
        ManseryeokConfirmRequest request = confirmRequest(
                "America/New_York", "-04:00", Instant.parse("2000-10-29T05:30:00Z"), TimeAccuracy.EXACT);

        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, request);

        ManseryeokResult saved = manseryeokResultRepository.findByApplicationMemberIdAndActiveTrue(memberId).orElseThrow();
        assertThat(saved.getTimezoneId()).isEqualTo("America/New_York");
        assertThat(saved.getSelectedOffset()).isEqualTo("-04:00");
        assertThat(saved.getUtcInstant()).isEqualTo(Instant.parse("2000-10-29T05:30:00Z"));
        assertThat(saved.isActive()).isTrue();
        assertThat(adminActivityLogRepository.findAll()).hasSize(1);
    }

    @Test
    void confirmRejectsExactResultWhenUtcInstantDoesNotMatchRecomputedOffset() throws Exception {
        // -04:00 offset이면 05:30Z여야 하는데 엉뚱한 시각을 보냄 — 무결성 불일치.
        ManseryeokConfirmRequest request = confirmRequest(
                "America/New_York", "-04:00", Instant.parse("1999-01-01T00:00:00Z"), TimeAccuracy.EXACT);

        assertThatThrownBy(() -> manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThat(manseryeokResultRepository.count()).isZero();
    }

    @Test
    void confirmPreservesHistoryAndKeepsOnlyOneActiveResult() throws Exception {
        ManseryeokConfirmRequest first = confirmRequest(
                "America/New_York", "-04:00", Instant.parse("2000-10-29T05:30:00Z"), TimeAccuracy.EXACT);
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, first);

        ManseryeokConfirmRequest second = confirmRequest(
                "America/New_York", "-05:00", Instant.parse("2000-10-29T06:30:00Z"), TimeAccuracy.EXACT);
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, second);

        List<ManseryeokResult> all = manseryeokResultRepository.findByApplicationMemberIdOrderByCalculatedAtDesc(memberId);
        assertThat(all).hasSize(2);
        assertThat(all).filteredOn(ManseryeokResult::isActive).hasSize(1)
                .first().extracting(ManseryeokResult::getSelectedOffset).isEqualTo("-05:00");
    }

    @Test
    void confirmStoresPartialResultWithoutIntegrityCheck() throws Exception {
        ManseryeokConfirmRequest request = confirmRequest("America/New_York", null, null, TimeAccuracy.PARTIAL);

        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, request);

        ManseryeokResult saved = manseryeokResultRepository.findByApplicationMemberIdAndActiveTrue(memberId).orElseThrow();
        assertThat(saved.getTimeAccuracy()).isEqualTo(TimeAccuracy.PARTIAL);
        assertThat(saved.getUtcInstant()).isNull();
    }

    @Test
    void getActiveResultReturnsParsedPillarsAndElementCounts() throws Exception {
        ManseryeokConfirmRequest request = confirmRequest(
                "America/New_York", "-04:00", Instant.parse("2000-10-29T05:30:00Z"), TimeAccuracy.EXACT);
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, request);

        var response = manseryeokService.getActiveManseryeokResult(adminId, applicationId, memberId);

        assertThat(response.getTimezoneId()).isEqualTo("America/New_York");
        assertThat(response.getConfirmedPillars().get("year").get("stem")).isEqualTo("갑");
        assertThat(response.getElementCounts().get("목")).isEqualTo(2);
    }

    @Test
    void getActiveResultReturnsNotFoundWhenNoneStored() {
        assertThatThrownBy(() -> manseryeokService.getActiveManseryeokResult(adminId, applicationId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsWhenMemberDoesNotBelongToApplication() {
        Application other = applicationRepository.save(Application.createIndividual(
                applicationRepository.findById(applicationId).orElseThrow().getUserId(),
                "APP-2026-970002", cardTypeRepository.findAll().get(0).getId(), IssueType.MOBILE, true, null, null));
        ManseryeokResolveRequest request = resolveRequest(40.7128, -74.0060, "America/New_York", "-04:00");

        assertThatThrownBy(() -> manseryeokService.resolveBirthTime(adminId, other.getId(), memberId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsForNonAdminCaller() {
        User user = userRepository.save(
                User.createOAuthUser("manseryeok-plain@example.com", "oauth-manseryeok-plain", "google", "User"));
        ManseryeokResolveRequest request = resolveRequest(40.7128, -74.0060, "America/New_York", null);

        assertThatThrownBy(() -> manseryeokService.resolveBirthTime(user.getId(), applicationId, memberId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void listActiveResultsReturnsOnlyMembersWithActiveResult() throws Exception {
        // 같은 신청에 멤버 1명 추가(확정 안 함) — 확정된 멤버만 응답에 포함돼야 한다.
        ApplicationMember unconfirmed = applicationMemberRepository.save(ApplicationMember.createIndividual(
                applicationId, "No Result", LocalDate.of(1999, 1, 1), "US",
                LocalTime.of(10, 0), "Boston", Gender.FEMALE, null, null, null, "photos/b.jpg"));
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, confirmRequest(
                "America/New_York", "-04:00", Instant.parse("2000-10-29T05:30:00Z"), TimeAccuracy.EXACT));

        var results = manseryeokService.listActiveManseryeokResults(adminId, applicationId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMemberId()).isEqualTo(memberId);
        assertThat(results.get(0).getResult().getConfirmedPillars().get("year").get("stem")).isEqualTo("갑");
        assertThat(results).noneMatch(r -> r.getMemberId().equals(unconfirmed.getId()));
    }

    @Test
    void listActiveResultsExcludesInactiveHistory() throws Exception {
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, confirmRequest(
                "America/New_York", "-04:00", Instant.parse("2000-10-29T05:30:00Z"), TimeAccuracy.EXACT));
        // 재확정 — 기존 활성 결과는 비활성 이력으로 남고, 응답에는 최신 활성 1건만 나와야 한다.
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, confirmRequest(
                "America/New_York", "-05:00", Instant.parse("2000-10-29T06:30:00Z"), TimeAccuracy.EXACT));

        var results = manseryeokService.listActiveManseryeokResults(adminId, applicationId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResult().getSelectedOffset()).isEqualTo("-05:00");
    }

    @Test
    void listActiveResultsRejectsUnknownApplication() {
        assertThatThrownBy(() -> manseryeokService.listActiveManseryeokResults(adminId, 999_999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void listActiveResultsExcludesOtherApplicationMembers() throws Exception {
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, confirmRequest(
                "America/New_York", "-04:00", Instant.parse("2000-10-29T05:30:00Z"), TimeAccuracy.EXACT));
        Application other = applicationRepository.save(Application.createIndividual(
                applicationRepository.findById(applicationId).orElseThrow().getUserId(),
                "APP-2026-970003", cardTypeRepository.findAll().get(0).getId(), IssueType.MOBILE, true, null, null));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                other.getId(), "Other App", LocalDate.of(1998, 2, 2), "US",
                LocalTime.of(9, 0), "Chicago", Gender.MALE, null, null, null, "photos/c.jpg"));

        var results = manseryeokService.listActiveManseryeokResults(adminId, other.getId());

        assertThat(results).isEmpty();
    }

    @Test
    void listActiveResultsRejectsNonAdminCaller() {
        User user = userRepository.save(
                User.createOAuthUser("manseryeok-bulk-plain@example.com", "oauth-manseryeok-bulk-plain", "google", "User"));

        assertThatThrownBy(() -> manseryeokService.listActiveManseryeokResults(user.getId(), applicationId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    private ManseryeokResolveRequest resolveRequest(double lat, double lng, String timezoneId, String selectedOffset) {
        String json = """
                {
                  "latitude": %s,
                  "longitude": %s
                  %s
                  %s
                }
                """.formatted(lat, lng,
                timezoneId == null ? "" : ", \"timezoneId\": \"%s\"".formatted(timezoneId),
                selectedOffset == null ? "" : ", \"selectedOffset\": \"%s\"".formatted(selectedOffset));
        return objectMapper.readValue(json, ManseryeokResolveRequest.class);
    }

    private ManseryeokConfirmRequest confirmRequest(String timezoneId, String selectedOffset, Instant utcInstant,
            TimeAccuracy timeAccuracy) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("timezoneId", timezoneId);
        body.put("longitude", -74.0060);
        body.put("selectedOffset", selectedOffset);
        body.put("utcInstant", utcInstant == null ? null : utcInstant.toString());
        body.put("timeAccuracy", timeAccuracy.name());
        body.put("confirmedPillars", Map.of("year", Map.of("stem", "갑", "branch", "자")));
        body.put("uncertainPillars", List.of());
        body.put("elementCounts", Map.of("목", 2, "화", 1, "토", 1, "금", 2, "수", 2));
        body.put("calculationEngineVersion", "manseryeok@1.0.0");
        body.put("inputHash", "test-hash");
        return objectMapper.readValue(objectMapper.writeValueAsString(body), ManseryeokConfirmRequest.class);
    }
}
