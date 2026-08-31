package com.example.honorcitizen.infra.translation;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 한국어→영어 번역 결과의 영속 캐시. 같은 원문은 API를 다시 호출하지 않도록 SHA-256 해시로
// 조회한다(원문이 TEXT라 원문 컬럼 자체엔 유니크 인덱스를 걸 수 없음). 동시 삽입 경합은
// source_hash 유니크 제약이 잡고, 호출측(ContentTranslationService)이 조용히 무시한다.
@Entity
@Table(name = "translation_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TranslationCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 원문(sourceText)의 SHA-256 hex — 캐시 조회 키.
    @Column(nullable = false, length = 64, unique = true)
    private String sourceHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    public static TranslationCache create(String sourceHash, String sourceText, String translatedText) {
        TranslationCache cache = new TranslationCache();
        cache.sourceHash = sourceHash;
        cache.sourceText = sourceText;
        cache.translatedText = translatedText;
        return cache;
    }
}
