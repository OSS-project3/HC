package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.Gender;
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

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "application_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String englishName;

    @Column(length = 50)
    private String chineseName;

    @Column(columnDefinition = "TEXT")
    private String nameMeaning;

    @Column(columnDefinition = "TEXT")
    private String nameInterpretation;

    @Column(length = 500)
    private String photoPath;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String nationality;

    private LocalTime birthTime;

    @Column(length = 200)
    private String birthRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private LocalDate entryDate;

    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String studentId;

    @Column(length = 100)
    private String department;

    private LocalDate issueDate;

    @Column(unique = true, length = 30)
    private String cardNumber;

    @Column(length = 500)
    private String cardFrontPath;

    @Column(length = 500)
    private String cardBackPath;

    public static ApplicationMember createIndividual(Long applicationId, String englishName,
            LocalDate birthDate, String nationality, LocalTime birthTime, String birthRegion,
            Gender gender, LocalDate entryDate, String studentId, String department, String photoPath) {
        ApplicationMember member = new ApplicationMember();
        member.applicationId = applicationId;
        member.englishName = englishName;
        member.birthDate = birthDate;
        member.nationality = nationality;
        member.birthTime = birthTime;
        member.birthRegion = birthRegion;
        member.gender = gender;
        member.entryDate = entryDate;
        member.studentId = studentId;
        member.department = department;
        member.photoPath = photoPath;
        return member;
    }

    public static ApplicationMember createGroupRow(Long applicationId, String englishName,
            LocalDate birthDate, String nationality, LocalTime birthTime, String birthRegion,
            Gender gender, LocalDate entryDate, String email, String phone, String address,
            String studentId, String department, String photoPath) {
        ApplicationMember member = createIndividual(applicationId, englishName, birthDate, nationality,
                birthTime, birthRegion, gender, entryDate, studentId, department, photoPath);
        member.email = email;
        member.phone = phone;
        member.address = address;
        return member;
    }

    public void updatePhoto(String photoPath) {
        this.photoPath = photoPath;
    }
}
