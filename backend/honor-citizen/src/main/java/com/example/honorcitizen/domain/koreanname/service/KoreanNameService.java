package com.example.honorcitizen.domain.koreanname.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameRegisterRequest;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameRegisterResponse;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameUpdateRequest;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameUpdateResponse;
import com.example.honorcitizen.domain.koreanname.entity.KoreanName;
import com.example.honorcitizen.domain.koreanname.repository.KoreanNameRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KoreanNameService {

    private final KoreanNameRepository koreanNameRepository;
    private final ApplicationRepository applicationRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;

    @Transactional
    public KoreanNameRegisterResponse registerKoreanName(Long adminId, Long applicationId,
            KoreanNameRegisterRequest request) {
        validateApplicationReviewing(applicationId);
        validateNameNotExists(applicationId);

        KoreanName koreanName = KoreanName.create(
                applicationId,
                request.getFamilyName(),
                request.getGivenName(),
                request.getFullNameEn(),
                request.getMeaning(),
                request.getNameOrigin()
        );
        koreanNameRepository.save(koreanName);

        adminActivityLogRepository.save(AdminActivityLog.create(
                adminId, AdminActivityLog.KOREAN_NAME_REGISTER, applicationId,
                request.getFullNameEn()));

        return KoreanNameRegisterResponse.of(koreanName);
    }

    @Transactional
    public KoreanNameUpdateResponse updateKoreanName(Long adminId, Long applicationId,
            KoreanNameUpdateRequest request) {
        validateApplicationReviewing(applicationId);
        KoreanName koreanName = findByApplicationId(applicationId);

        koreanName.update(
                request.getFamilyName(),
                request.getGivenName(),
                request.getFullNameEn(),
                request.getMeaning(),
                request.getNameOrigin()
        );
        koreanNameRepository.save(koreanName);

        adminActivityLogRepository.save(AdminActivityLog.create(
                adminId, AdminActivityLog.KOREAN_NAME_UPDATE, applicationId,
                request.getFullNameEn()));

        return KoreanNameUpdateResponse.from(koreanName);
    }

    @Transactional(readOnly = true)
    public Optional<KoreanName> findOptionalByApplicationId(Long applicationId) {
        return koreanNameRepository.findByApplicationId(applicationId);
    }

    private KoreanName findByApplicationId(Long applicationId) {
        return koreanNameRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.KOREAN_NAME_NOT_FOUND));
    }

    private void validateApplicationReviewing(Long applicationId) {
        ApplicationStatus status = applicationRepository.findStatusById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        Optional.of(status)
                .filter(s -> s == ApplicationStatus.REVIEWING)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_APPLICATION_STATUS));
    }

    private void validateNameNotExists(Long applicationId) {
        Optional.of(applicationId)
                .filter(id -> !koreanNameRepository.existsByApplicationId(id))
                .orElseThrow(() -> new CustomException(ErrorCode.KOREAN_NAME_ALREADY_EXISTS));
    }
}
