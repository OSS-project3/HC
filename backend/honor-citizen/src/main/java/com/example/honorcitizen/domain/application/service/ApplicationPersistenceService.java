package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// ApplicationService의 self-invocation은 Spring AOP 프록시를 우회해 @Transactional이 안 걸리므로 별도 Bean으로 분리
@Service
@RequiredArgsConstructor
class ApplicationPersistenceService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final ReceiverRepository receiverRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final ApplicationFactory applicationFactory;

    @Transactional
    Application saveIndividual(Long userId, String applicationNumber, Long cardTypeId, IssueType issueType,
            boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId,
            ApplicationCreateRequest request, String applicantEmail, String photoPath) {
        Application application = applicationFactory.createIndividualApplication(
                userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant, logoFileId, sealFileId);
        applicationRepository.save(application);

        Applicant applicant = applicationFactory.createIndividualApplicant(
                application.getId(), request.getApplicant().getName(), applicantEmail,
                request.getApplicant().getPhone());
        applicantRepository.save(applicant);

        saveReceiverIfNeeded(application, request, applicant);

        ApplicationCreateRequest.MemberRequest memberRequest = request.getMember();
        ApplicationMember member = applicationFactory.createIndividualMember(
                application.getId(), memberRequest.getEnglishName(), memberRequest.getBirthDate(),
                memberRequest.getNationality(), memberRequest.getBirthTime(), memberRequest.getBirthRegion(),
                memberRequest.getGender(), memberRequest.getEntryDate(),
                memberRequest.getStudentId(), memberRequest.getDepartment(), photoPath);
        applicationMemberRepository.save(member);

        return application;
    }

    @Transactional
    Application saveGroup(Long userId, String applicationNumber, Long cardTypeId, IssueType issueType,
            boolean receiverSameAsApplicant, int totalQuantity, Long logoFileId, Long sealFileId, Long submitFileId,
            BulkApplicationCreateRequest request, String applicantEmail, Iterable<GroupMemberUpload> memberUploads) {
        Application application = Application.createGroup(
                userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant, totalQuantity,
                logoFileId, sealFileId, submitFileId);
        applicationRepository.save(application);

        BulkApplicationCreateRequest.ApplicantRequest applicantRequest = request.getApplicant();
        Applicant applicant = Applicant.createGroup(application.getId(), applicantRequest.getName(),
                applicantEmail, applicantRequest.getPhone(),
                applicantRequest.getOrganizationName(), applicantRequest.getDepartment());
        applicantRepository.save(applicant);

        saveGroupReceiverIfNeeded(application, request, applicant);

        for (GroupMemberUpload upload : memberUploads) {
            BulkMemberRow row = upload.row();
            ApplicationMember member = ApplicationMember.createGroupRow(
                    application.getId(), row.englishName(), row.birthDate(), row.nationality(),
                    row.birthTime(), row.birthRegion(), row.gender(), row.entryDate(),
                    row.email(), row.phone(), row.address(), row.studentId(), row.department(), upload.photoPath());
            applicationMemberRepository.save(member);
        }

        return application;
    }

    private void saveReceiverIfNeeded(Application application, ApplicationCreateRequest request, Applicant applicant) {
        if (request.getIssueType() != IssueType.MOBILE_AND_PHYSICAL) {
            return;
        }
        ApplicationCreateRequest.ReceiverRequest receiverRequest = request.getReceiver();
        String name = StringUtils.hasText(receiverRequest.getName()) ? receiverRequest.getName() : applicant.getName();
        String phone = StringUtils.hasText(receiverRequest.getPhone()) ? receiverRequest.getPhone() : applicant.getPhone();
        Receiver receiver = applicationFactory.createIndividualReceiver(application.getId(), name, phone,
                receiverRequest.getZipCode(), receiverRequest.getAddress(), receiverRequest.getDetailAddress(),
                receiverRequest.getDeliveryRequest());
        receiverRepository.save(receiver);
    }

    private void saveGroupReceiverIfNeeded(Application application, BulkApplicationCreateRequest request, Applicant applicant) {
        if (request.getIssueType() != IssueType.MOBILE_AND_PHYSICAL) {
            return;
        }
        BulkApplicationCreateRequest.ReceiverRequest receiverRequest = request.getReceiver();
        String name = StringUtils.hasText(receiverRequest.getName()) ? receiverRequest.getName() : applicant.getName();
        String phone = StringUtils.hasText(receiverRequest.getPhone()) ? receiverRequest.getPhone() : applicant.getPhone();
        Receiver receiver = Receiver.create(application.getId(), name, phone,
                receiverRequest.getZipCode(), receiverRequest.getAddress(), receiverRequest.getDetailAddress(),
                receiverRequest.getDeliveryRequest(), receiverRequest.getOrganizationName(), receiverRequest.getDepartment());
        receiverRepository.save(receiver);
    }
}
