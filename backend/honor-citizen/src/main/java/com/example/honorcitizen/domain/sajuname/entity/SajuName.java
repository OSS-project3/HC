package com.example.honorcitizen.domain.sajuname.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사주 작명용 이름 사전(saju 레포 names.json + 소유자 제공 데이터 병합, 700개).
// jawon/eum은 글자 수만큼 오행이 붙는데 이름이 항상 2글자는 아니어서(외자 1글자·간혹 3~4글자도 있음)
// 고정 컬럼 대신 콤마로 이은 문자열로 저장한다(예: "화,화").
@Entity
@Table(name = "saju_names", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "hanja"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SajuName extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 40)
    private String hanja;

    @Column(length = 100)
    private String roman;

    @Column(length = 500)
    private String reading;

    @Column(columnDefinition = "TEXT")
    private String meaning;

    // 자원오행 — 글자별 오행을 콤마로 이음(예: "화,화")
    @Column(nullable = false, length = 20)
    private String jawon;

    // 발음오행 — 글자별 오행을 콤마로 이음
    @Column(nullable = false, length = 20)
    private String eum;

    public static SajuName create(String name, String hanja, String roman, String reading,
            String meaning, String jawon, String eum) {
        SajuName sajuName = new SajuName();
        sajuName.name = name;
        sajuName.hanja = hanja;
        sajuName.roman = roman;
        sajuName.reading = reading;
        sajuName.meaning = meaning;
        sajuName.jawon = jawon;
        sajuName.eum = eum;
        return sajuName;
    }
}
