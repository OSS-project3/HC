package com.example.honorcitizen.domain.inquiry.repository;

import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

// 목록 조회 메서드(findAllByUserIdOrderByCreatedAtDesc 등)는 INQUIRY-2/3에서 추가한다.
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
