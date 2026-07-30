package com.example.honorcitizen.domain.log.repository;

import com.example.honorcitizen.domain.log.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findAllByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
