package com.example.honorcitizen.domain.koreanname.dto;

import com.example.honorcitizen.domain.koreanname.entity.KoreanName;
import lombok.Getter;

@Getter
public class KoreanNameRegisterResponse {

    private final Long koreanNameId;
    private final String fullNameKo;
    private final String fullNameEn;
    private final String meaning;
    private final String nameOrigin;
    private final String applicationStatus;

    private KoreanNameRegisterResponse(Long koreanNameId, String fullNameKo, String fullNameEn,
                                        String meaning, String nameOrigin, String applicationStatus) {
        this.koreanNameId = koreanNameId;
        this.fullNameKo = fullNameKo;
        this.fullNameEn = fullNameEn;
        this.meaning = meaning;
        this.nameOrigin = nameOrigin;
        this.applicationStatus = applicationStatus;
    }

    public static KoreanNameRegisterResponse of(KoreanName koreanName) {
        return new KoreanNameRegisterResponse(
                koreanName.getId(),
                koreanName.getFullNameKo(),
                koreanName.getFullNameEn(),
                koreanName.getMeaning(),
                koreanName.getNameOrigin(),
                "REVIEWING"
        );
    }
}
