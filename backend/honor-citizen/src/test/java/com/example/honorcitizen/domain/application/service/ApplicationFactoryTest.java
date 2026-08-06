package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFactoryTest {

    private final ApplicationFactory factory = new ApplicationFactory();

    @Test
    void createsIndividualApplicationWithoutPersistenceDependency() {
        Application application = factory.createIndividualApplication(
                1L, text('A', 'P', 'P', '-', '1'), 2L, IssueType.MOBILE, true, null, null);

        assertThat(application.getUserId()).isEqualTo(1L);
        assertThat(application.getApplicationNumber()).isEqualTo(text('A', 'P', 'P', '-', '1'));
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PAYMENT_PENDING);
    }

    @Test
    void createsApplicantOnlyAfterApplicationIdIsAvailable() {
        Applicant applicant = factory.createIndividualApplicant(
                10L, text('N'), text('e'), text('p'));

        assertThat(applicant.getApplicationId()).isEqualTo(10L);
        assertThat(applicant.getName()).isEqualTo(text('N'));
    }

    private String text(char... value) {
        return new String(value);
    }
}
