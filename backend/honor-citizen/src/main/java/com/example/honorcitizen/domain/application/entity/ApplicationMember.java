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

// 실제 카드 발급 대상자 1명 = 1 row. 개인 신청은 Application 1개당 이 row가 1개, 단체 신청은
// 제출 ZIP 엑셀의 데이터 행 수만큼(N개) 생성된다. email/phone/address는 단체(createGroupRow)에서만
// 채워짐 — 개인 신청은 이 정보를 Applicant가 대신 갖고 있어서 여기선 null.
@Entity
@Table(name = "application_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 Application의 FK. Application 1개당 여러 개 존재 가능(개인은 1개, 단체는 N개) — unique 제약 없음
    @Column(nullable = false)
    private Long applicationId;

    // 한글(한국식) 이름 — 신청 시점엔 채워지지 않고 작명 단계(범위 밖)에서 채워짐
    @Column(length = 100)
    private String name;

    // 영문 이름 — 신청 시점에 필수로 입력받는 실제 값
    @Column(length = 100)
    private String englishName;

    // 한자 이름 — name과 마찬가지로 작명 단계(범위 밖)에서 채워짐
    @Column(length = 50)
    private String chineseName;

    // 이름의 의미 설명 — 작명 단계(범위 밖)에서 채워짐
    @Column(columnDefinition = "TEXT")
    private String nameMeaning;

    // 이름 해석/설명 — 작명 단계(범위 밖)에서 채워짐
    @Column(columnDefinition = "TEXT")
    private String nameInterpretation;

    // 얼굴 사진 저장 경로(스토리지 key). 개인은 요청 파일, 단체는 ZIP 안 사진에서 옴
    @Column(length = 500)
    private String photoPath;

    // 단체 신청(createGroupRow)에서만 채워짐 — 엑셀의 "주소" 컬럼. 개인은 항상 null
    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String nationality;

    // 사주정보용 — 선택 입력
    private LocalTime birthTime;

    // 사주정보용 — 선택 입력
    @Column(length = 200)
    private String birthRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // 입국 예정일. 단체는 행별 값이 없으면 엑셀 1행의 "공통 입국날짜"로 대체(BulkExcelParser)
    private LocalDate entryDate;

    // 단체 신청(createGroupRow)에서만 채워짐 — 개인은 Applicant.email을 대신 쓰므로 항상 null
    private String email;

    // 단체 신청(createGroupRow)에서만 채워짐 — 개인은 Applicant.phone을 대신 쓰므로 항상 null
    @Column(length = 20)
    private String phone;

    // 학생증일 때만 필수. 최대 10자·숫자만 허용(개인은 ApplicationService, 단체는 BulkExcelParser에서 검증)
    @Column(length = 10)
    private String studentId;

    // 학생증일 때만 필수(department 자체를 제외할지는 아직 미결정 — PENDING_DECISIONS.md 참고)
    @Column(length = 100)
    private String department;

    // issueDate ~ cardBackPath는 신청 시점엔 비어있고, 심사/작명/카드제작 단계(이번 리팩터링 범위 밖)에서 채워짐
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
