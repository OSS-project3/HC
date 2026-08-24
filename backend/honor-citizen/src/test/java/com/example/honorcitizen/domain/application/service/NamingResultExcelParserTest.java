package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NamingResultExcelParserTest {

    private final NamingResultExcelParser parser = new NamingResultExcelParser();

    private static final String[] HEADERS = {
            "사진 번호", "영문명", "생년월일", "출생국가", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소", "사주이름",
    };

    // 컬럼 순서: 사진번호|영문명|생년월일|출생국가|출생시간|출생지역|성별|개별입국날짜|이메일|전화번호|주소|사주이름
    private byte[] buildExcel(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(0).createCell(0).setCellValue("공통 입국날짜");
            sheet.createRow(1).createCell(0).setCellValue("1.1");
            Row header = sheet.createRow(2);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
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
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile toMultipart(byte[] bytes) {
        return new MockMultipartFile("file", "result.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }

    @Test
    void parsesHangulOnlyAndHangulWithHanjaNames() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호(智毫)",
                "2|Mike Kim|1992-03-03|US||Chicago|MALE||mike@example.com|010-3333-4444||수민");

        List<NamingResultExcelParser.NamingResultRow> rows = parser.parse(toMultipart(excel));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).email()).isEqualTo("john@example.com");
        assertThat(rows.get(0).phone()).isEqualTo("010-1111-2222");
        assertThat(rows.get(0).name()).isEqualTo("지호");
        assertThat(rows.get(0).chineseName()).isEqualTo("智毫");
        assertThat(rows.get(1).name()).isEqualTo("수민");
        assertThat(rows.get(1).chineseName()).isNull();
    }

    @Test
    void skipsRowsWithBlankSajuNameWithoutError() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호",
                "2|Mike Kim|1992-03-03|US||Chicago|MALE||mike@example.com|010-3333-4444||");

        List<NamingResultExcelParser.NamingResultRow> rows = parser.parse(toMultipart(excel));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    void rejectsWholeFileWhenAnyRowMissingEmailOrPhone() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호",
                "2|Mike Kim|1992-03-03|US||Chicago|MALE|||010-3333-4444||수민");

        assertThatThrownBy(() -> parser.parse(toMultipart(excel)))
                .isInstanceOf(BulkValidationException.class)
                .satisfies(e -> assertThat(((BulkValidationException) e).getErrors())
                        .anySatisfy(detail -> assertThat(detail.row()).isEqualTo(5)));
    }

    @Test
    void rejectsMalformedSajuNameWithUnbalancedParenthesis() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호(智毫");

        assertThatThrownBy(() -> parser.parse(toMultipart(excel)))
                .isInstanceOf(BulkValidationException.class);
    }

    @Test
    void throwsWhenNoRowsHaveSajuNameAtAll() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||");

        assertThatThrownBy(() -> parser.parse(toMultipart(excel)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void throwsWhenRequiredHeadersAreMissing() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(0);
            sheet.createRow(1);
            Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("영문명"); // 이메일/전화번호/사주이름 헤더 없음
            Row data = sheet.createRow(3);
            data.createCell(0).setCellValue("John Doe");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            assertThatThrownBy(() -> parser.parse(toMultipart(out.toByteArray())))
                    .isInstanceOf(CustomException.class);
        }
    }
}
