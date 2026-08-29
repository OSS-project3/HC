package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

// 실제 카드 발급 대상자 1명 = 1 row. 개인 신청은 Application 1개당 이 row가 1개, 단체 신청은
// 제출 ZIP 엑셀의 데이터 행 수만큼(N개) 생성된다. email/phone은 단체(createGroupRow)에서만
// 채워짐 — 개인 신청은 이 정보를 Applicant가 대신 갖고 있어서 여기선 null.
// address는 카드 표기용 주소 — 단체는 엑셀 행에서, 개인은 신청 입력값에서 채워진다(admin-saju.md 참고).
@Entity
@Table(name = "application_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "photo_number"}))
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

    // 카드에 인쇄되는 주소. 단체는 엑셀의 "주소" 컬럼, 개인은 신청 입력값(학생증 제외)에서 채워진다.
    @Column(length = 255)
    private String address;

    // 관리자가 작명 단계에서 확정하는 한글 성씨(1~2글자) — NAME_EDITING 중 nullable, completeNaming()
    // 실행 시 필수가 된다(검증은 1-B에서 추가). 카드의 한글 이름은 surname + name으로 조합한다.
    @Column(length = 10)
    private String surname;

    // 단체 신청 Excel의 고정 사진 번호(예: "001") — 단체는 필수, 개인은 항상 null.
    // (application_id, photo_number) 조합이 유일해야 관리자 카드번호 일괄 입력에서 행을 정확히 매칭할 수 있다.
    @Column(length = 10)
    private String photoNumber;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String nationality;

    // 사주정보용 — 선택 입력
    private LocalTime birthTime;

    // 태어난 도시/지역 — 신청 입력에서는 필수, 기존 데이터 호환을 위해 DB nullable 유지
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
    //학번
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

    // 기존 호출부(카드 표기 주소 없이 생성하던 개인 신청 흐름 이전 코드)와의 하위 호환용.
    public static ApplicationMember createIndividual(Long applicationId, String englishName,
            LocalDate birthDate, String nationality, LocalTime birthTime, String birthRegion,
            Gender gender, LocalDate entryDate, String studentId, String department, String photoPath) {
        return createIndividual(applicationId, englishName, birthDate, nationality, birthTime, birthRegion,
                gender, entryDate, studentId, department, photoPath, null);
    }

    // address: 카드에 인쇄되는 주소 — 학생증을 제외한 카드종류는 개인 신청도 신청 입력값으로 받아 저장한다.
    public static ApplicationMember createIndividual(Long applicationId, String englishName,
            LocalDate birthDate, String nationality, LocalTime birthTime, String birthRegion,
            Gender gender, LocalDate entryDate, String studentId, String department, String photoPath,
            String address) {
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
        member.address = address;
        return member;
    }

    // 기존 호출부(사진 번호 없이 생성하던 코드)와의 하위 호환용.
    public static ApplicationMember createGroupRow(Long applicationId, String englishName,
            LocalDate birthDate, String nationality, LocalTime birthTime, String birthRegion,
            Gender gender, LocalDate entryDate, String email, String phone, String address,
            String studentId, String department, String photoPath) {
        return createGroupRow(applicationId, englishName, birthDate, nationality, birthTime, birthRegion,
                gender, entryDate, email, phone, address, studentId, department, photoPath, null);
    }

    // photoNumber: 단체 신청 Excel의 고정 사진 번호(BulkMemberRow.photoNumber) — 단체는 필수.
    public static ApplicationMember createGroupRow(Long applicationId, String englishName,
            LocalDate birthDate, String nationality, LocalTime birthTime, String birthRegion,
            Gender gender, LocalDate entryDate, String email, String phone, String address,
            String studentId, String department, String photoPath, String photoNumber) {
        ApplicationMember member = createIndividual(applicationId, englishName, birthDate, nationality,
                birthTime, birthRegion, gender, entryDate, studentId, department, photoPath);
        member.email = email;
        member.phone = phone;
        member.address = address;
        member.photoNumber = photoNumber;
        return member;
    }

    // 작명 단계(saju 프로그램에서 확정된 결과를 관리자가 반영) — 이미 값이 있어도 덮어쓴다.
    // 성씨는 이 경로로 저장하지 않는다 — 엑셀 왕복의 "사주이름"은 외부 saju 프로그램이 돌려주는
    // 값이라 성씨·의미 구분이 없다(admin-saju.md 성씨 분리 정책은 관리자 인앱 확정 전용).
    public void assignKoreanName(String name, String chineseName) {
        validateNameFormat(name, chineseName);
        this.name = name;
        this.chineseName = chineseName;
    }

    // 관리자 대시보드 인앱 작명 확정 — 성씨·뜻·훈음까지 함께 저장한다(모두 덮어쓴다).
    // surname은 NAME_EDITING 중에는 null을 허용한다(completeNaming() 집계 검증은 Service에서 수행).
    public void assignKoreanName(String surname, String name, String chineseName,
            String nameMeaning, String nameInterpretation) {
        validateNameFormat(name, chineseName);
        if (surname != null) {
            validateSurnameFormat(surname);
        }
        this.surname = surname;
        this.name = name;
        this.chineseName = chineseName;
        this.nameMeaning = nameMeaning;
        this.nameInterpretation = nameInterpretation;
    }

    // 이름은 성씨를 제외한 한글 2~3글자만 허용한다. 한자가 있으면 한글 이름과 Unicode 글자 수가 같아야 한다.
    private static void validateNameFormat(String name, String chineseName) {
        if (name == null || !name.matches("[가-힣]{2,3}")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (chineseName != null && !chineseName.isBlank()
                && chineseName.codePointCount(0, chineseName.length()) != name.codePointCount(0, name.length())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // 성씨는 한글 1~2글자만 허용한다.
    private static void validateSurnameFormat(String surname) {
        if (!surname.matches("[가-힣]{1,2}")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    public void updatePhoto(String photoPath) {
        this.photoPath = photoPath;
    }

    public void clearPhoto() {
        this.photoPath = null;
    }

    // 관리자가 직접 입력·확정하는 카드번호(admin-saju.md "관리자 카드번호 입력 정책") — 서버가 채번하지 않는다.
    // 형식은 ROK-XXXXX-XXXX(5자리-4자리 숫자). 최초 카드 생성(cardFrontPath 확정) 이후에는 값이 바뀌는
    // 변경만 거절한다 — 같은 번호 재저장(멱등)은 항상 허용. DB UNIQUE 제약은 최종 방어선으로 Service가
    // DataIntegrityViolationException을 CARD_NUMBER_ALREADY_USED로 변환한다.
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("ROK-\\d{5}-\\d{4}");

    public static boolean isValidCardNumberFormat(String cardNumber) {
        return cardNumber != null && CARD_NUMBER_PATTERN.matcher(cardNumber).matches();
    }

    // 카드가 이미 생성됐는지(=최초 카드 생성 성공 여부)의 판단 기준 — cardFrontPath 확정 여부.
    public boolean isCardGenerated() {
        return this.cardFrontPath != null;
    }

    public void assignCardNumber(String cardNumber) {
        if (!isValidCardNumberFormat(cardNumber)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (isCardGenerated() && !cardNumber.equals(this.cardNumber)) {
            throw new CustomException(ErrorCode.CARD_NUMBER_LOCKED);
        }
        this.cardNumber = cardNumber;
    }

    // 카드 생성/재생성(3-B) 확정 — 앞·뒷면 S3 key와 발급일자를 함께 저장한다. 재생성이어도 그대로
    // 덮어쓴다 — 기존 파일 삭제는 이 메서드가 아니라 호출부(CardGenerationPersistenceService)가
    // 트랜잭션 커밋 이후에 처리한다(신규 파일 선저장 → commit → 기존 파일 후삭제 정책).
    public void assignCardImages(String cardFrontPath, String cardBackPath, LocalDate issueDate) {
        this.cardFrontPath = cardFrontPath;
        this.cardBackPath = cardBackPath;
        this.issueDate = issueDate;
    }
}
