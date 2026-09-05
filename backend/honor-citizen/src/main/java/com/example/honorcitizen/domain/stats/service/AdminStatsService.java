package com.example.honorcitizen.domain.stats.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.domain.stats.dto.AdminStatsResponse;
import com.example.honorcitizen.domain.user.service.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 통계 집계 전용 Service. 라우트 검증과 별개로 직접 호출 시에도 공통 관리자 인가를 수행한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final ApplicationRepository applicationRepository;
    private final InquiryRepository inquiryRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminStatsResponse getStats(Long adminId) {
        adminAuthorizationService.requireAdmin(adminId);
        return new AdminStatsResponse(
                applicationRepository.count(),
                applicationRepository.countByApplicationType(ApplicationType.INDIVIDUAL),
                applicationRepository.countByApplicationType(ApplicationType.GROUP),
                inquiryRepository.count(),
                inquiryRepository.countByStatus(InquiryStatus.PENDING),
                inquiryRepository.countByStatus(InquiryStatus.COMPLETED));
    }
}
