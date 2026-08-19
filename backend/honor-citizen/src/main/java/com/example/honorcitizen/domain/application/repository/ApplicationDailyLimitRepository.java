package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.ApplicationDailyLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ApplicationDailyLimitRepository extends JpaRepository<ApplicationDailyLimit, Long> {

    // 비관적 락(FOR UPDATE) — 같은 사용자·같은 날짜의 row를 두 트랜잭션이 동시에 읽고 증가시키는
    // 경쟁 상태를 막는다. row가 아직 없으면(오늘 첫 신청) 아무것도 잠그지 못하므로, 그 경우는
    // ApplicationDailyLimitService가 INSERT를 시도하고 유니크 제약 충돌 시 재시도하는 방식으로 처리한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ApplicationDailyLimit a WHERE a.userId = :userId AND a.countDate = :countDate")
    Optional<ApplicationDailyLimit> findByUserIdAndCountDateForUpdate(
            @Param("userId") Long userId, @Param("countDate") LocalDate countDate);

    // 락 없는 단순 조회 — 트랜잭션 밖(예: 테스트 검증 코드)에서도 호출할 수 있다.
    // PESSIMISTIC_WRITE 쿼리는 활성 트랜잭션이 없으면 TransactionRequiredException을 던지므로
    // 상태 확인 목적으로는 이 메서드를 쓴다.
    Optional<ApplicationDailyLimit> findByUserIdAndCountDate(Long userId, LocalDate countDate);

    void deleteByUserId(Long userId);
}
