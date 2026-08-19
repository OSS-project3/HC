package com.example.honorcitizen.domain.inquiry.repository;

import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 관리자 목록 조회 메서드는 INQUIRY-3에서 추가한다.
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
