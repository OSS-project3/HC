package com.example.honorcitizen.domain.koreanname.repository;

import com.example.honorcitizen.domain.koreanname.entity.KoreanName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KoreanNameRepository extends JpaRepository<KoreanName, Long> {

    Optional<KoreanName> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);
}
