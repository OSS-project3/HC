package com.example.honorcitizen.domain.school.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.SchoolType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 학생증 지원 대상 학교 마스터 목록 — CardDesign(템플릿) 등록 여부와 무관하게 존재한다(TODO.md
// "학생증 카드 — 작명~카드 제작 확장 계획" 4-A). 관리자 업로드 UI는 이번 범위에 없어 개발/운영자가
// 직접 row를 등록한다. schoolType은 학교 자체의 고정 속성이라 신청마다 입력받지 않고 이 값을 그대로 쓴다.
@Entity
@Table(name = "schools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class School extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchoolType schoolType;

    public static School create(String name, SchoolType schoolType) {
        School school = new School();
        school.name = name;
        school.schoolType = schoolType;
        return school;
    }
}
