package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// 3-A 공유 준비 로직(2026-08-30) — Preview(CardPreviewService)와 Generate(CardGenerationService)가
// 공통으로 쓰는 "관리자 검증 ~ 렌더링" 구간. 원래 CardPreviewService.preview()에 있던 로직을 그대로
// 옮긴 것 — 유일한 차이는 상태 게이트(statusGate)뿐이라 파라미터로 받는다. Application.cardDesignId/
// cardIssueDate가 이미 확정된 이후에는 Preview·Generate 둘 다 여기서 걸린다(2026-08-30 정책 —
// 검증을 두 곳에 복제하지 않기 위해 반드시 이 한 곳에서만 체크한다).
// CardImageCompositor/CardMemberData가 패키지 프라이빗이라 이 클래스도 domain.card.service 패키지에 둔다.
@Component
@RequiredArgsConstructor
class CardRenderPreparation {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardDesignRepository cardDesignRepository;
    private final UploadFileRepository uploadFileRepository;
    private final ManseryeokResultRepository manseryeokResultRepository;
    private final StorageService storageService;
    private final UserService userService;
    private final CardImageCompositor compositor;
    private final ObjectMapper objectMapper;

    record CardRenderResult(Application application, ApplicationMember member, CardDesign design,
            byte[] front, byte[] back) {
    }

    CardRenderResult prepare(Long adminId, Long applicationId, Long memberId, CardPreviewRequest request,
            Predicate<Application> statusGate) {
        validateAdmin(adminId);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!statusGate.test(application)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        ApplicationMember member = findMember(applicationId, memberId);
        CardType cardType = cardTypeRepository.findById(application.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_TYPE_NOT_FOUND));
        CardDesign design = cardDesignRepository.findById(request.getCardDesignId())
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND));
        if (!design.getCardTypeId().equals(cardType.getId()) || !design.isActive()) {
            throw new CustomException(ErrorCode.CARD_DESIGN_MISMATCH);
        }
        // 같은 Application 안의 모든 Member는 같은 디자인을 공유한다는 전제 — 카드 생성으로 한 번
        // 확정된 이후에는 Preview든 Generate든 다른 디자인 요청을 거절한다.
        if (application.getCardDesignId() != null && !application.getCardDesignId().equals(request.getCardDesignId())) {
            throw new CustomException(ErrorCode.CARD_DESIGN_MISMATCH);
        }
        validateIssueDate(request.getIssueDate(), application);
        // 발급일자도 신청서 전체에서 하나만 존재해야 한다 — 확정 이후 다른 날짜 요청은 거절한다.
        if (application.getCardIssueDate() != null && !application.getCardIssueDate().equals(request.getIssueDate())) {
            throw new CustomException(ErrorCode.CARD_ISSUE_DATE_MISMATCH);
        }
        validateNamingComplete(member);
        validateCardNumber(member);
        validateIssuerAssets(application, cardType.getCode());

        String zodiacBranch = resolveZodiacBranch(member);
        byte[] photo = member.getPhotoPath() != null ? storageService.download(member.getPhotoPath()) : null;
        byte[] logo = downloadUploadFile(application.getLogoFileId());
        byte[] seal = downloadUploadFile(application.getSealFileId());

        CardMemberData data = cardType.getCode() == CardTypeCode.STUDENT
                ? studentMemberData(application, member, design, request, photo, zodiacBranch)
                : new CardMemberData(
                        member.getSurname(), member.getName(), member.getEnglishName(), member.getChineseName(),
                        member.getNameMeaning(), member.getNameInterpretation(), photo, member.getCardNumber(),
                        member.getAddress(), request.getIssueDate(), zodiacBranch, logo, seal);

        CardTypeCode code = cardType.getCode();
        byte[] front = compositor.composeFront(code, design.getDesignNumber(), data);
        byte[] back = compositor.composeBack(code, design.getDesignNumber(), data);

        return new CardRenderResult(application, member, design, front, back);
    }

    // 4-C: 학생증 전용 CardMemberData 조립 — 카드번호/주소 대신 학번/생년월일/학과를 채우고, 배경
    // 템플릿(templateFront/templateBack)을 다른 3종과 달리 classpath가 아니라 CardDesign이 가리키는
    // UploadFile(S3, 4-D 업로드 API가 등록)에서 내려받는다.
    private CardMemberData studentMemberData(Application application, ApplicationMember member, CardDesign design,
            CardPreviewRequest request, byte[] photo, String zodiacBranch) {
        byte[] templateFront = downloadUploadFile(design.getTemplateFrontId());
        byte[] templateBack = downloadUploadFile(design.getTemplateBackId());
        if (templateFront == null || templateBack == null) {
            // 4-D 업로드 API로 등록되지 않은 CardDesign(운영자가 row만 만들고 템플릿을 아직 안 올린
            // 경우) — 기존 CARD_DESIGN_NOT_FOUND를 재사용한다(STUDENT 전용 에러코드 신설 안 함, 정책 9번).
            throw new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND);
        }
        // Application.orientation(Orientation)과 CardDesignOrientation은 각자 다른 기능에서 독립적으로
        // 추가된 별개 enum이라 타입은 다르지만 값(LANDSCAPE/PORTRAIT)은 같다 — 이름으로 변환한다
        // (CardDesignService.listStudentCardDesigns()와 동일한 변환).
        CardDesignOrientation orientation = CardDesignOrientation.valueOf(application.getOrientation().name());
        return new CardMemberData(
                member.getSurname(), member.getName(), member.getEnglishName(), member.getChineseName(),
                member.getNameMeaning(), member.getNameInterpretation(), photo, member.getCardNumber(),
                member.getAddress(), request.getIssueDate(), zodiacBranch, null, null,
                application.getSchoolType(), orientation, member.getStudentId(), member.getDepartment(),
                member.getBirthDate(), templateFront, templateBack);
    }

    private ApplicationMember findMember(Long applicationId, Long memberId) {
        ApplicationMember member = applicationMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!member.getApplicationId().equals(applicationId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return member;
    }

    private void validateIssueDate(LocalDate issueDate, Application application) {
        LocalDate submittedDate = application.getCreatedAt().toLocalDate();
        if (issueDate.isBefore(submittedDate) || issueDate.isAfter(submittedDate.plusMonths(3))) {
            throw new CustomException(ErrorCode.CARD_ISSUE_DATE_OUT_OF_RANGE);
        }
    }

    private void validateNamingComplete(ApplicationMember member) {
        if (!StringUtils.hasText(member.getSurname()) || !StringUtils.hasText(member.getName())
                || !StringUtils.hasText(member.getNameMeaning())) {
            throw new CustomException(ErrorCode.NAMING_INCOMPLETE);
        }
    }

    private void validateCardNumber(ApplicationMember member) {
        if (!StringUtils.hasText(member.getCardNumber())) {
            throw new CustomException(ErrorCode.CARD_NOT_READY);
        }
    }

    // 정책 매트릭스(1-A): 단체 일반카드(HONOR_KOREAN/HONOR_CITIZEN/VISITOR)는 로고·직인 둘 다 필수.
    // 개인 일반카드는 없는 게 정상이라 검증하지 않는다. 학생증(STUDENT)은 4-C 확정대로 카드에
    // 로고·직인을 아예 그리지 않으므로(학교 크레스트는 학교별 템플릿 이미지 자체에 이미 포함) 이
    // 검증 자체를 건너뛴다(2026-08-31 정정 — 예전엔 "CardImageCompositor가 자체 거절"이 근거였는데
    // 이제 학생증 렌더링이 실제로 구현돼 있어 그 전제가 더 이상 맞지 않는다).
    private void validateIssuerAssets(Application application, CardTypeCode cardType) {
        if (cardType != CardTypeCode.STUDENT && !application.isIndividual()
                && (application.getLogoFileId() == null || application.getSealFileId() == null)) {
            throw new CustomException(ErrorCode.CARD_ISSUER_ASSETS_MISSING);
        }
    }

    private byte[] downloadUploadFile(Long uploadFileId) {
        if (uploadFileId == null) {
            return null;
        }
        UploadFile uploadFile = uploadFileRepository.findById(uploadFileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return storageService.download(uploadFile.getFilePath());
    }

    // 띠 이미지 결정 정책(admin-saju.md): 활성 만세력 결과가 없거나 연주가 불확실하면 거절한다.
    private String resolveZodiacBranch(ApplicationMember member) {
        ManseryeokResult result = manseryeokResultRepository
                .findByApplicationMemberIdAndActiveTrue(member.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MANSERYEOK_NOT_CONFIRMED));
        List<String> uncertain = readJson(result.getUncertainPillarsJson(), new TypeReference<List<String>>() { });
        if (uncertain != null && uncertain.contains("year")) {
            throw new CustomException(ErrorCode.MANSERYEOK_NOT_CONFIRMED);
        }
        Map<String, Map<String, String>> pillars = readJson(result.getConfirmedPillarsJson(),
                new TypeReference<Map<String, Map<String, String>>>() { });
        Map<String, String> year = pillars != null ? pillars.get("year") : null;
        if (year == null || !StringUtils.hasText(year.get("branch"))) {
            throw new CustomException(ErrorCode.MANSERYEOK_NOT_CONFIRMED);
        }
        return year.get("branch");
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, typeReference);
    }

    private void validateAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
