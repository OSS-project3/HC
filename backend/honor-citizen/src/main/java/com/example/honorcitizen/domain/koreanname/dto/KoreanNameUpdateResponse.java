package com.example.honorcitizen.domain.koreanname.dto;

import com.example.honorcitizen.domain.koreanname.entity.KoreanName;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class KoreanNameUpdateResponse {

    private final Long koreanNameId;
    private final String fullNameKo;
    private final String fullNameEn;
    private final String meaning;
    private final String nameOrigin;
    private final LocalDateTime updatedAt;

    private KoreanNameUpdateResponse(Long koreanNameId, String fullNameKo, String fullNameEn,
                                      String meaning, String nameOrigin, LocalDateTime updatedAt) {
        this.koreanNameId = koreanNameId;
        this.fullNameKo = fullNameKo;
        this.fullNameEn = fullNameEn;
        this.meaning = meaning;
        this.nameOrigin = nameOrigin;
        this.updatedAt = updatedAt;
    }

    public static KoreanNameUpdateResponse from(KoreanName koreanName) {
        return new KoreanNameUpdateResponse(
                koreanName.getId(),
                koreanName.getFullNameKo(),
                koreanName.getFullNameEn(),
                koreanName.getMeaning(),
                koreanName.getNameOrigin(),
                koreanName.getUpdatedAt()
        );
    }
}
