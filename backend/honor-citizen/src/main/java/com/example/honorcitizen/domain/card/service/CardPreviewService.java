package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardSide;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 2-C: 저장 없는 카드 미리보기. DB row·S3 object를 생성하지 않고 실제 신청 데이터로 렌더링만 한다
// (admin-saju.md/TODO.md "관리자 작명 확정·카드 제작 구현 계획" 2-C). CardImageCompositor/
// CardMemberData가 패키지 프라이빗이라 이 서비스도 domain.card.service 패키지에 둔다.
@Service
@RequiredArgsConstructor
public class CardPreviewService {

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

    @Transactional(readOnly = true)
    public byte[] preview(Long adminId, Long applicationId, Long memberId, CardPreviewRequest request) {
        validateAdmin(adminId);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (application.getStatus() != ApplicationStatus.PRODUCTION_READY) {
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
        validateIssueDate(request.getIssueDate(), application);
        validateNamingComplete(member);
        validateCardNumber(member);
        validateIssuerAssets(application);

        String zodiacBranch = resolveZodiacBranch(member);
        byte[] photo = member.getPhotoPath() != null ? storageService.download(member.getPhotoPath()) : null;
        byte[] logo = downloadUploadFile(application.getLogoFileId());
        byte[] seal = downloadUploadFile(application.getSealFileId());

        CardMemberData data = new CardMemberData(
                member.getSurname(), member.getName(), member.getEnglishName(), member.getChineseName(),
                member.getNameMeaning(), member.getNameInterpretation(), photo, member.getCardNumber(),
                member.getAddress(), request.getIssueDate(), zodiacBranch, logo, seal);

        CardTypeCode code = cardType.getCode();
        return request.getSide() == CardSide.BACK
                ? compositor.composeBack(code, design.getDesignNumber(), data)
                : compositor.composeFront(code, design.getDesignNumber(), data);
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

    // 정책 매트릭스(1-A): 단체 일반카드는 로고·직인 둘 다 필수. 개인 일반카드는 없는 게 정상이라
    // 검증하지 않는다(학생증은 이 API에 오면 CardImageCompositor가 자체적으로 거절한다).
    private void validateIssuerAssets(Application application) {
        if (!application.isIndividual()
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
