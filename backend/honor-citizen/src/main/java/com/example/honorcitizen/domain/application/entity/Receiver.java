package com.example.honorcitizen.domain.application.entity;

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

// 실물카드 배송지. issueType=MOBILE_AND_PHYSICAL일 때만 존재(applicationId unique라 0개 또는 1개).
@Entity
@Table(name = "receivers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receiver extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    // sameAsApplicant=true여도 요청값이 비어있을 때만 Applicant 이름으로 대체(fallback) — 항상 신청인 이름인 건 아님
    @Column(length = 100)
    private String receiverName;

    // receiverName과 동일한 fallback 규칙 적용
    @Column(length = 20)
    private String receiverPhone;

    // 어디서도 값을 채우지 않는 미사용 컬럼
    @Column(length = 100)
    private String country;

    // sameAsApplicant 여부와 무관하게 항상 요청값을 그대로 저장 — 배송지는 자동 복사되지 않음(정책)
    @Column(length = 10)
    private String zipCode;

    // zipCode와 동일하게 항상 요청값 사용
    private String address;

    private String detailAddress;

    private String deliveryRequest;

    // 단체 신청에서만 값이 들어감(receiverRequest.organizationName)
    @Column(length = 200)
    private String organizationName;

    // 단체 신청에서만 값이 들어감(receiverRequest.department)
    @Column(length = 100)
    private String department;

    public static Receiver create(Long applicationId, String receiverName, String receiverPhone,
            String zipCode, String address, String detailAddress, String deliveryRequest,
            String organizationName, String department) {
        Receiver receiver = new Receiver();
        receiver.applicationId = applicationId;
        receiver.receiverName = receiverName;
        receiver.receiverPhone = receiverPhone;
        receiver.zipCode = zipCode;
        receiver.address = address;
        receiver.detailAddress = detailAddress;
        receiver.deliveryRequest = deliveryRequest;
        receiver.organizationName = organizationName;
        receiver.department = department;
        return receiver;
    }

    // sameAsApplicant=true여도 배송지는 항상 요청값을 써야 한다는 정책으로 바뀐 뒤로는
    // ApplicationPersistenceService에서 이 메서드를 더 이상 호출하지 않음(현재 미사용, 삭제는 checklist.md 범위 밖이라 보류).
    public static Receiver copyFromApplicant(Long applicationId, Applicant applicant) {
        return create(applicationId, applicant.getName(), applicant.getPhone(),
                null, null, null, null,
                applicant.getOrganizationName(), applicant.getDepartment());
    }
}
