package com.example.honorcitizen.infra.translation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 사용자 노출 자유 텍스트의 한국어→영어 번역 진입점.
 *
 * 조회 순서: 인메모리 캐시 → DB 캐시(translation_cache) → 남은 문자열 전부를 API 1회 배치 호출.
 * 새로 번역된 결과는 두 캐시에 모두 적재한다. null/공백/한글 없는 문자열은 번역 대상이 아니며
 * (원문 그대로 반환, API 호출·캐시 적재 없음), 어떤 실패도 사용자 요청을 실패시키지 않는다 —
 * 번역이 안 되면 그냥 한국어 원문이 나간다(점진적 개선).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTranslationService {

    private static final Pattern HANGUL = Pattern.compile("[가-힣]");

    private final TranslationClient translationClient;
    private final TranslationCacheRepository translationCacheRepository;

    // 원문 → 번역문 인메모리 캐시. 프로세스 생애 동안만 유지되고, 재기동 시 DB 캐시로 다시 채워진다.
    private final ConcurrentHashMap<String, String> memoryCache = new ConcurrentHashMap<>();

    public String toEnglish(String text) {
        return toEnglish(Collections.singletonList(text)).get(0);
    }

    // 입력 순서·크기를 그대로 유지한 리스트를 반환한다(null 요소는 null 그대로).
    public List<String> toEnglish(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        String[] result = texts.toArray(new String[0]);

        // 번역 대상만 중복 없이 추린다 — 목록 응답에서 같은 문자열이 반복돼도 API에는 한 번만 보낸다.
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        for (String text : texts) {
            if (isTranslatable(text)) {
                targets.add(text);
            }
        }
        if (targets.isEmpty()) {
            return Arrays.asList(result);
        }

        Map<String, String> resolved = new HashMap<>();
        List<String> misses = new ArrayList<>();
        for (String text : targets) {
            String cached = memoryCache.get(text);
            if (cached != null) {
                resolved.put(text, cached);
            } else {
                misses.add(text);
            }
        }

        if (!misses.isEmpty()) {
            lookupDbCache(misses, resolved);
            misses.removeIf(resolved::containsKey);
        }
        if (!misses.isEmpty()) {
            translateAndCache(misses, resolved);
        }

        for (int i = 0; i < result.length; i++) {
            String translated = result[i] == null ? null : resolved.get(result[i]);
            if (translated != null) {
                result[i] = translated;
            }
        }
        return Arrays.asList(result);
    }

    private void lookupDbCache(List<String> misses, Map<String, String> resolved) {
        Map<String, String> hashToSource = new LinkedHashMap<>();
        for (String source : misses) {
            hashToSource.put(sha256Hex(source), source);
        }
        for (TranslationCache row : translationCacheRepository.findAllBySourceHashIn(hashToSource.keySet())) {
            String source = hashToSource.get(row.getSourceHash());
            if (source != null) {
                resolved.put(source, row.getTranslatedText());
                memoryCache.putIfAbsent(source, row.getTranslatedText());
            }
        }
    }

    private void translateAndCache(List<String> misses, Map<String, String> resolved) {
        List<String> translated = translationClient.translate(misses);
        if (translated == null || translated.size() != misses.size()) {
            log.warn("번역 결과 개수가 요청과 다름(요청 {}건) — 해당 문자열은 원문 유지", misses.size());
            return;
        }
        for (int i = 0; i < misses.size(); i++) {
            String source = misses.get(i);
            String english = translated.get(i);
            // 원문이 그대로 돌아온 경우(키 미설정·API 실패의 패스스루)는 캐시에 넣지 않는다 —
            // 키가 나중에 설정됐을 때 "미번역" 결과가 눌러앉는 것을 막기 위함.
            if (english == null || english.isBlank() || english.equals(source)) {
                continue;
            }
            resolved.put(source, english);
            memoryCache.putIfAbsent(source, english);
            saveQuietly(source, english);
        }
    }

    private void saveQuietly(String source, String english) {
        try {
            translationCacheRepository.save(TranslationCache.create(sha256Hex(source), source, english));
        } catch (DataIntegrityViolationException e) {
            // 동일 문자열 동시 번역 경합 — 다른 요청이 이미 저장했다면 그 결과를 쓰면 되므로 무시.
            log.debug("translation_cache 중복 삽입 무시(동시 요청 경합)");
        }
    }

    private boolean isTranslatable(String text) {
        return text != null && !text.isBlank() && HANGUL.matcher(text).find();
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JVM 표준 알고리즘이라 발생할 수 없다.
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }
}
