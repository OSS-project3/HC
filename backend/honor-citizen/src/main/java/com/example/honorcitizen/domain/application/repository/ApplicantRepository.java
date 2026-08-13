package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    Optional<Applicant> findByApplicationId(Long applicationId);

    // 후기 자격검증(같은 이메일로 여러 번 신청했을 수 있음) — ReviewEligibilityService에서 사용.
    List<Applicant> findByEmail(String email);
}
