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

    // 작명 업무 진행중/완료/캔슬 조회(2026-09-24 정책) — 개인은 status=COMPLETED 그대로, 단체는
    // status와 무관하게 "멤버가 1명 이상 존재하고 전원 카드 앞·뒷면이 생성됨"으로 판정한다.
    // CANCELLED는 그 자체로 별도 분류이므로 단체 쪽에서 명시적으로 제외한다(카드가 다 만들어진
    // 뒤 취소된 경우도 완료가 아니라 캔슬로 집계).
    @Query("""
            SELECT a FROM Application a
            WHERE (a.applicationType = :individualType AND a.status = :completedStatus)
               OR (a.applicationType = :groupType AND a.status <> :cancelledStatus
                   AND EXISTS (SELECT 1 FROM ApplicationMember m WHERE m.applicationId = a.id)
                   AND NOT EXISTS (
                       SELECT 1 FROM ApplicationMember m2
                       WHERE m2.applicationId = a.id
                         AND (m2.cardFrontPath IS NULL OR m2.cardBackPath IS NULL)))
            """)
    Page<Application> findNamingDone(
            @Param("individualType") ApplicationType individualType,
            @Param("groupType") ApplicationType groupType,
            @Param("completedStatus") ApplicationStatus completedStatus,
            @Param("cancelledStatus") ApplicationStatus cancelledStatus,
            Pageable pageable);

    // CANCELLED가 아니면서 위 findNamingDone 조건에도 해당하지 않는 나머지 전부 — 두 메서드가
    // CANCELLED/DONE/IN_PROGRESS 세 분류를 상호 배타적으로 정확히 나누도록 정의를 그대로 반전한다.
    @Query("""
            SELECT a FROM Application a
            WHERE a.status <> :cancelledStatus
              AND NOT (
                (a.applicationType = :individualType AND a.status = :completedStatus)
                OR (a.applicationType = :groupType
                    AND EXISTS (SELECT 1 FROM ApplicationMember m WHERE m.applicationId = a.id)
                    AND NOT EXISTS (
                        SELECT 1 FROM ApplicationMember m2
                        WHERE m2.applicationId = a.id
                          AND (m2.cardFrontPath IS NULL OR m2.cardBackPath IS NULL)))
              )
            """)
    Page<Application> findNamingInProgress(
            @Param("individualType") ApplicationType individualType,
            @Param("groupType") ApplicationType groupType,
            @Param("completedStatus") ApplicationStatus completedStatus,
            @Param("cancelledStatus") ApplicationStatus cancelledStatus,
            Pageable pageable);

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
