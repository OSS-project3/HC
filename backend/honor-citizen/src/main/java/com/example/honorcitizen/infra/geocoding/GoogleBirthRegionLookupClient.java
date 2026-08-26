package com.example.honorcitizen.infra.geocoding;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

/**
 * Google Geocoding API + Time Zone API를 사용한 {@link BirthRegionLookupClient} 구현.
 *
 * API 키는 {@code app.google-maps.api-key}(환경변수 GOOGLE_MAPS_API_KEY)로 주입되며, 비어 있으면
 * 기동은 정상적으로 되지만 실제 호출 시 {@link ErrorCode#GEOCODING_NOT_CONFIGURED}를 던진다 —
 * 키 발급 전에도 나머지 기능·테스트가 막히지 않도록 하기 위함이다.
 */
@Slf4j
@Component
class GoogleBirthRegionLookupClient implements BirthRegionLookupClient {

    private final RestClient restClient;
    private final String apiKey;

    // RestClient.Builder를 빈으로 주입받지 않고 직접 만든다 — 이 프로젝트는 Jackson 3(tools.jackson)로
    // 옮겨가 있어 Spring Boot의 RestClientAutoConfiguration이 기대하는 classic Jackson 2 databind가
    // 클래스패스에 없고, 그 결과 RestClient.Builder 빈 자체가 등록되지 않는다.
    GoogleBirthRegionLookupClient(
            @Value("${app.google-maps.api-key:}") String apiKey,
            @Value("${app.google-maps.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public List<RegionCandidate> searchRegion(String query) {
        requireConfigured();
        GeocodingResponse response = callOrFail(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/geocode/json")
                        .queryParam("address", query)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(GeocodingResponse.class));

        if (response == null) {
            return List.of();
        }
        if ("ZERO_RESULTS".equals(response.status())) {
            return List.of();
        }
        if (!"OK".equals(response.status())) {
            log.warn("Google Geocoding API 비정상 status: {}", response.status());
            throw new CustomException(ErrorCode.GEOCODING_PROVIDER_ERROR);
        }
        return response.results().stream()
                .filter(result -> result.geometry() != null && result.geometry().location() != null)
                .map(result -> new RegionCandidate(
                        result.formattedAddress(),
                        result.geometry().location().lat(),
                        result.geometry().location().lng()))
                .toList();
    }

    @Override
    public String resolveTimezoneId(double latitude, double longitude, Instant approxInstant) {
        requireConfigured();
        TimeZoneResponse response = callOrFail(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/timezone/json")
                        .queryParam("location", latitude + "," + longitude)
                        .queryParam("timestamp", approxInstant.getEpochSecond())
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(TimeZoneResponse.class));

        if (response == null || !StringUtils.hasText(response.timeZoneId())) {
            if (response != null) {
                log.warn("Google Time Zone API 비정상 status: {}", response.status());
            }
            throw new CustomException(ErrorCode.GEOCODING_PROVIDER_ERROR);
        }
        return response.timeZoneId();
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(apiKey)) {
            throw new CustomException(ErrorCode.GEOCODING_NOT_CONFIGURED);
        }
    }

    private <T> T callOrFail(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientException e) {
            log.warn("Google Maps API 호출 실패", e);
            throw new CustomException(ErrorCode.GEOCODING_PROVIDER_ERROR);
        }
    }

    private record GeocodingResponse(List<GeocodingResult> results, String status) {
    }

    private record GeocodingResult(
            @JsonProperty("formatted_address") String formattedAddress,
            Geometry geometry) {
    }

    private record Geometry(Location location) {
    }

    private record Location(double lat, double lng) {
    }

    private record TimeZoneResponse(
            @JsonProperty("timeZoneId") String timeZoneId,
            String status) {
    }
}
