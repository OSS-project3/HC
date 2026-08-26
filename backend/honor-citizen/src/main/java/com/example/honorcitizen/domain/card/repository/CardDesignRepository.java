package com.example.honorcitizen.domain.card.repository;

import com.example.honorcitizen.domain.card.entity.CardDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardDesignRepository extends JpaRepository<CardDesign, Long> {

    List<CardDesign> findByCardTypeIdOrderByDesignNumber(Long cardTypeId);

    List<CardDesign> findByCardTypeIdAndActiveOrderByDesignNumber(Long cardTypeId, boolean active);
}
