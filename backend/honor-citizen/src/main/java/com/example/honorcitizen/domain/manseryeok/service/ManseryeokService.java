package com.example.honorcitizen.domain.manseryeok.service;

import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.manseryeok.dto.BirthRegionCandidateResponse;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokActiveResultResponse;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokConfirmRequest;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokResolveRequest;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokResolveResponse;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.geocoding.BirthRegionLookupClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 관리자 만세력 확정 결과 저장 계약(1-D, admin-saju.md). 세 축을 분리해 처리한다:
 *  1) 출생지역 검색(Google Geocoding) — DB 무관, 순수 조회.
 *  2) timezone/DST 판정(resolve) — {@link BirthTimeZoneResolver}로 utcInstant까지 확정하되 저장하지 않는다(미리보기).
 *  3) 확정 결과 저장(confirm) — 프론트가 계산한 사주 결과를 받아 무결성만 재검증하고 이력을 보존해 저장한다.
 */
@Service
@RequiredArgsConstructor
public class ManseryeokService {

    private final UserService userService;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final ManseryeokResultRepository manseryeokResultRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;
    private final BirthRegionLookupClient birthRegionLookupClient;
    private final BirthTimeZoneResolver birthTimeZoneResolver;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<BirthRegionCandidateResponse> searchBirthRegion(Long adminId, String query) {
        validateAdmin(adminId);
        return birthRegionLookupClient.searchRegion(query).stream()
                .map(c -> new BirthRegionCandidateResponse(c.displayName(), c.latitude(), c.longitude()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ManseryeokResolveResponse resolveBirthTime(Long adminId, Long applicationId, Long memberId,
            ManseryeokResolveRequest request) {
        validateAdmin(adminId);
        ApplicationMember member = findMember(applicationId, memberId);

        String timezoneId = request.getTimezoneId();
        if (timezoneId == null || timezoneId.isBlank()) {
            Instant approxInstant = member.getBirthDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            timezoneId = birthRegionLookupClient.resolveTimezoneId(
                    request.getLatitude(), request.getLongitude(), approxInstant);
        }

        BirthTimeResolution resolution = request.getSelectedOffset() != null && !request.getSelectedOffset().isBlank()
                ? birthTimeZoneResolver.confirmOffset(
                        member.getBirthDate(), member.getBirthTime(), timezoneId, request.getSelectedOffset())
                : birthTimeZoneResolver.resolve(member.getBirthDate(), member.getBirthTime(), timezoneId);
        return ManseryeokResolveResponse.of(resolution, request.getLongitude());
    }

    @Transactional
    public void confirmManseryeokResult(Long adminId, Long applicationId, Long memberId,
            ManseryeokConfirmRequest request) {
        validateAdmin(adminId);
        ApplicationMember member = findMember(applicationId, memberId);

        // Spring이 검증 가능한 부분만 재확인한다 — timezoneId+생년월일시로 다시 계산했을 때 요청의
        // selectedOffset/utcInstant와 일치하는지. 실제 사주(pillars) 계산은 재현할 수 없어 그대로 저장한다.
        if (request.getTimeAccuracy() == TimeAccuracy.EXACT) {
            validateExactIntegrity(member, request);
        }

        manseryeokResultRepository.findByApplicationMemberIdAndActiveTrue(memberId)
                .ifPresent(ManseryeokResult::deactivate);

        ManseryeokResult result = ManseryeokResult.create(
                memberId,
                request.getInputHash(),
                request.getTimezoneId(),
                request.getLongitude(),
                request.getSelectedOffset(),
                request.getUtcInstant(),
                request.getTimeAccuracy(),
                writeJson(request.getConfirmedPillars()),
                writeJson(request.getUncertainPillars()),
                writeJson(request.getElementCounts()),
                ZoneRulesTzdbVersion.CURRENT,
                request.getCalculationEngineVersion(),
                LocalDateTime.now(),
                adminId);
        manseryeokResultRepository.save(result);

        adminActivityLogRepository.save(AdminActivityLog.create(adminId, AdminActivityLog.MANSERYEOK_CONFIRMED,
                applicationId, "memberId=" + memberId + " timeAccuracy=" + request.getTimeAccuracy()));
    }

    @Transactional(readOnly = true)
    public ManseryeokActiveResultResponse getActiveManseryeokResult(Long adminId, Long applicationId, Long memberId) {
        validateAdmin(adminId);
        findMember(applicationId, memberId);
        ManseryeokResult result = manseryeokResultRepository.findByApplicationMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return new ManseryeokActiveResultResponse(result,
                readJson(result.getConfirmedPillarsJson(), new TypeReference<Map<String, Map<String, String>>>() { }),
                readJson(result.getUncertainPillarsJson(), new TypeReference<List<String>>() { }),
                readJson(result.getElementCountsJson(), new TypeReference<Map<String, Integer>>() { }));
    }

    private void validateExactIntegrity(ApplicationMember member, ManseryeokConfirmRequest request) {
        if (request.getSelectedOffset() == null || request.getUtcInstant() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        BirthTimeResolution recomputed;
        try {
            recomputed = birthTimeZoneResolver.resolve(member.getBirthDate(), member.getBirthTime(), request.getTimezoneId());
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        boolean offsetMatches = request.getSelectedOffset().equals(recomputed.selectedOffset())
                || recomputed.candidates().stream().anyMatch(c -> c.offset().equals(request.getSelectedOffset())
                        && c.utcInstant().equals(request.getUtcInstant()));
        boolean instantMatches = recomputed.status() == BirthTimeResolutionStatus.EXACT
                ? request.getUtcInstant().equals(recomputed.utcInstant())
                : true;
        if (!offsetMatches || !instantMatches) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private ApplicationMember findMember(Long applicationId, Long memberId) {
        ApplicationMember member = applicationMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!member.getApplicationId().equals(applicationId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return member;
    }

    private void validateAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, typeReference);
    }

    // 계산 재현성 기록용 tzdb 버전 — JVM이 번들한 IANA tzdata 버전을 그대로 쓴다.
    private static final class ZoneRulesTzdbVersion {
        static final String CURRENT = resolveVersion();

        private static String resolveVersion() {
            try {
                return ZoneRulesProvider.getVersions("Asia/Seoul").keySet().stream()
                        .max(String::compareTo)
                        .orElse("unknown");
            } catch (ZoneRulesException | NoSuchElementException e) {
                return "unknown";
            }
        }
    }
}
