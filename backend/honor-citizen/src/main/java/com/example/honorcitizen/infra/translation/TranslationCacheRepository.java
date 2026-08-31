package com.example.honorcitizen.infra.translation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TranslationCacheRepository extends JpaRepository<TranslationCache, Long> {

    List<TranslationCache> findAllBySourceHashIn(Collection<String> sourceHashes);
}
