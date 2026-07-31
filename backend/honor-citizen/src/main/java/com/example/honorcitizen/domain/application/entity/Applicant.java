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

@Entity
@Table(name = "applicants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 10)
    private String postalCode;

    private String address1;

    private String address2;

    @Column(length = 200)
    private String organizationName;

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
