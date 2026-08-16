package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.ApplicationDailyLimit;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ApplicationDailyLimitServiceTest {

    @Autowired
    private ApplicationDailyLimitService service;
    @Autowired
    private ApplicationDailyLimitRepository repository;

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 16);

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void reserveSlotCreatesRowOnFirstCallOfDay() {
        service.reserveSlot(USER_ID, TODAY);

        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(1);
    }

    @Test
    void reserveSlotIncrementsBelowLimit() {
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);

        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(3);
    }

    @Test
    void reserveSlotThrowsWhenAtLimit() {
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);

        assertThatThrownBy(() -> service.reserveSlot(USER_ID, TODAY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_LIMIT_EXCEEDED);

        // 거절된 시도는 카운트를 증가시키지 않는다.
        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(3);
    }

    @Test
    void reserveSlotDoesNotAffectOtherUsersOrDates() {
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);

        // 다른 사용자·다른 날짜는 별도 카운터라 오늘의 한도(3)와 무관하게 통과해야 한다.
        assertThatCode(() -> service.reserveSlot(2L, TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> service.reserveSlot(USER_ID, TODAY.plusDays(1))).doesNotThrowAnyException();
    }

    @Test
    void releaseSlotDecrementsCount() {
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);

        service.releaseSlot(USER_ID, TODAY);

        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(1);
    }

    @Test
    void releaseSlotOnMissingRowDoesNotThrow() {
        service.releaseSlot(USER_ID, TODAY);

        assertThat(repository.findByUserIdAndCountDate(USER_ID, TODAY)).isEmpty();
    }

    @Test
    void releaseSlotFreesUpSpaceForANewReservation() {
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);
        service.reserveSlot(USER_ID, TODAY);

        service.releaseSlot(USER_ID, TODAY); // 취소 시나리오 — 자리 하나 반환

        service.reserveSlot(USER_ID, TODAY); // 다시 3회 안에 들어오므로 허용돼야 한다

        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(3);
    }

    // 동시성 시나리오 1: 카운터 row가 이미 존재하는 상태(2/3)에서 동시에 여러 요청이 마지막 자리를 다툰다.
    // 단순 COUNT 후 INSERT였다면 여러 요청이 동시에 "2건이니 통과"로 오판할 수 있는 경쟁 상태다.
    @Test
    void concurrentReservationsOnExistingRowRespectLimit() throws InterruptedException {
        repository.save(seedLimit(2));

        int attempts = 5;
        RaceResult result = runConcurrently(attempts, () -> service.reserveSlot(USER_ID, TODAY));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.limitExceededCount()).isEqualTo(4);

        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(3);
    }

    // 동시성 시나리오 2: 오늘 첫 신청 row 자체가 아직 없는 상태에서 여러 요청이 동시에 "첫 신청"으로 도착한다
    // — UNIQUE(user_id, count_date) 제약 충돌이 나는 경로(재시도 로직)를 검증한다.
    @Test
    void concurrentReservationsOnBrandNewRowDoNotDoubleInsertOrLoseCounts() throws InterruptedException {
        int attempts = 3; // 한도(3) 이내라 전부 성공해야 한다

        RaceResult result = runConcurrentlyWithRetry(attempts, () -> service.reserveSlot(USER_ID, TODAY));

        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.limitExceededCount()).isZero();
        assertThat(repository.findAll()).hasSize(1);

        ApplicationDailyLimit limit = repository.findByUserIdAndCountDate(USER_ID, TODAY).orElseThrow();
        assertThat(limit.getCount()).isEqualTo(3);
    }

    private ApplicationDailyLimit seedLimit(int count) {
        ApplicationDailyLimit limit = ApplicationDailyLimit.createFirst(USER_ID, TODAY);
        for (int i = 1; i < count; i++) {
            limit.increment();
        }
        return limit;
    }

    private record RaceResult(int successCount, int limitExceededCount) {
    }

    private RaceResult runConcurrently(int attempts, Runnable call) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Boolean> successes = new CopyOnWriteArrayList<>();
        List<Boolean> limitExceeded = new CopyOnWriteArrayList<>();

        IntStream.range(0, attempts).forEach(i -> executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                call.run();
                successes.add(true);
            } catch (CustomException e) {
                if (e.getErrorCode() == ErrorCode.APPLICATION_LIMIT_EXCEEDED) {
                    limitExceeded.add(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        return new RaceResult(successes.size(), limitExceeded.size());
    }

    // reserveSlot() 단독 호출은 "오늘 첫 신청" 경쟁에서 DataIntegrityViolationException을 던질 수 있다
    // (production 코드에서는 ApplicationService가 이 예외를 잡아 한 번 재시도한다 — 여기서 동일하게 재현).
    private RaceResult runConcurrentlyWithRetry(int attempts, Runnable call) throws InterruptedException {
        Runnable withRetry = () -> {
            try {
                call.run();
            } catch (DataIntegrityViolationException e) {
                call.run();
            }
        };
        return runConcurrently(attempts, withRetry);
    }
}
