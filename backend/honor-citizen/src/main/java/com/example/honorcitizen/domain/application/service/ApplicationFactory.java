package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
class ApplicationFactory {

    Application createIndividualApplication(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId) {
        return Application.createIndividual(userId, applicationNumber, cardTypeId, issueType,
                receiverSameAsApplicant, logoFileId, sealFileId);
    }

    Applicant createIndividualApplicant(Long applicationId, String name, String email, String phone) {
        return Applicant.createIndividual(applicationId, name, email, phone);
    }

    Receiver copyIndividualReceiver(Long applicationId, Applicant applicant) {
        return Receiver.copyFromApplicant(applicationId, applicant);
    }

    Receiver createIndividualReceiver(Long applicationId, String name, String phone, String zipCode,
            String address, String detailAddress, String deliveryRequest) {
        return Receiver.create(applicationId, name, phone, zipCode, address, detailAddress, deliveryRequest,
                null, null);
    }

    ApplicationMember createIndividualMember(Long applicationId, String englishName, LocalDate birthDate,
            String nationality, LocalTime birthTime, String birthRegion, Gender gender, LocalDate entryDate,
            String studentId, String department, String photoPath) {
        return ApplicationMember.createIndividual(applicationId, englishName, birthDate, nationality,
                birthTime, birthRegion, gender, entryDate, studentId, department, photoPath);
    }
}
