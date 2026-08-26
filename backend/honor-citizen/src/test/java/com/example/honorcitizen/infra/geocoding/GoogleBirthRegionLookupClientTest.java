package com.example.honorcitizen.infra.geocoding;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// API 키 미설정 시 호출을 막는 가드만 검증한다(GOOGLE_MAPS_API_KEY 없이도 이 테스트는 통과해야 한다).
// 실제 Google API 호출은 실 네트워크·유료 키가 필요해 이 스위트에서 다루지 않는다.
class GoogleBirthRegionLookupClientTest {

    private final BirthRegionLookupClient client = new GoogleBirthRegionLookupClient(
            "", "https://maps.googleapis.com/maps/api");

    @Test
    void searchRegionRejectsWhenApiKeyBlank() {
        assertThatThrownBy(() -> client.searchRegion("Chicago"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GEOCODING_NOT_CONFIGURED);
    }

    @Test
    void resolveTimezoneIdRejectsWhenApiKeyBlank() {
        assertThatThrownBy(() -> client.resolveTimezoneId(41.8781, -87.6298, Instant.now()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GEOCODING_NOT_CONFIGURED);
    }
}
