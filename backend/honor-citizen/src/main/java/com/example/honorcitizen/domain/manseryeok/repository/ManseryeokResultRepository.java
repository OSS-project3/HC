package com.example.honorcitizen.domain.manseryeok.repository;

import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManseryeokResultRepository extends JpaRepository<ManseryeokResult, Long> {

    Optional<ManseryeokResult> findByApplicationMemberIdAndActiveTrue(Long applicationMemberId);

    List<ManseryeokResult> findByApplicationMemberIdOrderByCalculatedAtDesc(Long applicationMemberId);
}
