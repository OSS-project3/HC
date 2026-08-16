package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.ApplicationDailyLimit;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

// 일일 신청 생성 횟수 제한(APPLICATION.md §7: 하루 3회, KST 00:00~23:59, 개인/단체 합산) 전담.
// reserveSlot/releaseSlot 각각 독립된 @Transactional 메서드다 — ApplicationService가 이 두 메서드를
// 호출하는 시점(호출부 자체는 트랜잭션이 아님)마다 매번 새 트랜잭션이 열리고 즉시 커밋된다.
// 파일 업로드처럼 오래 걸리는 작업 이전에 자리를 먼저 원자적으로 확정해야, 동시에 들어온 다른 요청이
// 그 확정을 곧바로 볼 수 있다(같은 트랜잭션 안에 계속 묶어두면 커밋 전까지 다른 트랜잭션이 못 본다).
@Service
@RequiredArgsConstructor
public class ApplicationDailyLimitService {

    static final int MAX_APPLICATIONS_PER_DAY = 3;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ApplicationDailyLimitRepository repository;

    public static LocalDate today() {
        return LocalDate.now(KST);
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

    // 신청 생성이 실패(파일 업로드·DB 저장 실패)했을 때 예약한 자리를 반환한다.
    // 향후 "신청 취소" API가 생기면 취소 시점에도 이 메서드를 재사용해 자리를 반환할 수 있다 —
    // 다만 Entity(Application.cancel())는 Service를 호출할 수 없으므로(arch.md §3.2 계층 규칙),
    // 그 연결은 취소 기능을 실제로 구현하는 Service 계층에서 이 메서드를 호출하는 방식으로 이뤄져야 한다.
    @Transactional
    public void releaseSlot(Long userId, LocalDate countDate) {
        repository.findByUserIdAndCountDateForUpdate(userId, countDate)
                .ifPresent(ApplicationDailyLimit::decrement);
    }
}
