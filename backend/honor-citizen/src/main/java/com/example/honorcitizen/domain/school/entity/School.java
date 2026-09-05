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

    // 나이스/공공데이터포털 원본 데이터의 학교 식별 코드(예: 고등학교의 행정표준코드). name은 화면에
    // 보여줄 표시명(동명이교면 지역명이 이미 붙은 값)이라 재실행 시 idempotency 키로 못 쓴다 — 이
    // 컬럼이 실제 식별자다. University 시딩(SchoolSeeder)처럼 원본에 코드가 없는 시드는 null로 둔다
    // (UNIQUE 인덱스는 NULL끼리 서로 다른 값으로 취급하므로 여러 school이 null이어도 안전 — schema.sql
    // 참고).
    @Column(name = "admin_standard_code", length = 20)
    private String adminStandardCode;

    public static School create(String name, SchoolType schoolType) {
        return createWithCode(name, schoolType, null);
    }

    public static School createWithCode(String name, SchoolType schoolType, String adminStandardCode) {
        School school = new School();
        school.name = name;
        school.schoolType = schoolType;
        school.adminStandardCode = adminStandardCode;
        return school;
    }
}
