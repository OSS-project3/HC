package com.example.honorcitizen.domain.application.scheduler;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationPaymentTimeoutScheduler {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;

    @Scheduled(cron = "${application.payment-timeout-scheduler.cron:0 */10 * * * *}")
    public void cancelExpiredUnpaidApplications() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> candidateIds = applicationRepository.findPaymentTimeoutCandidateIds(
                ApplicationStatus.SUBMITTED, PaymentStatus.WAITING, now);

        for (Long applicationId : candidateIds) {
            try {
                applicationService.cancelForPaymentTimeout(applicationId, now);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("Skipped payment timeout cancellation due to concurrent update. applicationId={}", applicationId);
            } catch (CustomException e) {
                if (e.getErrorCode() == ErrorCode.INVALID_STATUS_TRANSITION
                        || e.getErrorCode() == ErrorCode.APPLICATION_NOT_FOUND) {
                    log.info("Skipped stale payment timeout candidate. applicationId={}, errorCode={}",
                            applicationId, e.getErrorCode());
                } else {
                    log.error("Failed payment timeout cancellation. applicationId={}", applicationId, e);
                }
            } catch (RuntimeException e) {
                log.error("Failed payment timeout cancellation. applicationId={}", applicationId, e);
            }
        }
    }
}
