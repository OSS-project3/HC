package com.example.honorcitizen.domain.application.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationDailyLimitTest {

    @Test
    void createFirstSetsCountToOne() {
        ApplicationDailyLimit limit = ApplicationDailyLimit.createFirst(1L, LocalDate.of(2026, 8, 16));

        assertThat(limit.getUserId()).isEqualTo(1L);
        assertThat(limit.getCountDate()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(limit.getCount()).isEqualTo(1);
    }

    @Test
    void incrementIncreasesCount() {
        ApplicationDailyLimit limit = ApplicationDailyLimit.createFirst(1L, LocalDate.of(2026, 8, 16));

        limit.increment();

        assertThat(limit.getCount()).isEqualTo(2);
    }

    @Test
    void decrementDecreasesCount() {
        ApplicationDailyLimit limit = ApplicationDailyLimit.createFirst(1L, LocalDate.of(2026, 8, 16));
        limit.increment();

        limit.decrement();

        assertThat(limit.getCount()).isEqualTo(1);
    }

    @Test
    void decrementAtZeroStaysZero() {
        ApplicationDailyLimit limit = ApplicationDailyLimit.createFirst(1L, LocalDate.of(2026, 8, 16));
        limit.decrement();
        limit.decrement();

        assertThat(limit.getCount()).isZero();
    }

    @Test
    void isAtLimitTrueWhenCountReachesMax() {
        ApplicationDailyLimit limit = ApplicationDailyLimit.createFirst(1L, LocalDate.of(2026, 8, 16));
        limit.increment();
        limit.increment();

        assertThat(limit.isAtLimit(3)).isTrue();
        assertThat(limit.isAtLimit(4)).isFalse();
    }
}
