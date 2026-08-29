package com.example.honorcitizen.domain.application.entity;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void assignKoreanNameWithSurnameStoresAllNamingFields() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        member.assignKoreanName("홍", "길동", "吉童", "길할 길, 아이 동", "복을 비는 이름");

        assertThat(member.getSurname()).isEqualTo("홍");
        assertThat(member.getName()).isEqualTo("길동");
        assertThat(member.getChineseName()).isEqualTo("吉童");
        assertThat(member.getNameMeaning()).isEqualTo("길할 길, 아이 동");
        assertThat(member.getNameInterpretation()).isEqualTo("복을 비는 이름");
    }

    @Test
    void assignKoreanNameWithSurnameAllowsNullSurnameDuringNameEditing() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        member.assignKoreanName(null, "길동", null, "뜻", null);

        assertThat(member.getSurname()).isNull();
        assertThat(member.getName()).isEqualTo("길동");
    }

    @Test
    void assignKoreanNameRejectsNameOutsideTwoToThreeKoreanCharacters() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThatThrownBy(() -> member.assignKoreanName(null, "가", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> member.assignKoreanName(null, "가나다라", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> member.assignKoreanName(null, "abc", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void assignKoreanNameRejectsSurnameOutsideOneToTwoKoreanCharacters() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThatThrownBy(() -> member.assignKoreanName("황보김", "길동", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void assignKoreanNameRejectsHanjaLengthMismatch() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThatThrownBy(() -> member.assignKoreanName(null, "길동", "吉", "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void twoArgAssignKoreanNameAlsoValidatesFormat() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThatThrownBy(() -> member.assignKoreanName("가", "吉"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        member.assignKoreanName("길동", "吉童");
        assertThat(member.getName()).isEqualTo("길동");
        assertThat(member.getChineseName()).isEqualTo("吉童");
    }

    @Test
    void assignCardNumberStoresValidFormat() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        member.assignCardNumber("ROK-12345-6789");

        assertThat(member.getCardNumber()).isEqualTo("ROK-12345-6789");
        assertThat(member.isCardGenerated()).isFalse();
    }

    @Test
    void assignCardNumberRejectsInvalidFormat() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");

        assertThatThrownBy(() -> member.assignCardNumber("12345-6789"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> member.assignCardNumber("ROK-1234-6789"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void isValidCardNumberFormatMatchesRokPattern() {
        assertThat(ApplicationMember.isValidCardNumberFormat("ROK-12345-6789")).isTrue();
        assertThat(ApplicationMember.isValidCardNumberFormat("rok-12345-6789")).isFalse();
        assertThat(ApplicationMember.isValidCardNumberFormat(null)).isFalse();
    }

    @Test
    void updatePhotoReplacesPhotoPath() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/old.jpg");

        member.updatePhoto("photos/new.jpg");

        assertThat(member.getPhotoPath()).isEqualTo("photos/new.jpg");
    }

    // 3-B(2026-08-30): 카드 생성/재생성 확정 — assignCardImages는 검증 없이 그대로 덮어쓴다
    // (호출 전 Service가 이미 필요한 검증을 끝냈다는 전제).
    @Test
    void assignCardImagesStoresFrontBackPathsAndIssueDate() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");
        assertThat(member.isCardGenerated()).isFalse();

        member.assignCardImages("cards/front-1.png", "cards/back-1.png", LocalDate.of(2026, 9, 1));

        assertThat(member.getCardFrontPath()).isEqualTo("cards/front-1.png");
        assertThat(member.getCardBackPath()).isEqualTo("cards/back-1.png");
        assertThat(member.getIssueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(member.isCardGenerated()).isTrue();
    }

    @Test
    void assignCardImagesOverwritesOnRegenerate() {
        ApplicationMember member = ApplicationMember.createIndividual(
                1L, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");
        member.assignCardImages("cards/front-1.png", "cards/back-1.png", LocalDate.of(2026, 9, 1));

        member.assignCardImages("cards/front-2.png", "cards/back-2.png", LocalDate.of(2026, 9, 1));

        assertThat(member.getCardFrontPath()).isEqualTo("cards/front-2.png");
        assertThat(member.getCardBackPath()).isEqualTo("cards/back-2.png");
    }
}
