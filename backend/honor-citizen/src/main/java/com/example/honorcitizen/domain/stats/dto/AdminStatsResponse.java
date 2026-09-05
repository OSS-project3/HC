package com.example.honorcitizen.domain.stats.dto;

import lombok.Getter;

// 관리자 통계 대시보드(GET /api/admin/stats) 응답 — AdminPage.tsx가 페이지 슬라이스(size=100)로
// 직접 세던 개인/단체 신청 수를 정확한 DB 집계로 대체한다(2026-09-05 정책 확정, 기간 필터 없음·전체 누적).
@Getter
public class AdminStatsResponse {

    private final long totalApplications;
    private final long individualApplications;
    private final long groupApplications;
    private final long totalInquiries;
    private final long pendingInquiries;
    private final long completedInquiries;

    public AdminStatsResponse(long totalApplications, long individualApplications, long groupApplications,
            long totalInquiries, long pendingInquiries, long completedInquiries) {
        this.totalApplications = totalApplications;
        this.individualApplications = individualApplications;
        this.groupApplications = groupApplications;
        this.totalInquiries = totalInquiries;
        this.pendingInquiries = pendingInquiries;
        this.completedInquiries = completedInquiries;
    }
}
