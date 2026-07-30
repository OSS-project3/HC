package com.example.honorcitizen.domain.koreanname.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "korean_names")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KoreanName extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, length = 10)
    private String familyName;

    @Column(nullable = false, length = 20)
    private String givenName;

    @Column(nullable = false, length = 30)
    private String fullNameKo;

    @Column(nullable = false, length = 100)
    private String fullNameEn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @Column(length = 100)
    private String nameOrigin;

    public static KoreanName create(Long applicationId, String familyName, String givenName,
                                    String fullNameEn, String meaning, String nameOrigin) {
        KoreanName koreanName = new KoreanName();
        koreanName.applicationId = applicationId;
        koreanName.familyName = familyName;
        koreanName.givenName = givenName;
        koreanName.fullNameKo = familyName + givenName;
        koreanName.fullNameEn = fullNameEn;
        koreanName.meaning = meaning;
        koreanName.nameOrigin = nameOrigin;
        return koreanName;
    }

    public void update(String familyName, String givenName, String fullNameEn,
                       String meaning, String nameOrigin) {
        this.familyName = familyName;
        this.givenName = givenName;
        this.fullNameKo = familyName + givenName;
        this.fullNameEn = fullNameEn;
        this.meaning = meaning;
        this.nameOrigin = nameOrigin;
    }
}
