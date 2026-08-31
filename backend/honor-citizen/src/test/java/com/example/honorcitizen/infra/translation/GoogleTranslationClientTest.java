package com.example.honorcitizen.infra.translation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// API 키 미설정 시 원문 패스스루만 검증한다(GOOGLE_TRANSLATE_API_KEY 없이도 통과해야 한다).
// 실제 Google API 호출은 실 네트워크·유료 키가 필요해 이 스위트에서 다루지 않는다.
class GoogleTranslationClientTest {

    private final TranslationClient client = new GoogleTranslationClient(
            "", "https://translation.googleapis.com");

    @Test
    void returnsOriginalsWhenApiKeyBlank() {
        List<String> texts = List.of("안녕하세요", "명예시민증");

        assertThat(client.translate(texts)).isEqualTo(texts);
    }

    @Test
    void returnsEmptyListForEmptyInput() {
        assertThat(client.translate(List.of())).isEmpty();
        assertThat(client.translate(null)).isEmpty();
    }
}
