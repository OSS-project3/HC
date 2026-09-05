package com.example.honorcitizen.domain.inquiry.service;

import com.example.honorcitizen.common.enums.EmailType;
import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateRequest;
import com.example.honorcitizen.domain.inquiry.dto.InquiryCreateResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import com.example.honorcitizen.domain.inquiry.repository.InquiryRepository;
import com.example.honorcitizen.infra.mail.EmailSender;
import com.example.honorcitizen.domain.user.service.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final EmailSender emailSender;
    private final AdminAuthorizationService adminAuthorizationService;

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

    // 관리자 전체 목록. Service 직접 호출도 안전하도록 공통 관리자 인가를 가장 먼저 수행한다.
    @Transactional(readOnly = true)
    public List<InquiryListItemResponse> listAdmin(Long adminId) {
        adminAuthorizationService.requireAdmin(adminId);
        return inquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InquiryListItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryDetailResponse getAdminDetail(Long adminId, Long inquiryId) {
        adminAuthorizationService.requireAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        return InquiryDetailResponse.from(inquiry);
    }

    // 답변 등록/수정(requirements.md §④·§⑤ PATCH .../answer, 신규·수정 동일 API) — 갱신 직전
    // answer가 null이었는지로 "최초 등록"을 판정해, 최초 등록일 때만 커밋 이후 이메일을 보낸다
    // (§⑥ "답변 수정 시 이메일 재발송 안 함" 정책).
    @Transactional
    public void answer(Long adminId, Long inquiryId, String answerText) {
        adminAuthorizationService.requireAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        boolean isFirstAnswer = inquiry.getAnswer() == null;

        inquiry.answer(answerText);

        if (isFirstAnswer) {
            registerAnswerEmailAfterCommit(inquiry.getEmail(), inquiry.getTitle(), answerText);
        }
    }

    // Board의 deleteFilesAfterCommit과 동일한 after-commit 등록 패턴 — 커밋이 실제로 끝난 뒤에만
    // 이메일을 보내, 롤백된 답변에 대해 알림이 나가는 상황을 막는다.
    private void registerAnswerEmailAfterCommit(String email, String title, String answerText) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendAnswerEmailQuietly(email, title, answerText);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendAnswerEmailQuietly(email, title, answerText);
            }
        });
    }

    // best-effort 발송 — 실패해도 답변 저장 자체는 이미 커밋된 뒤라 되돌리지 않는다(§⑥ 정책).
    private void sendAnswerEmailQuietly(String email, String title, String answerText) {
        try {
            emailSender.send(email, EmailType.INQUIRY_ANSWERED, "문의하신 내용에 답변이 등록되었습니다",
                    buildAnswerHtmlBody(title, answerText), buildAnswerTextBody(title, answerText));
        } catch (RuntimeException e) {
            log.warn("Failed to send inquiry answer email. email={}", email, e);
        }
    }

    private String buildAnswerTextBody(String title, String answerText) {
        return """
                문의하신 [%s]에 대한 답변이 등록되었습니다.

                %s
                """.formatted(title, answerText);
    }

    private String buildAnswerHtmlBody(String title, String answerText) {
        return """
                <p>문의하신 [%s]에 대한 답변이 등록되었습니다.</p>
                <p>%s</p>
                """.formatted(title, answerText);
    }

    // 답변 유무와 무관한 독립 상태 변경(requirements.md §④·§⑥ PATCH .../status) — 전화 상담 등으로
    // COMPLETED이면서 answer가 null인 상태도 유효하다(answer/status 사이에 불변식을 걸지 않음).
    @Transactional
    public void changeStatus(Long adminId, Long inquiryId, InquiryStatus status) {
        adminAuthorizationService.requireAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        inquiry.changeStatus(status);
    }
}
