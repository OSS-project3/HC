package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByApplicationNumber(String applicationNumber);

    // 마이페이지 신청 목록(api.md API 6) — 로그인 사용자 본인 신청만.
    Page<Application> findByUserId(Long userId, Pageable pageable);

    Page<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

    @Query("""
            SELECT a.id FROM Application a
            WHERE a.status = :status
              AND a.paymentStatus = :paymentStatus
              AND a.paymentDueAt IS NOT NULL
              AND a.paymentDueAt <= :now
            ORDER BY a.id
            """)
    List<Long> findPaymentTimeoutCandidateIds(
            @Param("status") ApplicationStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("now") LocalDateTime now);
}
