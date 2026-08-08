package com.example.honorcitizen.domain.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 신청서를 "제출한 사람"(개인 신청 본인 또는 단체 담당자). applicationId가 unique라 Application 1개당 1개만 존재
// — 실제 카드 발급 대상자(ApplicationMember, 1개 또는 N개)와는 다른 엔티티.
@Entity
@Table(name = "applicants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 Application의 FK. unique라 Application 1개당 Applicant는 정확히 1개.
    @Column(nullable = false, unique = true)
    private Long applicationId;

    // 신청인 이름(개인은 본인, 단체는 담당자)
    @Column(nullable = false, length = 100)
    private String name;

    // 요청에 email이 있으면 그 값, 없으면 User.email로 대체(fallback) — ApplicationService에서 결정 후 저장
    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    // postalCode/address1/address2: 어디서도 값을 채우지 않는 미사용 컬럼(생성 메서드에 없음)
    @Column(length = 10)
    private String postalCode;

    private String address1;

    private String address2;

    // 단체 신청에서만 값이 들어감(createGroup) — 개인 신청은 항상 null
    @Column(length = 200)
    private String organizationName;

    // 단체 신청에서만 값이 들어감(createGroup) — 개인 신청은 항상 null
    @Column(length = 100)
    private String department;

    public static Applicant createIndividual(Long applicationId, String name, String email, String phone) {
        Applicant applicant = new Applicant();
        applicant.applicationId = applicationId;
        applicant.name = name;
        applicant.email = email;
        applicant.phone = phone;
        return applicant;
    }

    public static Applicant createGroup(Long applicationId, String name, String email, String phone,
            String organizationName, String department) {
        Applicant applicant = createIndividual(applicationId, name, email, phone);
        applicant.organizationName = organizationName;
        applicant.department = department;
        return applicant;
    }
}
