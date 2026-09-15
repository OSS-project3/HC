package com.example.honorcitizen.domain.manseryeok.repository;

import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ManseryeokResultRepository extends JpaRepository<ManseryeokResult, Long> {

    Optional<ManseryeokResult> findByApplicationMemberIdAndActiveTrue(Long applicationMemberId);

    List<ManseryeokResult> findByApplicationMemberIdOrderByCalculatedAtDesc(Long applicationMemberId);

    // 재진입 일괄 복원(1-E-3) — Application 소속 Member 전체의 활성 결과를 쿼리 한 번으로 조회한다.
    List<ManseryeokResult> findByApplicationMemberIdInAndActiveTrue(Collection<Long> applicationMemberIds);
}
