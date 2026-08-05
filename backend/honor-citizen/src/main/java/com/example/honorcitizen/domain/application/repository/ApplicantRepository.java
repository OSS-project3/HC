package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    Optional<Applicant> findByApplicationId(Long applicationId);
}
