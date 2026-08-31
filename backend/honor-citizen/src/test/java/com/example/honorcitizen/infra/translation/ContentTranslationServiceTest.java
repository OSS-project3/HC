package com.example.honorcitizen.infra.translation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 캐시 계층(인메모리→DB→API 배치 호출) 동작 검증. 인메모리 캐시는 컨텍스트 생애 동안 공유되므로
// 테스트마다 서로 다른 원문 문자열을 써서 교차 오염을 피한다.
@SpringBootTest
class ContentTranslationServiceTest {

    @Autowired
    private ContentTranslationService contentTranslationService;
    @Autowired
    private TranslationCacheRepository translationCacheRepository;

    @MockitoBean
    private TranslationClient translationClient;

    @Test
    void skipsNullBlankAndNonHangulWithoutCallingClient() {
        List<String> result = contentTranslationService.toEnglish(
                Arrays.asList(null, "", "   ", "Hello world", "12345!"));

        assertThat(result).containsExactly(null, "", "   ", "Hello world", "12345!");
        verify(translationClient, never()).translate(anyList());
    }

    @Test
    void translatesAllHangulStringsInOneBatchPreservingOrder() {
        when(translationClient.translate(List.of("배치 첫 문장", "배치 둘째 문장")))
                .thenReturn(List.of("First batch sentence", "Second batch sentence"));

        List<String> result = contentTranslationService.toEnglish(
                Arrays.asList("배치 첫 문장", "plain english", "배치 둘째 문장"));

        assertThat(result).containsExactly("First batch sentence", "plain english", "Second batch sentence");
        verify(translationClient, times(1)).translate(anyList());
    }

    @Test
    void servesRepeatCallsFromMemoryCacheWithoutSecondApiCall() {
        when(translationClient.translate(List.of("메모리 캐시 문장")))
                .thenReturn(List.of("Memory cached sentence"));

        assertThat(contentTranslationService.toEnglish("메모리 캐시 문장")).isEqualTo("Memory cached sentence");
        assertThat(contentTranslationService.toEnglish("메모리 캐시 문장")).isEqualTo("Memory cached sentence");

        verify(translationClient, times(1)).translate(anyList());
    }

    @Test
    void servesFromDbCacheWithoutApiCall() {
        // 다른 프로세스(이전 기동)가 저장해 둔 DB 캐시를 흉내낸다 — 인메모리 캐시엔 없는 상태.
        String source = "디비 캐시 문장";
        translationCacheRepository.save(TranslationCache.create(sha256Hex(source), source, "DB cached sentence"));

        assertThat(contentTranslationService.toEnglish(source)).isEqualTo("DB cached sentence");
        verify(translationClient, never()).translate(anyList());
    }

    @Test
    void persistsNewTranslationsToDbCache() {
        when(translationClient.translate(List.of("영속화 대상 문장")))
                .thenReturn(List.of("Persisted sentence"));

        contentTranslationService.toEnglish("영속화 대상 문장");

        assertThat(translationCacheRepository.findAllBySourceHashIn(List.of(sha256Hex("영속화 대상 문장"))))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getSourceText()).isEqualTo("영속화 대상 문장");
                    assertThat(row.getTranslatedText()).isEqualTo("Persisted sentence");
                });
    }

    @Test
    void doesNotCachePassthroughResults() {
        // 키 미설정·API 실패 시 클라이언트는 원문을 그대로 돌려준다 — 이건 캐시에 남기면 안 된다
        // (키가 나중에 설정되면 다시 번역을 시도해야 하므로).
        when(translationClient.translate(List.of("패스스루 문장"))).thenReturn(List.of("패스스루 문장"));

        assertThat(contentTranslationService.toEnglish("패스스루 문장")).isEqualTo("패스스루 문장");
        assertThat(contentTranslationService.toEnglish("패스스루 문장")).isEqualTo("패스스루 문장");

        assertThat(translationCacheRepository.findAllBySourceHashIn(List.of(sha256Hex("패스스루 문장")))).isEmpty();
        // 캐시에 안 남았으니 두 번째 호출도 API를 다시 시도한다.
        verify(translationClient, times(2)).translate(anyList());
    }

    @Test
    void deduplicatesRepeatedStringsBeforeApiCall() {
        when(translationClient.translate(List.of("중복 문장")))
                .thenReturn(List.of("Deduplicated sentence"));

        List<String> result = contentTranslationService.toEnglish(List.of("중복 문장", "중복 문장"));

        assertThat(result).containsExactly("Deduplicated sentence", "Deduplicated sentence");
        verify(translationClient, times(1)).translate(List.of("중복 문장"));
    }

    @Test
    void keepsOriginalWhenClientReturnsWrongSize() {
        when(translationClient.translate(List.of("크기 불일치 문장"))).thenReturn(List.of());

        assertThat(contentTranslationService.toEnglish("크기 불일치 문장")).isEqualTo("크기 불일치 문장");
    }

    private String sha256Hex(String text) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
