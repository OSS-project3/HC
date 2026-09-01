package com.example.honorcitizen.domain.card.dto;

// 4-D: GET/POST /api/admin/schools/{schoolId}/card-template 공통 응답 — POST 성공 응답도 GET과
// 동일한 이 DTO를 반환한다(관리자 화면이 등록 직후 별도 재조회 없이 미리보기를 갱신할 수 있게, 정책
// 확정 사항). GET에서 등록된 템플릿이 없으면 이 DTO 자체가 아니라 data:null을 반환한다(Controller에서 처리).
public record SchoolCardTemplateResponse(
        Long cardDesignId,
        String frontPreviewUrl,
        String backPreviewUrl) {
}
