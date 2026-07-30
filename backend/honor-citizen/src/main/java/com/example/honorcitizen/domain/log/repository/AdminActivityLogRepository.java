package com.example.honorcitizen.domain.log.repository;

import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {}
