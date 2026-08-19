package com.example.honorcitizen.domain.inquiry.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateRequest;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<InquiryListItemResponse> listMine(Long userId) {
        return inquiryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(InquiryListItemResponse::from)
                .toList();
    }

    // requirements.md §⑤ GET /api/my/inquiries/{id} — 존재 확인(404)과 소유권 확인(403)을 분리한다
    // (MyApplicationController/ApplicationService.getMyApplicationDetail과 동일 패턴, §⑤ 정정 내용 참고).
    @Transactional(readOnly = true)
    public InquiryDetailResponse getMineDetail(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        if (!inquiry.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return InquiryDetailResponse.from(inquiry);
    }

    // 관리자 전체 목록(requirements.md §④ API 4) — 검색·페이지네이션 없음(프론트에 해당 UI 자체가
    // 없음, §⑥ 확정 정책). 권한 검증은 SecurityConfig의 /api/admin/** 라우트 레벨이 담당한다(Board 선례).
    @Transactional(readOnly = true)
    public List<InquiryListItemResponse> listAdmin() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InquiryListItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryDetailResponse getAdminDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        return InquiryDetailResponse.from(inquiry);
    }
}
