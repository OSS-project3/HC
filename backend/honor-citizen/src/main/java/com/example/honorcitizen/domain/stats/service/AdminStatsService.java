package com.example.honorcitizen.domain.stats.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.domain.stats.dto.AdminStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 통계(GET /api/admin/stats) — Application/Inquiry 어느 한쪽 도메인에도 속하지 않는
// 집계 전용 기능이라 별도 도메인으로 둔다. 권한은 SecurityConfig의 /api/admin/**(hasRole ADMIN)
// 라우트 레벨 검증에만 맡긴다(InquiryAdminController와 동일 원칙 — 조회뿐이라 adminId 자체가 필요 없음).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final ApplicationRepository applicationRepository;
    private final InquiryRepository inquiryRepository;

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                applicationRepository.count(),
                applicationRepository.countByApplicationType(ApplicationType.INDIVIDUAL),
                applicationRepository.countByApplicationType(ApplicationType.GROUP),
                inquiryRepository.count(),
                inquiryRepository.countByStatus(InquiryStatus.PENDING),
                inquiryRepository.countByStatus(InquiryStatus.COMPLETED));
    }
}
