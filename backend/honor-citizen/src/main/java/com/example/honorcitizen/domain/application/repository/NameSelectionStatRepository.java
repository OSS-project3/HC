package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.NameSelectionStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NameSelectionStatRepository extends JpaRepository<NameSelectionStat, Long> {

    Optional<NameSelectionStat> findByNameAndHanja(String name, String hanja);
}
