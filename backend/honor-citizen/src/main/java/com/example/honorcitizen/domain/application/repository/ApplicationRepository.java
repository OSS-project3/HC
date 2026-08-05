package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByApplicationNumber(String applicationNumber);

    long countByApplicationNumberStartingWith(String prefix);
}
