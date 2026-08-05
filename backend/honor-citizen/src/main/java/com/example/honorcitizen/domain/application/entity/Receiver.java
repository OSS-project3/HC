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

    @Column(length = 100)
    private String receiverName;

    @Column(length = 20)
    private String receiverPhone;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String zipCode;

    private String address;

    private String detailAddress;

    private String deliveryRequest;

    @Column(length = 200)
    private String organizationName;

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

    public static Receiver copyFromApplicant(Long applicationId, Applicant applicant) {
        return create(applicationId, applicant.getName(), applicant.getPhone(),
                null, null, null, null,
                applicant.getOrganizationName(), applicant.getDepartment());
    }
}
