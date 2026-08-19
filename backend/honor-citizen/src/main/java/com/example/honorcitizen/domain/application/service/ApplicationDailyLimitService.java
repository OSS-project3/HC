package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.ApplicationDailyLimit;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

// 일일 신청 생성 횟수 제한(APPLICATION.md §7: 하루 3회, KST 00:00~23:59, 개인/단체 합산) 전담.
// reserveSlot/releaseSlot은 기본 REQUIRED 전파를 사용한다. 신청 생성처럼 트랜잭션 밖에서 호출하면
// 각각 새 트랜잭션으로 즉시 커밋되고, 사용자 취소처럼 이미 열린 트랜잭션 안에서 호출하면 그 트랜잭션에
// 합류한다. 이 차이로 생성 시 슬롯은 파일 업로드 전에 즉시 확정하고, 취소 시에는 Application의
// CANCELLED 전이와 슬롯 반환을 함께 commit/rollback한다.
@Service
@RequiredArgsConstructor
public class ApplicationDailyLimitService {

    static final int MAX_APPLICATIONS_PER_DAY = 3;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ApplicationDailyLimitRepository repository;

    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    public static LocalDate toCountDate(LocalDateTime createdAt) {
        return createdAt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(KST)
                .toLocalDate();
    }

    // 신청 생성 직전(파일 업로드 이전)에 호출한다. 오늘 첫 신청이면 row가 없어 INSERT를 시도하는데,
    // 두 요청이 동시에 "오늘 첫 신청"으로 도착하면 UNIQUE(user_id, count_date) 제약 충돌이 날 수 있다 —
    // 이 예외는 트랜잭션 밖(ApplicationService)에서 잡아 새 트랜잭션으로 한 번 재시도해야 한다.
    // 같은 트랜잭션 안에서 잡아 재시도하면 이미 실패로 표시된 트랜잭션을 계속 쓰게 돼 불안정하다.
    @Transactional
    public void reserveSlot(Long userId, LocalDate countDate) {
        Optional<ApplicationDailyLimit> existing = repository.findByUserIdAndCountDateForUpdate(userId, countDate);
        if (existing.isPresent()) {
            ApplicationDailyLimit limit = existing.get();
            if (limit.isAtLimit(MAX_APPLICATIONS_PER_DAY)) {
                throw new CustomException(ErrorCode.APPLICATION_LIMIT_EXCEEDED);
            }
            limit.increment();
            return;
        }
        repository.saveAndFlush(ApplicationDailyLimit.createFirst(userId, countDate));
    }

    // 신청 생성 실패 보상과 최초 사용자 취소에 사용한다. 사용자 취소에서는 ApplicationService의
    // 외부 트랜잭션에 합류하므로 상태 전이와 카운터 감소가 원자적으로 처리된다.
    @Transactional
    public void releaseSlot(Long userId, LocalDate countDate) {
        repository.findByUserIdAndCountDateForUpdate(userId, countDate)
                .ifPresent(ApplicationDailyLimit::decrement);
    }

    // 회원탈퇴(하드 삭제) 전용 — arch.md §5.1 "다른 모듈의 Repository를 직접 호출하지 않는다" 원칙에
    // 따라 UserService가 이 Repository를 직접 쓰지 않고 이 공개 메서드를 거친다(2026-08-19 정책).
    @Transactional
    public void deleteAllForUser(Long userId) {
        repository.deleteByUserId(userId);
    }
}
