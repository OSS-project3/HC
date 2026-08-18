package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationMemberRepository extends JpaRepository<ApplicationMember, Long> {

    List<ApplicationMember> findByApplicationId(Long applicationId);

    // 마이페이지 신청 상세(api.md API 7)의 memberCount — 단체 신청은 구성원 개별 목록 대신 총원수만 노출한다.
    long countByApplicationId(Long applicationId);

    Optional<ApplicationMember> findByCardNumber(String cardNumber);

    // 후기 자격검증(단체 신청의 실제 카드 수령자) — ReviewEligibilityService에서 사용.
    List<ApplicationMember> findByEmail(String email);

    void deleteByApplicationId(Long applicationId);
}
