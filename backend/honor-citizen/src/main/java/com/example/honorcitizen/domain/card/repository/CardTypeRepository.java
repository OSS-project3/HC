package com.example.honorcitizen.domain.card.repository;

import com.example.honorcitizen.domain.card.entity.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTypeRepository extends JpaRepository<CardType, Long> {
}
