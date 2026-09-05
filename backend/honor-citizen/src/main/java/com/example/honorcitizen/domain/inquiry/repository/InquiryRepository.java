package com.example.honorcitizen.domain.inquiry.repository;

import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Inquiry> findAllByOrderByCreatedAtDesc();

    // 관리자 통계(GET /api/admin/stats) — 답변대기/답변완료 건수 집계.
    long countByStatus(InquiryStatus status);
}
