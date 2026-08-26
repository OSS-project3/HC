package com.example.honorcitizen.infra.geocoding;

import java.time.Instant;
import java.util.List;

/**
 * 출생지역(도시명) → 좌표 → IANA timezoneId 조회를 추상화한다(admin-saju.md "출생지역 해석 정책").
 * 공급자는 설정으로 교체 가능해야 하며, 이 인터페이스의 반환 타입에는 특정 공급자의 응답 객체를
 * 그대로 노출하지 않는다.
 */
public interface BirthRegionLookupClient {

    /** 지명 검색 → 후보 목록. 검색 결과가 없으면 빈 리스트를 반환한다(예외 아님 — 호출부가 REGION_NOT_FOUND로 변환). */
    List<RegionCandidate> searchRegion(String query);

    /**
     * 좌표 + 대략적인 시점(historical DST 판정에 필요) → IANA timezoneId.
     * approxInstant는 출생일 근방이면 충분하다 — 실제 DST/offset 확정은 이 timezoneId를 받은 뒤
     * {@link com.example.honorcitizen.domain.manseryeok.service.BirthTimeZoneResolver}가 담당한다.
     */
    String resolveTimezoneId(double latitude, double longitude, Instant approxInstant);
}
