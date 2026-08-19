package com.example.honorcitizen.domain.inquiry.service;

import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateRequest;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateResponse;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    // 문의 등록(requirements.md §⑤ POST /api/inquiries) — userId는 컨트롤러가 JWT에서 이미 추출해
    // 전달한다. privacyConsent는 Bean Validation(@AssertTrue)에서 걸러지므로 여기서 재검증하지 않는다.
    @Transactional
    public InquiryCreateResponse create(Long userId, InquiryCreateRequest request) {
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(userId, request.getCategory(), request.getName(),
                request.getEmail(), request.getPhone(), request.getTitle(), request.getContent()));
        return InquiryCreateResponse.from(inquiry);
    }
}
