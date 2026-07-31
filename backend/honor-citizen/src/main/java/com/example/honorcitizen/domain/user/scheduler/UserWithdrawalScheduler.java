package com.example.honorcitizen.domain.user.scheduler;

import com.example.honorcitizen.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserWithdrawalScheduler {

    private final UserService userService;

    @Scheduled(cron = "${app.scheduler.withdrawal-cleanup-cron:0 0 3 * * *}")
    public void anonymizeExpiredWithdrawnUsers() {
        userService.anonymizeExpiredWithdrawnUsers();
    }
}
