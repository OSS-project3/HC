package com.example.honorcitizen.domain.application.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicantTest {

    @Test
    void individualApplicantHasNoOrganizationFields() {
        Applicant applicant = Applicant.createIndividual(1L, "Hong Gildong", "hong@example.com", "010-1234-5678");

        assertThat(applicant.getOrganizationName()).isNull();
        assertThat(applicant.getDepartment()).isNull();
    }

    @Test
    void groupApplicantCarriesOrganizationAndDepartment() {
        Applicant applicant = Applicant.createGroup(
                1L, "Hong Gildong", "hong@example.com", "010-1234-5678", "OO기업", "인사팀");

        assertThat(applicant.getOrganizationName()).isEqualTo("OO기업");
        assertThat(applicant.getDepartment()).isEqualTo("인사팀");
    }
}
