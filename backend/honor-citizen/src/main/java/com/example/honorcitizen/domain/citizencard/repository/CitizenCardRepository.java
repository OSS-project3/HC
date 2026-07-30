package com.example.honorcitizen.domain.citizencard.repository;

import com.example.honorcitizen.domain.citizencard.entity.CitizenCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CitizenCardRepository extends JpaRepository<CitizenCard, Long> {

    Optional<CitizenCard> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    // 전체 카드 중 마지막 4자리(순번) 최댓값 조회 — 카드 번호 채번용
    @Query(value = "SELECT MAX(CAST(RIGHT(card_number, 4) AS UNSIGNED)) FROM citizen_cards", nativeQuery = true)
    Optional<Integer> findMaxSequence();
}
