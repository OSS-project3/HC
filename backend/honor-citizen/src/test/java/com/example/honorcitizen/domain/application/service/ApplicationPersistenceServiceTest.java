package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationPersistenceServiceTest {

    @Autowired
    private ApplicationPersistenceService persistenceService;
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
    private ObjectMapper objectMapper;

    private CardType cardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-persist", null, BigDecimal.valueOf(30000)));
    }

    @Test
    void saveIndividualPersistsApplicationApplicantAndMemberWithoutReceiver() throws Exception {
        ApplicationCreateRequest request = individualRequest(null);

        Application application = persistenceService.saveIndividual(
                1L, "APP-2026-900001", cardType.getId(), IssueType.MOBILE, true,
                null, null, request, "member@example.com", "photos/member.jpg");

        assertThat(application.getId()).isNotNull();

        Applicant applicant = applicantRepository.findByApplicationId(application.getId()).orElseThrow();
        assertThat(applicant.getEmail()).isEqualTo("member@example.com");

        ApplicationMember member = applicationMemberRepository.findByApplicationId(application.getId()).get(0);
        assertThat(member.getPhotoPath()).isEqualTo("photos/member.jpg");

        assertThat(receiverRepository.findByApplicationId(application.getId())).isEmpty();
    }

    @Test
    void saveIndividualPersistsReceiverWhenMobileAndPhysical() throws Exception {
        ApplicationCreateRequest request = individualRequest(true);

        Application application = persistenceService.saveIndividual(
                1L, "APP-2026-900002", cardType.getId(), IssueType.MOBILE_AND_PHYSICAL, true,
                null, null, request, "member@example.com", "photos/member.jpg");

        assertThat(receiverRepository.findByApplicationId(application.getId())).isPresent();
    }

    @Test
    void saveGroupPersistsApplicationApplicantAndAllMembers() throws Exception {
        BulkApplicationCreateRequest request = groupRequest();
        List<GroupMemberUpload> uploads = List.of(
                new GroupMemberUpload(memberRow("1"), "photos/1.jpg"),
                new GroupMemberUpload(memberRow("2"), "photos/2.jpg"));

        Application application = persistenceService.saveGroup(
                1L, "APP-2026-900003", cardType.getId(), IssueType.MOBILE, true, uploads.size(),
                null, null, 99L, request, "rep@example.com", uploads);

        assertThat(application.getId()).isNotNull();
        Applicant applicant = applicantRepository.findByApplicationId(application.getId()).orElseThrow();
        assertThat(applicant.getEmail()).isEqualTo("rep@example.com");

        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(application.getId());
        assertThat(members).hasSize(2);
    }

    private BulkMemberRow memberRow(String id) {
        return new BulkMemberRow(id, "John Doe", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "010-1111-2222", "Seoul", null, null,
                new byte[0], id + ".jpg");
    }

    private ApplicationCreateRequest individualRequest(Boolean sameAsApplicant) throws Exception {
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
                  }
                }
                """.formatted(
                cardType.getId(), sameAsApplicant == null ? "MOBILE" : "MOBILE_AND_PHYSICAL",
                sameAsApplicant == null ? "" : """
                        "receiver": {
                          "sameAsApplicant": %s,
                          "name": "김수령",
                          "phone": "010-9999-8888",
                          "zipCode": "06236",
                          "address": "서울특별시 강남구",
                          "detailAddress": "101동"
                        },
                        """.formatted(sameAsApplicant));
        return objectMapper.readValue(json, ApplicationCreateRequest.class);
    }

    private BulkApplicationCreateRequest groupRequest() throws Exception {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "organizationName": "OO기업", "department": "인사팀", "name": "홍길동", "phone": "010-1234-5678" }
                }
                """.formatted(cardType.getId());
        return objectMapper.readValue(json, BulkApplicationCreateRequest.class);
    }
}
