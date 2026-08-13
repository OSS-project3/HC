package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationMemberRepository extends JpaRepository<ApplicationMember, Long> {

    List<ApplicationMember> findByApplicationId(Long applicationId);

    Optional<ApplicationMember> findByCardNumber(String cardNumber);

    // 후기 자격검증(단체 신청의 실제 카드 수령자) — ReviewEligibilityService에서 사용.
    List<ApplicationMember> findByEmail(String email);

    void deleteByApplicationId(Long applicationId);
}
