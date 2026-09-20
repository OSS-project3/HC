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
                1L, text('A', 'P', 'P', '-', '1'), 2L, IssueType.MOBILE, true, null, null, null, null, null, null,
                false, false);

        assertThat(application.getUserId()).isEqualTo(1L);
        assertThat(application.getApplicationNumber()).isEqualTo(text('A', 'P', 'P', '-', '1'));
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void createsIndividualApplicationWithConsentFlagsAndStampsCurrentPolicyVersion() {
        Application application = factory.createIndividualApplication(
                1L, text('A', 'P', 'P', '-', '2'), 2L, IssueType.MOBILE, true, null, null, null, null, null, null,
                true, true);

        assertThat(application.isConsultationConfirmed()).isTrue();
        assertThat(application.isDisclaimerConfirmed()).isTrue();
        assertThat(application.getConsentPolicyVersion()).isEqualTo(Application.CONSENT_POLICY_VERSION);
    }

    @Test
    void createsIndividualApplicationDefaultsConsentFlagsToFalseWhenOmittedByLegacyOverload() {
        Application application = Application.createIndividual(
                1L, text('A', 'P', 'P', '-', '3'), 2L, IssueType.MOBILE, true, null, null, null, null, null, null);

        assertThat(application.isConsultationConfirmed()).isFalse();
        assertThat(application.isDisclaimerConfirmed()).isFalse();
        // 동의 여부와 무관하게 정책 버전은 항상 현재값으로 채워진다(생성 시점에 어떤 문구가 유효했는지 기록).
        assertThat(application.getConsentPolicyVersion()).isEqualTo(Application.CONSENT_POLICY_VERSION);
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
