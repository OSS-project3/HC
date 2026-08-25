package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.enums.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMemberTest {

    @Test
    void individualMemberLeavesEmailAndPhoneNull() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThat(member.getEmail()).isNull();
        assertThat(member.getPhone()).isNull();
        assertThat(member.getAddress()).isNull();
    }

    @Test
    void individualMemberAllowsOptionalFieldsToBeNull() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThat(member.getBirthTime()).isNull();
        assertThat(member.getBirthRegion()).isNull();
        assertThat(member.getEntryDate()).isNull();
        assertThat(member.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(member.getNationality()).isEqualTo("US");
    }

    @Test
    void groupMemberRowCarriesOwnEmailPhoneAndAddress() {
        ApplicationMember member = ApplicationMember.createGroupRow(
                2L, "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, LocalDate.of(2026, 8, 15),
                "john@example.com", "010-1111-2222", "Seoul", null, null, "photos/b.jpg");

        assertThat(member.getEmail()).isEqualTo("john@example.com");
        assertThat(member.getPhone()).isEqualTo("010-1111-2222");
        assertThat(member.getAddress()).isEqualTo("Seoul");
        assertThat(member.getEntryDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(member.getPhotoNumber()).isNull();
    }

    @Test
    void groupMemberRowCarriesPhotoNumberWhenGiven() {
        ApplicationMember member = ApplicationMember.createGroupRow(
                2L, "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, LocalDate.of(2026, 8, 15),
                "john@example.com", "010-1111-2222", "Seoul", null, null, "photos/b.jpg", "001");

        assertThat(member.getPhotoNumber()).isEqualTo("001");
    }

    @Test
    void individualMemberWithAddressOverloadStoresCardDisplayAddress() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg", "서울특별시 종로구 세종대로 1");

        assertThat(member.getAddress()).isEqualTo("서울특별시 종로구 세종대로 1");
        assertThat(member.getSurname()).isNull();
    }

    @Test
    void nonStudentCardLeavesStudentFieldsNull() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThat(member.getStudentId()).isNull();
        assertThat(member.getDepartment()).isNull();
    }

    @Test
    void studentCardCarriesStudentIdAndDepartment() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, "20261234", "컴퓨터공학과", "photos/a.jpg");

        assertThat(member.getStudentId()).isEqualTo("20261234");
        assertThat(member.getDepartment()).isEqualTo("컴퓨터공학과");
    }

    @Test
    void updatePhotoReplacesPhotoPath() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/old.jpg");

        member.updatePhoto("photos/new.jpg");

        assertThat(member.getPhotoPath()).isEqualTo("photos/new.jpg");
    }
}
