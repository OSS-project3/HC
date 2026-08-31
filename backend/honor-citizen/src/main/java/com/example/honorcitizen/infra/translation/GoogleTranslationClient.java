package com.example.honorcitizen.infra.translation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Cloud Translation v2를 사용한 {@link TranslationClient} 구현(ko→en).
 *
 * API 키는 {@code app.translation.api-key}(환경변수 GOOGLE_TRANSLATE_API_KEY)로 주입되며,
 * 비어 있으면 원문을 그대로 반환한다 — Geocoding과 달리 예외조차 던지지 않는다. 번역은
 * 콘텐츠 조회의 부가 기능이라 키 발급 전에도(그리고 API 장애 중에도) 조회 자체는 항상
 * 한국어 원문으로 성공해야 하기 때문이다.
 */
@Slf4j
@Component
class GoogleTranslationClient implements TranslationClient {

    private final RestClient restClient;
    private final String apiKey;

    // RestClient.Builder를 빈으로 주입받지 않고 직접 만든다 — 이 프로젝트는 Jackson 3(tools.jackson)로
    // 옮겨가 있어 RestClient.Builder 빈이 등록되지 않는다(GoogleBirthRegionLookupClient와 동일 사정).
    GoogleTranslationClient(
            @Value("${app.translation.api-key:}") String apiKey,
            @Value("${app.translation.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public List<String> translate(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (!StringUtils.hasText(apiKey)) {
            return texts;
        }
        try {
            // 본문(content)이 길 수 있어 쿼리스트링 대신 form 바디로 보낸다(v2는 GET/POST 모두 지원).
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            texts.forEach(text -> form.add("q", text == null ? "" : text));
            form.add("source", "ko");
            form.add("target", "en");
            form.add("format", "text");

            TranslateResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/language/translate/v2")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TranslateResponse.class);

            if (response == null || response.data() == null || response.data().translations() == null
                    || response.data().translations().size() != texts.size()) {
                log.warn("Google Translation API 응답 형식이 예상과 다름 — 원문을 그대로 반환한다");
                return texts;
            }
            List<String> translated = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                String text = response.data().translations().get(i).translatedText();
                // format=text라 HTML 엔티티가 없어야 정상이지만, 방어적으로 디코딩해 둔다.
                translated.add(StringUtils.hasText(text) ? HtmlUtils.htmlUnescape(text) : texts.get(i));
            }
            return translated;
        } catch (RestClientException e) {
            log.warn("Google Translation API 호출 실패 — 원문을 그대로 반환한다", e);
            return texts;
        }
    }

    private record TranslateResponse(TranslateData data) {
    }

    private record TranslateData(List<Translation> translations) {
    }

    private record Translation(String translatedText) {
    }
}
