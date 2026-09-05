package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByApplicationNumber(String applicationNumber);

    // 단체 카드번호 일괄 저장(1-C) — Application row를 잠그고 요청 version과 대조해 동시 수정을 막는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Application a WHERE a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") Long id);

    // 마이페이지 신청 목록(api.md API 6) — 로그인 사용자 본인 신청만.
    Page<Application> findByUserId(Long userId, Pageable pageable);

    Page<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

    // 관리자 신청 목록(api.md 신규) — 소유자 무관 전체 조회. status 없으면 JpaRepository.findAll(Pageable) 사용.
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    // 관리자 통계(GET /api/admin/stats) — 개인/단체 신청 건수 집계.
    long countByApplicationType(ApplicationType applicationType);

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
