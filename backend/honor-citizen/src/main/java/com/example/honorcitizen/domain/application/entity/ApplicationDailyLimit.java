package com.example.honorcitizen.domain.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 사용자별·일자별(KST) 신청 생성 횟수 카운터. 개인/단체 신청을 합산해 하루 3회로 제한한다
// (APPLICATION.md §7). Application row 자체가 아니라 별도 카운터를 두는 이유:
// COUNT(*) FROM applications 라이브 집계는 "카운트 확인 → 저장" 사이에 동시 요청이 끼어드는
// 경쟁 상태를 막을 수 없다 — 이 엔티티의 row를 비관적 락으로 잠그고 원자적으로 증가시켜야
// 두 요청이 동시에 "아직 2건이니 통과"라고 오판하는 것을 막을 수 있다(ApplicationDailyLimitService 참고).
@Entity
@Table(name = "application_daily_limits", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "count_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationDailyLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(nullable = false)
    private int count;

    public static ApplicationDailyLimit createFirst(Long userId, LocalDate countDate) {
        ApplicationDailyLimit limit = new ApplicationDailyLimit();
        limit.userId = userId;
        limit.countDate = countDate;
        limit.count = 1;
        return limit;
    }

    public boolean isAtLimit(int maxPerDay) {
        return this.count >= maxPerDay;
    }

    public void increment() {
        this.count++;
    }

    // 신청 생성 도중 실패(파일 업로드·DB 저장 실패)했을 때 예약한 자리를 반환한다.
    // 향후 "신청 취소" 기능이 생기면 취소 시점에도 이 메서드로 자리를 반환할 수 있다.
    public void decrement() {
        if (this.count > 0) {
            this.count--;
        }
    }
}
