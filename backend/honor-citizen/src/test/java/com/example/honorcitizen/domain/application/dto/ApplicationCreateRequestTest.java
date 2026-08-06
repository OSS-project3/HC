package com.example.honorcitizen.domain.application.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCreateRequestTest {

    @Test
    void receiverIsSameAsApplicantWhenReceiverIsOmitted() {
        ApplicationCreateRequest request = new ApplicationCreateRequest();

        assertThat(request.isReceiverSameAsApplicant()).isTrue();
    }

    @Test
    void receiverSameAsApplicantUsesSubmittedFlag() throws Exception {
        ApplicationCreateRequest same = requestWithReceiverFlag(true);
        ApplicationCreateRequest different = requestWithReceiverFlag(false);

        assertThat(same.isReceiverSameAsApplicant()).isTrue();
        assertThat(different.isReceiverSameAsApplicant()).isFalse();
    }

    private ApplicationCreateRequest requestWithReceiverFlag(boolean sameAsApplicant) throws Exception {
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        ApplicationCreateRequest.ReceiverRequest receiver = new ApplicationCreateRequest.ReceiverRequest();
        setField(receiver, chars('s', 'a', 'm', 'e', 'A', 's', 'A', 'p', 'p', 'l', 'i', 'c', 'a', 'n', 't'),
                sameAsApplicant);
        setField(request, chars('r', 'e', 'c', 'e', 'i', 'v', 'e', 'r'), receiver);
        return request;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private String chars(char... value) {
        return new String(value);
    }
}
