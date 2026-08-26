package com.example.honorcitizen.domain.manseryeok.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 출생 현지시각 + IANA timezoneId → 절대 시점(utcInstant) 확정을 전담한다(admin-saju.md "Spring 백엔드" 책임).
 *
 * 순수 java.time 로직만 사용하며 외부 서비스·DB에 의존하지 않는다 — timezoneId 자체를 알아내는 지오코딩은
 * {@link com.example.honorcitizen.infra.geocoding.BirthRegionLookupClient}가 별도로 담당한다.
 *
 * {@code LocalDateTime.atZone(zoneId)}를 검증 없이 바로 쓰지 않는 이유: DST 전환 경계에서 그 결과가
 * "존재하지 않는 시각"을 다음 유효 시각으로 조용히 밀어버리거나, "중복되는 시각"의 두 offset 중 하나를
 * 암묵적으로 골라버려서 실제로 어떤 상황인지 알 수 없게 된다. {@link ZoneId#getRules()}의
 * {@code getValidOffsets(LocalDateTime)}로 후보 offset 개수를 먼저 확인해야 한다(0=GAP, 1=정상, 2=OVERLAP).
 */
@Component
public class BirthTimeZoneResolver {

    public BirthTimeResolution resolve(LocalDate birthDate, LocalTime birthTime, String timezoneId) {
        ZoneId zoneId = parseZoneId(timezoneId);

        if (birthTime == null) {
            return new BirthTimeResolution(
                    BirthTimeResolutionStatus.UNKNOWN_TIME, timezoneId, null, null, List.of());
        }

        LocalDateTime localDateTime = LocalDateTime.of(birthDate, birthTime);
        List<ZoneOffset> validOffsets = zoneId.getRules().getValidOffsets(localDateTime);

        if (validOffsets.size() == 1) {
            ZoneOffset offset = validOffsets.get(0);
            return new BirthTimeResolution(BirthTimeResolutionStatus.EXACT, timezoneId,
                    offset.getId(), localDateTime.toInstant(offset), List.of());
        }
        if (validOffsets.isEmpty()) {
            return new BirthTimeResolution(
                    BirthTimeResolutionStatus.NONEXISTENT_LOCAL_TIME, timezoneId, null, null, List.of());
        }

        List<BirthTimeResolution.OffsetCandidate> candidates = validOffsets.stream()
                .map(offset -> new BirthTimeResolution.OffsetCandidate(offset.getId(), localDateTime.toInstant(offset)))
                .toList();
        return new BirthTimeResolution(
                BirthTimeResolutionStatus.AMBIGUOUS_LOCAL_TIME, timezoneId, null, null, candidates);
    }

    /**
     * AMBIGUOUS_LOCAL_TIME 상태에서 관리자가 후보 offset 중 하나를 선택했을 때 호출한다. 선택값이 실제로
     * 그 시각의 유효 후보인지 다시 계산해 재검증한 뒤에만 EXACT로 확정한다 — 클라이언트가 보낸 offset
     * 문자열을 그대로 신뢰하지 않는다.
     */
    public BirthTimeResolution confirmOffset(LocalDate birthDate, LocalTime birthTime, String timezoneId,
            String selectedOffsetId) {
        BirthTimeResolution resolution = resolve(birthDate, birthTime, timezoneId);
        if (resolution.status() != BirthTimeResolutionStatus.AMBIGUOUS_LOCAL_TIME) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        BirthTimeResolution.OffsetCandidate matched = resolution.candidates().stream()
                .filter(candidate -> candidate.offset().equals(selectedOffsetId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        return new BirthTimeResolution(BirthTimeResolutionStatus.EXACT, timezoneId,
                matched.offset(), matched.utcInstant(), List.of());
    }

    private ZoneId parseZoneId(String timezoneId) {
        try {
            return ZoneId.of(timezoneId);
        } catch (DateTimeException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
