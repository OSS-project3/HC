package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
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
        userRepository.deleteAll();

        user = User.createNewUser("member@example.com", "oauth-app", "google", "Member");
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
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "%s",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  %s
                  "member": {
                    "englishName": "Hong Gildong",
                    "birthDate": "1990-05-15",
                    "nationality": "US",
                    "gender": "MALE"
                    %s
                  }
                }
                """.formatted(
                cardTypeId, issueType,
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
                studentId == null ? "" : ", \"studentId\": \"%s\", \"department\": \"%s\"".formatted(studentId, department));
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
    void createIndividualRequiresReceiverWhenPhysicalIssueRequested() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE_AND_PHYSICAL", null, null, null);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void createIndividualCopiesReceiverFromApplicantWhenSameAsApplicantTrue() {
        ApplicationCreateRequest request = fromJson(honorKoreanCardType.getId(), "MOBILE_AND_PHYSICAL", true, null, null);

        var response = applicationService.createIndividual(user.getId(), request, photo(), null, null);

        Receiver receiver = receiverRepository.findByApplicationId(response.getApplicationId()).orElseThrow();
        assertThat(receiver.getReceiverName()).isEqualTo("홍길동");
        assertThat(receiver.getReceiverPhone()).isEqualTo("010-1234-5678");
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
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, seal);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getLogoFileId()).isNotNull();
        assertThat(saved.getSealFileId()).isNotNull();

        ApplicationMember member = applicationMemberRepository.findByApplicationId(saved.getId()).get(0);
        assertThat(member.getStudentId()).isEqualTo("20261234");
        assertThat(member.getDepartment()).isEqualTo("컴퓨터공학과");
    }

    @Test
    void createIndividualForStudentCardSucceedsWithoutSchoolSeal() {
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "20261234", "컴퓨터공학과");
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));

        var response = applicationService.createIndividual(user.getId(), request, photo(), logo, null);

        Application saved = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(saved.getLogoFileId()).isNotNull();
        assertThat(saved.getSealFileId()).isNull();
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

    @Test
    void createIndividualRejectsWithdrawnUserBeforeSideEffects() {
        user.withdraw();
        userRepository.save(user);

        assertUserValidationFailure(user.getId(), ErrorCode.ALREADY_WITHDRAWN);
    }

    @Test
    void createIndividualRejectsNonUserRoleBeforeSideEffects() {
        ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
        userRepository.save(user);

        assertUserValidationFailure(user.getId(), ErrorCode.FORBIDDEN);
    }

    @Test
    void createIndividualRejectsUserWithoutRequiredTermsBeforeSideEffects() {
        User unagreed = userRepository.save(
                User.createNewUser("unagreed@example.com", "oauth-unagreed", "google", "Unagreed"));

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
        ApplicationCreateRequest request = fromJson(studentCardType.getId(), "MOBILE", null, "   ", "   ");
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
}
