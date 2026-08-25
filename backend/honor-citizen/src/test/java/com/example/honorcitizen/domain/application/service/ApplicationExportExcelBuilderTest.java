package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationExportExcelBuilderTest {

    private final ApplicationExportExcelBuilder builder = new ApplicationExportExcelBuilder();

    private static final String[] GROUP_HEADERS = {
            "사진 번호", "영문명", "생년월일", "출생국가", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소",
    };

    @Test
    void buildsIndividualWorkbookWithOneRowPerMember() throws Exception {
        ApplicationMember memberA = ApplicationMember.createIndividual(
                1L, "John Doe", LocalDate.of(1988, 1, 1), "US", null, "Chicago",
                Gender.MALE, LocalDate.of(2026, 3, 1), null, null, null);
        memberA.assignKoreanName("지호", "智毫");
        ApplicationMember memberB = ApplicationMember.createIndividual(
                2L, "Mike Kim", LocalDate.of(1992, 3, 3), "US", null, "Chicago",
                Gender.MALE, null, null, null, null);

        byte[] bytes = builder.buildIndividualWorkbook(List.of(
                new ApplicationExportExcelBuilder.IndividualExportRow(memberA, "john@example.com", "010-1111-2222"),
                new ApplicationExportExcelBuilder.IndividualExportRow(memberB, "mike@example.com", "010-3333-4444")));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(2);
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("영문명");
            assertThat(header.getCell(13).getStringCellValue()).isEqualTo("이름");
            assertThat(header.getCell(14).getStringCellValue()).isEqualTo("한자");

            Row row1 = sheet.getRow(3);
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("John Doe");
            assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("1988-01-01");
            assertThat(row1.getCell(8).getStringCellValue()).isEqualTo("john@example.com");
            assertThat(row1.getCell(9).getStringCellValue()).isEqualTo("010-1111-2222");
            assertThat(row1.getCell(13).getStringCellValue()).isEqualTo("지호");
            assertThat(row1.getCell(14).getStringCellValue()).isEqualTo("智毫");

            Row row2 = sheet.getRow(4);
            assertThat(row2.getCell(1).getStringCellValue()).isEqualTo("Mike Kim");
            assertThat(row2.getCell(13)).isNull(); // 미확정 — 셀 자체를 안 만듦
        }
    }

    private byte[] buildGroupOriginalZip(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(0).createCell(0).setCellValue("공통 입국날짜");
            sheet.createRow(1).createCell(0).setCellValue("1.1");
            Row header = sheet.createRow(2);
            for (int i = 0; i < GROUP_HEADERS.length; i++) {
                header.createCell(i).setCellValue(GROUP_HEADERS[i]);
            }
            int rowIndex = 3;
            for (String rowCsv : rows) {
                String[] cols = rowCsv.split("\\|", -1);
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < cols.length; i++) {
                    if (!cols[i].isEmpty()) {
                        row.createCell(i).setCellValue(cols[i]);
                    }
                }
            }
            ByteArrayOutputStream xlsxOut = new ByteArrayOutputStream();
            workbook.write(xlsxOut);

            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
                zos.putNextEntry(new ZipEntry("members.xlsx"));
                zos.write(xlsxOut.toByteArray());
                zos.closeEntry();
            }
            return zipOut.toByteArray();
        }
    }

    @Test
    void appendsNameAndHanjaColumnsMatchedByEmailAndPhone() throws Exception {
        byte[] zip = buildGroupOriginalZip(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222|Seoul",
                "2|Mike Kim|1992-03-03|US||Chicago|MALE||mike@example.com|010-3333-4444|Busan");

        ApplicationMember memberA = ApplicationMember.createGroupRow(
                10L, "John Doe", LocalDate.of(1988, 1, 1), "US", null, "Chicago",
                Gender.MALE, null, "john@example.com", "010-1111-2222", "Seoul", null, null, null);
        memberA.assignKoreanName("지호", "智毫");
        ApplicationMember memberB = ApplicationMember.createGroupRow(
                10L, "Mike Kim", LocalDate.of(1992, 3, 3), "US", null, "Chicago",
                Gender.MALE, null, "mike@example.com", "010-3333-4444", "Busan", null, null, null);

        byte[] result = builder.appendNamesToGroupWorkbook(zip, List.of(memberA, memberB));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(2);
            assertThat(header.getCell(11).getStringCellValue()).isEqualTo("이름");
            assertThat(header.getCell(12).getStringCellValue()).isEqualTo("한자");
            // 원본 컬럼은 그대로 보존
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("영문명");

            Row row1 = sheet.getRow(3);
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("John Doe"); // 원본 데이터 보존
            assertThat(row1.getCell(11).getStringCellValue()).isEqualTo("지호");
            assertThat(row1.getCell(12).getStringCellValue()).isEqualTo("智毫");

            Row row2 = sheet.getRow(4);
            assertThat(row2.getCell(11)).isNull(); // 미확정 — 공란
        }
    }

    @Test
    void throwsWhenZipHasNoExcel() throws Exception {
        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("no excel here".getBytes());
            zos.closeEntry();
        }

        assertThatThrownBy(() -> builder.appendNamesToGroupWorkbook(zipOut.toByteArray(), List.of()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void throwsWhenZipHasMultipleExcelFiles() throws Exception {
        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
            zos.putNextEntry(new ZipEntry("a.xlsx"));
            zos.write(buildGroupOriginalZip());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("b.xlsx"));
            zos.write(buildGroupOriginalZip());
            zos.closeEntry();
        }

        assertThatThrownBy(() -> builder.appendNamesToGroupWorkbook(zipOut.toByteArray(), List.of()))
                .isInstanceOf(CustomException.class);
    }
}
