package com.example.honorcitizen.domain.log.repository;

import com.example.honorcitizen.domain.log.entity.ApplicationStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusLogRepository extends JpaRepository<ApplicationStatusLog, Long> {
    List<ApplicationStatusLog> findAllByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
