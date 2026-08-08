package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BulkExcelParserTest {

    private final BulkExcelParser parser = new BulkExcelParser();

    // 컬럼 순서: ID|영문명|생년월일|국적|출생시간|출생지역|성별|개별입국날짜|이메일|전화번호|주소
    private static final String ROW_1 = "1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul";
    private static final String ROW_2 = "2|Mike Kim|1992-03-03|US|||MALE||mike@example.com|010-3333-4444|Busan";

    private byte[] buildExcel(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
            Row commonRow = sheet.createRow(0);
            commonRow.createCell(0).setCellValue("공통 입국날짜");
            commonRow.createCell(1).setCellValue("2026-08-15");
            sheet.createRow(2).createCell(0).setCellValue("ID");

            int rowIndex = 3;
            for (String rowCsv : rows) {
                if (rowCsv == null) {
                    rowIndex++;
                    continue;
                }
                String[] cols = rowCsv.split("\\|", -1);
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < cols.length; i++) {
                    if (!cols[i].isEmpty()) {
                        row.createCell(i).setCellValue(cols[i]);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildExcelWithNumericId(double numericId) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
            Row commonRow = sheet.createRow(0);
            commonRow.createCell(0).setCellValue("공통 입국날짜");
            commonRow.createCell(1).setCellValue("2026-08-15");
            sheet.createRow(2).createCell(0).setCellValue("ID");

            Row row = sheet.createRow(3);
            row.createCell(0).setCellValue(numericId);
            row.createCell(1).setCellValue("John Doe");
            row.createCell(2).setCellValue("1988-01-01");
            row.createCell(3).setCellValue("US");
            row.createCell(6).setCellValue("MALE");
            row.createCell(8).setCellValue("john@example.com");
            row.createCell(9).setCellValue("010-1111-2222");
            row.createCell(10).setCellValue("Seoul");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile zipOf(byte[] excelBytes, String excelEntryName, String... photoEntries) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            if (excelBytes != null) {
                zip.putNextEntry(new ZipEntry(excelEntryName));
                zip.write(excelBytes);
                zip.closeEntry();
            }
            for (String photoEntry : photoEntries) {
                zip.putNextEntry(new ZipEntry(photoEntry));
                zip.write(("photo-" + photoEntry).getBytes());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("submitFile", "bulk.zip", "application/zip", out.toByteArray());
    }

    @Test
    void parseMatchesPhotoAtZipRoot() throws Exception {
        byte[] excel = buildExcel(ROW_1);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg");

        List<BulkMemberRow> rows = parser.parse(zip, false);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).photoFilename()).isEqualTo("1.jpg");
    }

    @Test
    void parseMatchesTextIdWithLeadingZerosToSamePhotoName() throws Exception {
        byte[] excel = buildExcel("001|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul");
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "001.jpg");

        List<BulkMemberRow> rows = parser.parse(zip, false);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).id()).isEqualTo("001");
        assertThat(rows.get(0).photoFilename()).isEqualTo("001.jpg");
    }

    @Test
    void parseDoesNotAutoMatchNumericIdToPhotoNameWithLeadingZeros() throws Exception {
        byte[] excel = buildExcelWithNumericId(1);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "001.jpg");

        assertThatThrownBy(() -> parser.parse(zip, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED)
                .satisfies(e -> assertThat(((BulkValidationException) e).getErrors())
                        .extracting("field").contains("photo"));
    }

    @Test
    void parseMatchesNumericIdToNonPaddedPhotoName() throws Exception {
        byte[] excel = buildExcelWithNumericId(1);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg");

        List<BulkMemberRow> rows = parser.parse(zip, false);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).id()).isEqualTo("1");
        assertThat(rows.get(0).photoFilename()).isEqualTo("1.jpg");
    }

    @Test
    void parseIgnoresPhotoInsideSubfolderAndTreatsItAsMissing() throws Exception {
        byte[] excel = buildExcel(ROW_1);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "photos/1.jpg");

        assertThatThrownBy(() -> parser.parse(zip, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED)
                .satisfies(e -> assertThat(((BulkValidationException) e).getErrors())
                        .extracting("field").containsExactly("photo"));
    }

    @Test
    void parseRejectsWhenMultipleExcelFilesAtRoot() throws Exception {
        byte[] excel = buildExcel(ROW_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("members.xlsx"));
            zip.write(excel);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("extra.xlsx"));
            zip.write(excel);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("1.jpg"));
            zip.write("photo-1".getBytes());
            zip.closeEntry();
        }
        MockMultipartFile zipFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", out.toByteArray());

        assertThatThrownBy(() -> parser.parse(zipFile, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED);
    }

    @Test
    void parseIgnoresExcelFileInsideSubfolder() throws Exception {
        byte[] excel = buildExcel(ROW_1);
        MockMultipartFile zip = zipOf(excel, "nested/members.xlsx", "1.jpg");

        assertThatThrownBy(() -> parser.parse(zip, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED);
    }

    @Test
    void parseCollectsErrorsFromMultipleRowsInsteadOfFailingOnFirst() throws Exception {
        String missingNameRow = "1||1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul";
        String badGenderRow = "2|Mike Kim|1992-03-03|US|||UNKNOWN||mike@example.com|010-3333-4444|Busan";
        byte[] excel = buildExcel(missingNameRow, badGenderRow);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg", "2.jpg");

        assertThatThrownBy(() -> parser.parse(zip, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED)
                .satisfies(e -> {
                    List<?> errors = ((BulkValidationException) e).getErrors();
                    assertThat(errors).hasSizeGreaterThanOrEqualTo(2);
                    assertThat(errors).extracting("row").contains(4, 5);
                });
    }

    @Test
    void parseRejectsStudentIdLongerThanTenDigits() throws Exception {
        String studentRow = "1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul|202612345678|컴퓨터공학과";
        byte[] excel = buildExcel(studentRow);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg");

        assertThatThrownBy(() -> parser.parse(zip, true))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED)
                .satisfies(e -> assertThat(((BulkValidationException) e).getErrors())
                        .extracting("field").contains("studentId"));
    }

    @Test
    void parseIgnoresMacosxAndDsStoreEntries() throws Exception {
        byte[] excel = buildExcel(ROW_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("members.xlsx"));
            zip.write(excel);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("1.jpg"));
            zip.write("photo-1".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(".DS_Store"));
            zip.write("ds-store-junk".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("__MACOSX/._1.jpg"));
            zip.write("macosx-junk".getBytes());
            zip.closeEntry();
        }
        MockMultipartFile zipFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", out.toByteArray());

        List<BulkMemberRow> rows = parser.parse(zipFile, false);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).photoBytes()).isEqualTo("photo-1".getBytes());
    }

    @Test
    void parseSkipsMiddleAndTrailingBlankRows() throws Exception {
        byte[] excel = buildExcel(ROW_1, null, ROW_2, null);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg", "2.jpg");

        List<BulkMemberRow> rows = parser.parse(zip, false);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).englishName()).isEqualTo("John Doe");
        assertThat(rows.get(1).englishName()).isEqualTo("Mike Kim");
    }

    @Test
    void parseRejectsNationalityThatIsNotAnIsoAlpha2Code() throws Exception {
        String badNationalityRow = "1|John Doe|1988-01-01|USA|||MALE||john@example.com|010-1111-2222|Seoul";
        byte[] excel = buildExcel(badNationalityRow);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg");

        assertThatThrownBy(() -> parser.parse(zip, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED)
                .satisfies(e -> assertThat(((BulkValidationException) e).getErrors())
                        .extracting("field").contains("nationality"));
    }

    @Test
    void parseRejectsFutureBirthDate() throws Exception {
        String futureBirthDateRow = "1|John Doe|2999-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul";
        byte[] excel = buildExcel(futureBirthDateRow);
        MockMultipartFile zip = zipOf(excel, "members.xlsx", "1.jpg");

        assertThatThrownBy(() -> parser.parse(zip, false))
                .isInstanceOf(BulkValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_APPLICATION_VALIDATION_FAILED)
                .satisfies(e -> assertThat(((BulkValidationException) e).getErrors())
                        .extracting("field").contains("birthDate"));
    }
}
