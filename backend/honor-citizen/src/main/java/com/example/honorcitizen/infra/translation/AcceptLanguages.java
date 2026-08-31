package com.example.honorcitizen.infra.translation;

// Accept-Language 헤더에서 "영어 응답을 원하는지"만 판정하는 최소 유틸.
// 프론트는 모든 API 호출에 Accept-Language: en 또는 ko를 실어 보낸다 — 첫 번째(primary)
// 태그가 en으로 시작할 때만 번역 대상으로 본다(en-US, en-GB 포함). 그 외(ko, 미지정)는 원문.
public final class AcceptLanguages {

    private AcceptLanguages() {
    }

    public static boolean wantsEnglish(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return false;
        }
        // "en-US,en;q=0.9,ko;q=0.8" → primary 태그 "en-US"만 본다(q 가중치 정렬까지는 하지 않음).
        String primary = acceptLanguage.split(",")[0].split(";")[0].trim();
        return primary.length() >= 2 && primary.regionMatches(true, 0, "en", 0, 2);
    }
}
