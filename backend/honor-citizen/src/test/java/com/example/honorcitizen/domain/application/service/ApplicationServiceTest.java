package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ApplicationServiceTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ReceiverRepository receiverRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SchoolRepository schoolRepository;

    @MockitoBean
    private StorageService storageService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private CardType honorKoreanCardType;
    private CardType studentCardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        schoolRepository.deleteAll();
        userRepository.deleteAll();

        user = User.createOAuthUser("member@example.com", "oauth-app", "google", "Member");
        user.agreeTerms(true, true, true);
        user = userRepository.save(user);
        honorKoreanCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증", null, BigDecimal.valueOf(30000)));
        studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증", null, BigDecimal.valueOf(20000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    private ApplicationCreateRequest mobileRequest(Long cardTypeId) {
        ApplicationCreateRequest request = fromJson(cardTypeId, "MOBILE", null, null, null);
        return request;
    }

    private ApplicationCreateRequest fromJson(Long cardTypeId, String issueType,
            Boolean sameAsApplicant, String studentId, String department) {
        return fromJson(cardTypeId, issueType, sameAsApplicant, studentId, department, null, null);
    }

    private ApplicationCreateRequest fromJson(Long cardTypeId, String issueType,
            Boolean sameAsApplicant, String studentId, String department,
            Orientation orientation, SchoolType schoolType) {
        return fromJson(cardTypeId, issueType, sameAsApplicant, studentId, department, orientation, schoolType, null);
    }

    private ApplicationCreateRequest fromJson(Long cardTypeId, String issueType,
            Boolean sameAsApplicant, String studentId, String department,
            Orientation orientation, SchoolType schoolType, String schoolName) {
        return fromJson(cardTypeId, issueType, sameAsApplicant, studentId, department,
                orientation, schoolType, schoolName, null);
    }

    private ApplicationCreateRequest fromJson(Long cardTypeId, String issueType,
            Boolean sameAsApplicant, String studentId, String department,
            Orientation orientation, SchoolType schoolType, String schoolName, Long schoolId) {
        // 학생증은 카드에 주소를 표시하지 않아 address를 보내면 거절되므로 학생증 카드타입일 때만 비운다.
        // (studentCardType이 아직 설정 전인 호출 경로는 없음 — setUp()에서 항상 먼저 초기화된다.)
        boolean isStudentCard = studentCardType != null && cardTypeId != null
                && cardTypeId.equals(studentCardType.getId());
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "%s",
                  %s
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  %s
                  "member": {
                    "englishName": "Hong Gildong",
                    "birthDate": "1990-05-15",
                    "nationality": "US",
                    "gender": "MALE"
                    %s
                    %s
                  }
                }
                """.formatted(
                cardTypeId, issueType,
                (orientation == null ? "" : "\"orientation\": \"%s\",".formatted(orientation))
                        + (schoolType == null ? "" : "\"schoolType\": \"%s\",".formatted(schoolType))
                        + (schoolName == null ? "" : "\"schoolName\": \"%s\",".formatted(schoolName))
                        + (schoolId == null ? "" : "\"schoolId\": %d,".formatted(schoolId)),
                sameAsApplicant == null ? "" : """
                        "receiver": {
                          "sameAsApplicant": %s,
                          "name": "김수령",
                          "phone": "010-9999-8888",
                          "zipCode": "06236",
                          "address": "서울특별시 강남구",
                          "detailAddress": "101동"
                        },
                        """.formatted(sameAsApplicant),
                studentId == null ? "" : ", \"studentId\": \"%s\", \"department\": \"%s\"".formatted(studentId, department),
                isStudentCard ? "" : ", \"address\": \"서울특별시 종로구 세종대로 1\"");
        return parse(json);
    }

    private ApplicationCreateRequest parse(String json) {
        try {
            return objectMapper.readValue(json, ApplicationCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes(300, 400, "jpg"));
    }

    private byte[] imageBytes(int width, int height, String format) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createIndividualSavesApplicationApplicantAndMemberWithoutReceiverForMobileOnly() {
        var response = applicationService.createIndividual(
                user.getId(), mobileRequest(honorKoreanCardType.getId()), photo(), null, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.isIndividual()).isTrue();
        assertThat(saved.getTotalQuantity()).isEqualTo(1);
        assertThat(saved.getCardDesignId()).isNull();
        assertThat(saved.getApplicationNumber()).startsWith("APP-");

        Applicant applicant = applicantRepository.findByApplicationId(saved.getId()).orElseThrow();
        assertThat(applicant.getEmail()).isEqualTo("member@example.com");
        assertThat(applicant.getName()).isEqualTo("홍길동");

        ApplicationMember member = applicationMemberRepository.findByApplicationId(saved.getId()).get(0);
        assertThat(member.getEnglishName()).isEqualTo("Hong Gildong");
        assertThat(member.getEmail()).isNull();
        assertThat(member.getPhone()).isNull();

        assertThat(receiverRepository.findByApplicationId(saved.getId())).isEmpty();
    }

    @Test
    void generateApplicationNumberNeverReusesSequenceEvenAfterExistingApplicationsAreDeleted() {
        var first = applicationService.createIndividual(
                user.getId(), mobileRequest(honorKoreanCardType.getId()), photo(), null, null);
        String firstNumber = first.getApplicationNumber();

        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();

        var second = applicationService.createIndividual(
                user.getId(), mobileRequest(honorKoreanCardType.getId()), photo(), null, null);
        String secondNumber = second.getApplicationNumber();

        assertThat(secondNumber).isNotEqualTo(firstNumber);
    }

    @Test
    void createIndividualSavesApplicantEmailFromRequestWhenProvided() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678", "email": "changed@example.com" },
                  "member": { "englishName": "Hong Gildong", "birthDate": "1990-05-15", "nationality": "US", "gender": "MALE", "address": "서울특별시 종로구 세종대로 1" }
                }
                """.formatted(honorKoreanCardType.getId());
        ApplicationCreateRequest request = parse(json);

        var response = applicationService.createIndividual(user.getId(), request, photo(), null, null);

        Applicant applicant = applicantRepository.findByApplicationId(response.getApplicationId()).orElseThrow();
        assertThat(applicant.getEmail()).isEqualTo("changed@example.com");
    }

    @Test
    void createIndividualFallsBackToUserEmailWhenApplicantEmailBlank() {
        var response = applicationService.createIndividual(
                user.getId(), mobileRequest(honorKoreanCardType.getId()), photo(), null, null);

        Applicant applicant = applicantRepository.findByApplicationId(response.getApplicationId()).orElseThrow();
        assertThat(applicant.getEmail()).isEqualTo("member@example.com");
    }

    @Test
    void createIndividualRequiresReceiverWhenPhysicalIssueRequested() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE_AND_PHYSICAL", null, null, null);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualUsesSubmittedReceiverAddressEvenWhenSameAsApplicantTrue() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE_AND_PHYSICAL", true, null, null);

        var response = applicationService.createIndividual(user.getId(), request, photo(), null, null);

        Receiver receiver = receiverRepository.findByApplicationId(response.getApplicationId()).orElseThrow();
        assertThat(receiver.getReceiverName()).isEqualTo("김수령");
        assertThat(receiver.getReceiverPhone()).isEqualTo("010-9999-8888");
        assertThat(receiver.getZipCode()).isEqualTo("06236");
        assertThat(receiver.getAddress()).isEqualTo("서울특별시 강남구");
        assertThat(receiver.getDetailAddress()).isEqualTo("101동");
    }

    @Test
    void createIndividualFallsBackToApplicantNameAndPhoneWhenReceiverFieldsBlank() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE_AND_PHYSICAL",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "receiver": { "sameAsApplicant": true, "zipCode": "06236", "address": "서울특별시 강남구", "detailAddress": "101동" },
                  "member": { "englishName": "Hong Gildong", "birthDate": "1990-05-15", "nationality": "US", "gender": "MALE", "address": "서울특별시 종로구 세종대로 1" }
                }
                """.formatted(honorKoreanCardType.getId());
        ApplicationCreateRequest request = parse(json);

        var response = applicationService.createIndividual(user.getId(), request, photo(), null, null);

        Receiver receiver = receiverRepository.findByApplicationId(response.getApplicationId()).orElseThrow();
        assertThat(receiver.getReceiverName()).isEqualTo("홍길동");
        assertThat(receiver.getReceiverPhone()).isEqualTo("010-1234-5678");
        assertThat(receiver.getAddress()).isEqualTo("서울특별시 강남구");
    }

    @Test
    void createIndividualUsesSubmittedReceiverWhenNotSameAsApplicant() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE_AND_PHYSICAL", false, null, null);

        var response = applicationService.createIndividual(user.getId(), request, photo(), null, null);

        Receiver receiver = receiverRepository.findByApplicationId(response.getApplicationId()).orElseThrow();
        assertThat(receiver.getReceiverName()).isEqualTo("김수령");
        assertThat(receiver.getZipCode()).isEqualTo("06236");
    }

    @Test
    void createIndividualForStudentCardRequiresStudentFieldsAndSchoolFiles() {
        ApplicationCreateRequest missingLogo = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과");

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), missingLogo, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualForStudentCardSucceedsWithAllRequiredFields() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "전북대학교");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, seal);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getLogoFileId()).isNotNull();
        assertThat(saved.getSealFileId()).isNotNull();
        assertThat(saved.getOrientation()).isEqualTo(Orientation.LANDSCAPE);
        assertThat(saved.getSchoolType()).isEqualTo(SchoolType.UNIVERSITY);
        assertThat(saved.getSchoolName()).isEqualTo("전북대학교");

        ApplicationMember member = applicationMemberRepository.findByApplicationId(saved.getId()).get(0);
        assertThat(member.getStudentId()).isEqualTo("20261234");
        assertThat(member.getDepartment()).isEqualTo("컴퓨터공학과");
    }

    @Test
    void createIndividualForStudentCardSucceedsWithoutSchoolSeal() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.PORTRAIT, SchoolType.UNIVERSITY, "전북대학교");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getLogoFileId()).isNotNull();
        assertThat(saved.getSealFileId()).isNull();
    }

    @Test
    void createIndividualHighSchoolSucceedsWithoutStudentIdOrDepartment() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, null, null,
                Orientation.LANDSCAPE, SchoolType.HIGH_SCHOOL, "전주고등학교");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getSchoolType()).isEqualTo(SchoolType.HIGH_SCHOOL);
        assertThat(saved.getSchoolName()).isEqualTo("전주고등학교");
        ApplicationMember member = applicationMemberRepository.findByApplicationId(saved.getId()).get(0);
        assertThat(member.getStudentId()).isNull();
        assertThat(member.getDepartment()).isNull();
    }

    @Test
    void createIndividualRejectsStudentCardMissingSchoolName() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsSchoolNameShorterThanFiveCharacters() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "전북대");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsSchoolNameLongerThanTwentyCharacters() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "가".repeat(21));
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsSchoolNameWithDisallowedCharacters() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "전북대학교(OO캠퍼스)");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualTrimsSchoolNameSurroundingWhitespaceBeforeSaving() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "  전북대학교  ");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getSchoolName()).isEqualTo("전북대학교");
    }

    @Test
    void createIndividualRejectsSchoolNameForNonStudentCard() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE", null, null, null,
                null, null, "전북대학교");

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsHighSchoolWithStudentIdAndDepartmentPresent() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.HIGH_SCHOOL);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsStudentCardMissingOrientation() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                null, SchoolType.UNIVERSITY);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsStudentCardMissingSchoolType() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과",
                Orientation.LANDSCAPE, null);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsOrientationOrSchoolTypeForNonStudentCard() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE", null, null, null,
                Orientation.LANDSCAPE, null);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsStudentIdWithNonDigitCharacters() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "2026-1234", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsStudentIdLongerThanTenDigits() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "202612345678", "컴퓨터공학과",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsReceiverWhenMobile() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE", true, null, null);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualRejectsStudentFieldsForNonStudentCard() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과");

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualThrowsNotFoundForUnknownCardType() {
        ApplicationCreateRequest request = mobileRequest(999999L);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void createIndividualRequiresPhoto() {
        ApplicationCreateRequest request = mobileRequest(honorKoreanCardType.getId());

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, null, null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    // 2026-08-19 정책 변경(WITHDRAW-3B): 탈퇴 계정은 즉시 하드 삭제되므로 User가 더 이상 탈퇴 상태를
    // 표현하지 않는다 — 이 서비스 레벨에서 "탈퇴한 사용자" 시나리오를 더 이상 재현할 수 없다
    // (findEligibleApplicationUser의 상태 체크 제거 참고). 하드 삭제 후에는 존재하지 않는 userId로
    // USER_NOT_FOUND가 나는 것으로 자연히 대체된다.

    @Test
    void createIndividualRejectsNonUserRoleBeforeSideEffects() {
        ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
        userRepository.save(user);

        assertUserValidationFailure(user.getId(), ErrorCode.FORBIDDEN);
    }

    @Test
    void createIndividualRejectsUserWithoutRequiredTermsBeforeSideEffects() {
        User unagreed = userRepository.save(
                User.createOAuthUser("unagreed@example.com", "oauth-unagreed", "google", "Unagreed"));

        assertUserValidationFailure(unagreed.getId(), ErrorCode.TERMS_NOT_AGREED);
    }

    private void assertUserValidationFailure(Long userId, ErrorCode expectedErrorCode) {
        ApplicationCreateRequest request = fromJson(
                studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(userId, request, photo(), logo, seal))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);

        verify(storageService, never()).upload(anyString(), any());
        assertThat(applicationRepository.count()).isZero();
        assertThat(applicantRepository.count()).isZero();
        assertThat(receiverRepository.count()).isZero();
        assertThat(applicationMemberRepository.count()).isZero();
    }

    @Test
    void createIndividualRejectsInvalidPhotoBeforeUploadOrSave() {
        ApplicationCreateRequest request = mobileRequest(honorKoreanCardType.getId());
        MockMultipartFile invalid = new MockMultipartFile(
                "photo", "face.png", "image/png", imageBytes(299, 400, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, invalid, null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE);

        verify(storageService, never()).upload(anyString(), any());
        assertThat(applicationRepository.count()).isZero();
    }

    @Test
    void createIndividualRejectsBlankStudentFieldsBeforeUploadOrSave() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "   ", "   ",
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY);
        MockMultipartFile logo = new MockMultipartFile(
                "schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile(
                "schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, seal))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        verify(storageService, never()).upload(anyString(), any());
        assertThat(applicationRepository.count()).isZero();
    }
    @Test
    void createIndividualValidatesUserBeforeCardType() {
        ApplicationCreateRequest request = mobileRequest(999999L);

        assertThatThrownBy(() -> applicationService.createIndividual(999999L, request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void createIndividualForMissingUserDoesNotUploadFilesOrSaveEntities() {
        ApplicationCreateRequest request = fromJson(
                studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(999999L, request, photo(), logo, seal))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(storageService, never()).upload(anyString(), any());
        assertThat(applicationRepository.count()).isZero();
        assertThat(applicantRepository.count()).isZero();
        assertThat(receiverRepository.count()).isZero();
        assertThat(applicationMemberRepository.count()).isZero();
    }

    @Test
    void createIndividualRejectsMissingCardAddressForNonStudentCard() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "member": { "englishName": "Hong Gildong", "birthDate": "1990-05-15", "nationality": "US", "gender": "MALE" }
                }
                """.formatted(honorKoreanCardType.getId());
        ApplicationCreateRequest request = parse(json);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThat(applicationRepository.count()).isZero();
    }

    @Test
    void createIndividualRejectsCardAddressForStudentCard() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "orientation": "LANDSCAPE",
                  "schoolType": "UNIVERSITY",
                  "schoolName": "전북대학교",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "member": {
                    "englishName": "Hong Gildong",
                    "birthDate": "1990-05-15",
                    "nationality": "US",
                    "gender": "MALE",
                    "studentId": "20261234",
                    "department": "컴퓨터공학과",
                    "address": "서울특별시 종로구 세종대로 1"
                  }
                }
                """.formatted(studentCardType.getId());
        ApplicationCreateRequest request = parse(json);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThat(applicationRepository.count()).isZero();
    }

    @Test
    void createIndividualPersistsCardAddressForNonStudentCard() {
        ApplicationCreateRequest request = mobileRequest(honorKoreanCardType.getId());

        var response = applicationService.createIndividual(user.getId(), request, photo(), null, null);

        ApplicationMember member = applicationMemberRepository.findByApplicationId(response.getApplicationId()).get(0);
        assertThat(member.getAddress()).isEqualTo("서울특별시 종로구 세종대로 1");
    }

    // --- School 검색select(schoolId) 관련 (TODO.md "학생증 카드" 4-A) ---

    @Test
    void createIndividualResolvesRegisteredSchoolAndIgnoresTamperedSchoolNameAndType() {
        School school = schoolRepository.save(School.create("전북대학교", SchoolType.UNIVERSITY));
        // 클라이언트가 schoolId와 함께 일부러 다른 schoolName/schoolType(고등학교)을 같이 보내도
        // 서버는 School(전북대학교, UNIVERSITY) 값을 그대로 강제해야 한다 — 위변조 차단 검증.
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null,
                "20261234", "컴퓨터공학과", Orientation.LANDSCAPE, SchoolType.HIGH_SCHOOL, "가짜대학", school.getId());
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getSchoolId()).isEqualTo(school.getId());
        assertThat(saved.getSchoolName()).isEqualTo("전북대학교");
        assertThat(saved.getSchoolType()).isEqualTo(SchoolType.UNIVERSITY);

        // schoolType이 실제로는 UNIVERSITY로 확정됐으므로 학번/학과도 정상 저장돼야 한다
        // (클라이언트가 보낸 HIGH_SCHOOL을 그대로 믿었다면 학번/학과가 있다는 이유로 거절됐을 것).
        ApplicationMember member = applicationMemberRepository.findByApplicationId(response.getApplicationId()).get(0);
        assertThat(member.getStudentId()).isEqualTo("20261234");
        assertThat(member.getDepartment()).isEqualTo("컴퓨터공학과");
    }

    @Test
    void createIndividualRejectsUnknownSchoolId() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null,
                "20261234", "컴퓨터공학과", Orientation.LANDSCAPE, null, null, 999999L);
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThat(applicationRepository.count()).isZero();
    }

    @Test
    void createIndividualSucceedsWithDirectInputSchoolWhenSchoolIdAbsent() {
        // schoolId 없이 기존처럼 schoolName/schoolType 직접입력으로도 여전히 성공해야 한다(회귀 확인).
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null,
                "20261234", "컴퓨터공학과", Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "전북대학교");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getSchoolId()).isNull();
        assertThat(saved.getSchoolName()).isEqualTo("전북대학교");
        assertThat(saved.getSchoolType()).isEqualTo(SchoolType.UNIVERSITY);
    }
}
