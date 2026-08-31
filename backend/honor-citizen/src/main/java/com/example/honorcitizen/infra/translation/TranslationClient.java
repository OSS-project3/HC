package com.example.honorcitizen.infra.translation;

import java.util.List;

/**
 * 한국어→영어 기계번역 클라이언트. 입력 순서를 그대로 유지한 번역 결과를 돌려준다.
 *
 * 번역은 "점진적 개선(progressive enhancement)"이다 — 구현체는 키 미설정·API 장애 등
 * 어떤 이유로든 번역이 불가능하면 예외를 던지지 말고 원문 리스트를 그대로 반환해야 한다.
 * 사용자 요청이 번역 실패 때문에 실패해서는 안 된다.
 */
public interface TranslationClient {

    List<String> translate(List<String> texts);
}
