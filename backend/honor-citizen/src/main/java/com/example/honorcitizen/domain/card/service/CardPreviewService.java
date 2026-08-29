package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.card.dto.CardPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.function.Predicate;

// 2-C: 저장 없는 카드 미리보기. DB row·S3 object를 생성하지 않고 실제 신청 데이터로 렌더링만 한다
// (admin-saju.md/TODO.md "관리자 작명 확정·카드 제작 구현 계획" 2-C). 검증·S3 다운로드·렌더링은
// CardRenderPreparation(3-A 공유 준비 로직, "3. 카드 생성·저장"의 Generate와 공유, 2026-08-30
// 리팩터링)에 위임하고 여기서는 base64 인코딩만 한다. CardImageCompositor/CardMemberData가
// 패키지 프라이빗이라 이 서비스도 domain.card.service 패키지에 둔다.
@Service
@RequiredArgsConstructor
public class CardPreviewService {

    private static final Predicate<Application> PREVIEW_STATUS_GATE =
            application -> application.getStatus() == ApplicationStatus.PRODUCTION_READY;

    private final CardRenderPreparation preparation;

    @Transactional(readOnly = true)
    public CardPreviewResponse preview(Long adminId, Long applicationId, Long memberId, CardPreviewRequest request) {
        CardRenderPreparation.CardRenderResult result = preparation.prepare(
                adminId, applicationId, memberId, request, PREVIEW_STATUS_GATE);
        return new CardPreviewResponse(
                Base64.getEncoder().encodeToString(result.front()),
                Base64.getEncoder().encodeToString(result.back()));
    }
}
