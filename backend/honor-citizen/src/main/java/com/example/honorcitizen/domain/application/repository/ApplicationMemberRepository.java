package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationMemberRepository extends JpaRepository<ApplicationMember, Long> {

    List<ApplicationMember> findByApplicationId(Long applicationId);

    Optional<ApplicationMember> findByCardNumber(String cardNumber);
}
