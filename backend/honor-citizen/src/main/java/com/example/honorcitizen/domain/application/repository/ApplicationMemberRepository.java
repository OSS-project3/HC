package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationMemberRepository extends JpaRepository<ApplicationMember, Long> {

    List<ApplicationMember> findByApplicationId(Long applicationId);

    // 단체 신청 작명 업무 진행률 표시(2026-09-24) — 목록 페이지 안의 여러 단체 신청에 대해 카드
    // 앞·뒷면이 모두 생성된 멤버 수를 한 번에 집계한다(N+1 방지). row[0]=applicationId, row[1]=count.
    @Query("""
            SELECT m.applicationId, COUNT(m) FROM ApplicationMember m
            WHERE m.applicationId IN :applicationIds
              AND m.cardFrontPath IS NOT NULL
              AND m.cardBackPath IS NOT NULL
            GROUP BY m.applicationId
            """)
    List<Object[]> countCompletedMembersByApplicationIds(@Param("applicationIds") List<Long> applicationIds);

    // 마이페이지 신청 상세(api.md API 7)의 memberCount — 단체 신청은 구성원 개별 목록 대신 총원수만 노출한다.
    long countByApplicationId(Long applicationId);

    Optional<ApplicationMember> findByCardNumber(String cardNumber);

    // 후기 자격검증(단체 신청의 실제 카드 수령자) — ReviewEligibilityService에서 사용.
    List<ApplicationMember> findByEmail(String email);

    void deleteByApplicationId(Long applicationId);
}
