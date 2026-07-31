package com.example.honorcitizen.domain.application.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiverTest {

    @Test
    void copyFromApplicantReusesNamePhoneAndOrganizationFields() {
        Applicant applicant = Applicant.createGroup(
                1L, "Hong Gildong", "hong@example.com", "010-1234-5678", "OO기업", "인사팀");

        Receiver receiver = Receiver.copyFromApplicant(1L, applicant);

        assertThat(receiver.getReceiverName()).isEqualTo("Hong Gildong");
        assertThat(receiver.getReceiverPhone()).isEqualTo("010-1234-5678");
        assertThat(receiver.getOrganizationName()).isEqualTo("OO기업");
        assertThat(receiver.getDepartment()).isEqualTo("인사팀");
        assertThat(receiver.getAddress()).isNull();
    }

    @Test
    void createStoresAddressFields() {
        Receiver receiver = Receiver.create(
                1L, "김수령", "010-9999-8888", "06236", "서울특별시 강남구", "101동 202호", "부재 시 경비실", null, null);

        assertThat(receiver.getZipCode()).isEqualTo("06236");
        assertThat(receiver.getAddress()).isEqualTo("서울특별시 강남구");
        assertThat(receiver.getDetailAddress()).isEqualTo("101동 202호");
        assertThat(receiver.getDeliveryRequest()).isEqualTo("부재 시 경비실");
    }
}
