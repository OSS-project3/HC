package com.example.honorcitizen.domain.application.entity;

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

// 이름(한글+한자)별 관리자 선택 이력 카운트. 관리자가 작명 화면에서 이름을 확정할 때마다 +1 한다.
// 프론트 localStorage가 아니라 DB에 보관해 데이터 유출 없이 집계한다.
@Entity
@Table(name = "name_selection_stats", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "hanja"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NameSelectionStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 40)
    private String hanja;

    @Column(nullable = false)
    private int selectedCount;

    private NameSelectionStat(String name, String hanja) {
        this.name = name;
        this.hanja = hanja;
        this.selectedCount = 0;
    }

    public static NameSelectionStat create(String name, String hanja) {
        return new NameSelectionStat(name, hanja);
    }

    public void increment() {
        this.selectedCount += 1;
    }
}
