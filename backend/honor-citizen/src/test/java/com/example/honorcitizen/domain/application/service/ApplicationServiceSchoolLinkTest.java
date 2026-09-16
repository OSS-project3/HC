package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.SchoolLinkRequest;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 관리자 학교 연결(4-A-1) — 직접입력(schoolId=null)으로 접수된 STUDENT 신청을 이미 등록된 School에
// 연결한다. School 신규 생성은 이 기능 범위 밖이라 이미 저장된 School row만 대상으로 검증한다.
@SpringBootTest
class ApplicationServiceSchoolLinkTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long ownerId;
    private CardType studentCardType;
    private CardType honorCardType;
    private School highSchool;
    private School university;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        schoolRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("school-link-admin@example.com", "oauth-school-link-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("school-link-owner@example.com", "oauth-school-link-owner", "google", "Owner"));
        ownerId = owner.getId();

        studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-schoollink", null, BigDecimal.valueOf(20000)));
        honorCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-schoollink", null, BigDecimal.valueOf(30000)));

        highSchool = schoolRepository.save(School.create("전주고등학교", SchoolType.HIGH_SCHOOL));
        university = schoolRepository.save(School.create("전북대학교", SchoolType.UNIVERSITY));
    }

    private SchoolLinkRequest schoolLinkRequest(Long schoolId, Long applicationVersion) {
        SchoolLinkRequest request = new SchoolLinkRequest();
        ReflectionTestUtils.setField(request, "schoolId", schoolId);
        ReflectionTestUtils.setField(request, "applicationVersion", applicationVersion);
        return request;
    }

    private Application directInputStudentApplication(int seq) {
        Application application = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-SCHOOL-" + String.format("%04d", seq), studentCardType.getId(),
                IssueType.MOBILE, true, null, null,
                Orientation.LANDSCAPE, SchoolType.HIGH_SCHOOL, "직접입력고등학교", null));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(2008, 1, 1), "KR",
                null, null, com.example.honorcitizen.common.enums.Gender.MALE, null, null, null, "photos/a.jpg"));
        return application;
    }

    @Test
    void linkSchoolSucceedsForDirectInputStudentApplication() {
        Application application = directInputStudentApplication(1);
        SchoolLinkRequest request = schoolLinkRequest(highSchool.getId(), application.getVersion());

        applicationService.linkSchool(adminId, application.getId(), request);

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getSchoolId()).isEqualTo(highSchool.getId());
        assertThat(reloaded.getSchoolName()).isEqualTo("직접입력고등학교");
    }

    @Test
    void linkSchoolRejectsSchoolTypeMismatch() {
        Application application = directInputStudentApplication(2);
        SchoolLinkRequest request = schoolLinkRequest(university.getId(), application.getVersion());

        assertThatThrownBy(() -> applicationService.linkSchool(adminId, application.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void linkSchoolRejectsNonexistentSchoolId() {
        Application application = directInputStudentApplication(3);
        SchoolLinkRequest request = schoolLinkRequest(999_999L, application.getVersion());

        assertThatThrownBy(() -> applicationService.linkSchool(adminId, application.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHOOL_NOT_FOUND);
    }

    @Test
    void linkSchoolRejectsNonStudentCardType() {
        Application application = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-SCHOOL-0004", honorCardType.getId(), IssueType.MOBILE, true, null, null));
        SchoolLinkRequest request = schoolLinkRequest(highSchool.getId(), application.getVersion());

        assertThatThrownBy(() -> applicationService.linkSchool(adminId, application.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void linkSchoolRejectsAfterCardGenerated() {
        Application application = directInputStudentApplication(5);
        ApplicationMember member = applicationMemberRepository.findByApplicationId(application.getId()).get(0);
        member.assignCardImages("cards/front.png", "cards/back.png", LocalDate.of(2026, 9, 14));
        applicationMemberRepository.save(member);
        SchoolLinkRequest request = schoolLinkRequest(highSchool.getId(), application.getVersion());

        assertThatThrownBy(() -> applicationService.linkSchool(adminId, application.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHOOL_ALREADY_LOCKED);
    }

    @Test
    void linkSchoolRejectsForNonAdmin() {
        Application application = directInputStudentApplication(6);
        SchoolLinkRequest request = schoolLinkRequest(highSchool.getId(), application.getVersion());

        assertThatThrownBy(() -> applicationService.linkSchool(ownerId, application.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void linkSchoolRejectsVersionConflict() {
        Application application = directInputStudentApplication(7);
        SchoolLinkRequest request = schoolLinkRequest(highSchool.getId(), application.getVersion() + 1);

        assertThatThrownBy(() -> applicationService.linkSchool(adminId, application.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_VERSION_CONFLICT);
    }

    @Test
    void linkSchoolRejectsMissingApplication() {
        SchoolLinkRequest request = schoolLinkRequest(highSchool.getId(), 0L);

        assertThatThrownBy(() -> applicationService.linkSchool(adminId, 999_999L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }
}
