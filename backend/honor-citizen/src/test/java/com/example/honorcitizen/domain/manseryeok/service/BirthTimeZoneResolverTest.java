package com.example.honorcitizen.domain.manseryeok.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// admin-saju.md "DST 경계 정책" 구현 검증 — 순수 java.time 로직이라 외부 의존성 없이 전체 케이스를 고정한다.
class BirthTimeZoneResolverTest {

    private final BirthTimeZoneResolver resolver = new BirthTimeZoneResolver();

    @Test
    void resolvesExactForZoneWithoutDst() {
        // 서울은 DST가 없어 항상 단일 offset.
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(1990, 5, 15), LocalTime.of(14, 30), "Asia/Seoul");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.EXACT);
        assertThat(result.selectedOffset()).isEqualTo("+09:00");
        assertThat(result.utcInstant()).isEqualTo(Instant.parse("1990-05-15T05:30:00Z"));
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void resolvesExactForOrdinaryDaytimeInDstZone() {
        // 한여름 뉴욕 — DST 전환 근처가 아니므로 단일 offset(EDT, -04:00).
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(1990, 7, 15), LocalTime.of(10, 0), "America/New_York");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.EXACT);
        assertThat(result.selectedOffset()).isEqualTo("-04:00");
        assertThat(result.utcInstant()).isEqualTo(Instant.parse("1990-07-15T14:00:00Z"));
    }

    @Test
    void resolvesNonexistentAtDstStartGap() {
        // 2007-03-11 뉴욕 DST 시작: 02:00 → 03:00으로 건너뜀. 02:30은 존재하지 않는 시각.
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(2007, 3, 11), LocalTime.of(2, 30), "America/New_York");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.NONEXISTENT_LOCAL_TIME);
        assertThat(result.utcInstant()).isNull();
        assertThat(result.selectedOffset()).isNull();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void resolvesAmbiguousAtDstEndOverlapMatchingAdminSajuMdExample() {
        // admin-saju.md 예시 그대로: 2000-10-29 뉴욕 DST 종료, 01:30이 EDT/EST 두 번 존재.
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(2000, 10, 29), LocalTime.of(1, 30), "America/New_York");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.AMBIGUOUS_LOCAL_TIME);
        assertThat(result.utcInstant()).isNull();
        assertThat(result.selectedOffset()).isNull();
        assertThat(result.candidates()).hasSize(2);
        assertThat(result.candidates()).extracting(BirthTimeResolution.OffsetCandidate::offset)
                .containsExactlyInAnyOrder("-04:00", "-05:00");
        assertThat(result.candidates()).extracting(BirthTimeResolution.OffsetCandidate::utcInstant)
                .containsExactlyInAnyOrder(
                        Instant.parse("2000-10-29T05:30:00Z"),
                        Instant.parse("2000-10-29T06:30:00Z"));
    }

    @Test
    void confirmOffsetResolvesAmbiguousCandidateToExact() {
        BirthTimeResolution confirmed = resolver.confirmOffset(
                LocalDate.of(2000, 10, 29), LocalTime.of(1, 30), "America/New_York", "-04:00");

        assertThat(confirmed.status()).isEqualTo(BirthTimeResolutionStatus.EXACT);
        assertThat(confirmed.selectedOffset()).isEqualTo("-04:00");
        assertThat(confirmed.utcInstant()).isEqualTo(Instant.parse("2000-10-29T05:30:00Z"));
        assertThat(confirmed.candidates()).isEmpty();
    }

    @Test
    void confirmOffsetRejectsValueNotAmongCandidates() {
        assertThatThrownBy(() -> resolver.confirmOffset(
                LocalDate.of(2000, 10, 29), LocalTime.of(1, 30), "America/New_York", "+09:00"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void confirmOffsetRejectsWhenBaseResolutionIsNotAmbiguous() {
        assertThatThrownBy(() -> resolver.confirmOffset(
                LocalDate.of(1990, 5, 15), LocalTime.of(14, 30), "Asia/Seoul", "+09:00"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void resolvesUnknownWhenBirthTimeMissing() {
        BirthTimeResolution result = resolver.resolve(LocalDate.of(1990, 5, 15), null, "Asia/Seoul");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.UNKNOWN_TIME);
        assertThat(result.utcInstant()).isNull();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void resolvesSouthernHemisphereDstZoneCorrectly() {
        // 시드니 1월 = 남반구 여름 DST 적용 기간(+11:00).
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(1990, 1, 15), LocalTime.of(10, 0), "Australia/Sydney");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.EXACT);
        assertThat(result.selectedOffset()).isEqualTo("+11:00");
    }

    @Test
    void resolvesHistoricalPre1970DateCorrectly() {
        // 1965년(1970년 이전) 뉴욕 — tzdb의 역사적 데이터가 정상 반영되는지 확인.
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(1965, 6, 15), LocalTime.of(10, 0), "America/New_York");

        assertThat(result.status()).isEqualTo(BirthTimeResolutionStatus.EXACT);
        assertThat(result.selectedOffset()).isEqualTo("-04:00");
        assertThat(result.utcInstant()).isEqualTo(Instant.parse("1965-06-15T14:00:00Z"));
    }

    @Test
    void resolvesUtcDateChangeAcrossTimezoneCorrectly() {
        // 서울 00:30은 UTC로 변환하면 전날 15:30 — 날짜가 바뀌는 지역의 변환 정확성 확인.
        BirthTimeResolution result = resolver.resolve(
                LocalDate.of(1990, 5, 15), LocalTime.of(0, 30), "Asia/Seoul");

        assertThat(result.utcInstant()).isEqualTo(Instant.parse("1990-05-14T15:30:00Z"));
    }

    @Test
    void rejectsInvalidTimezoneId() {
        assertThatThrownBy(() -> resolver.resolve(LocalDate.of(1990, 5, 15), LocalTime.of(10, 0), "Not/AZone"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
