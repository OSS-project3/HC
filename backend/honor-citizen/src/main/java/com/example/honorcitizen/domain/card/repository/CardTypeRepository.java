package com.example.honorcitizen.domain.card.repository;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardTypeRepository extends JpaRepository<CardType, Long> {

    Optional<CardType> findByCode(CardTypeCode code);
}
