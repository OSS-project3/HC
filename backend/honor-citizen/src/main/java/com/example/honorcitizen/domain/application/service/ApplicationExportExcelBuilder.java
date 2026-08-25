package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 관리자 신청 엑셀 내보내기(`POST /api/admin/applications/export`, DESIGN.md §2.4)의
 * 실제 xlsx 작성을 담당한다. {@link NamingResultExcelParser}/{@link BulkExcelParser}가 읽기 전용인
 * 것과 대칭으로, 이 클래스는 쓰기만 다룬다.
 *
 * INDIVIDUAL과 GROUP은 근본적으로 다른 문제다:
 * - INDIVIDUAL은 원본 파일이 없으므로(신청 데이터가 DB에만 있음) 새 워크북을 만든다.
 * - GROUP은 이미 신청 시 업로드된 원본 엑셀(서식·병합셀·데이터검증 포함)이 있으므로 그걸 그대로 열어
 *   이름·한자 컬럼만 append한다. 여러 GROUP 신청의 원본 서식이 서로 다를 수 있어(카드종류별 컬럼 수 상이)
 *   워크북 여러 개를 하나로 병합하는 건 POI로 안전하게 할 수 없다 — 그래서 GROUP은 호출부(Service)에서
 *   한 건씩만 이 클래스에 넘기도록 강제한다(이 클래스 자체는 그 제약을 모른다).
 */
@Component
class ApplicationExportExcelBuilder {

    private static final int COMMON_ENTRY_DATE_ROW = 0;
    private static final int HEADER_ROW = 2;
    private static final int FIRST_DATA_ROW = 3;

    private static final String[] INDIVIDUAL_HEADERS = {
            "사진 번호", "영문명", "생년월일", "출생국가", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소", "학번", "학과", "이름", "한자",
    };

    /** INDIVIDUAL 신청 여러 건을 새 워크북 한 장에 담는다. 행 순서는 입력 리스트 순서 그대로. */
    byte[] buildIndividualWorkbook(List<IndividualExportRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(COMMON_ENTRY_DATE_ROW).createCell(0).setCellValue("개인 신청 엑셀 내보내기");
            sheet.createRow(1);

            Row header = sheet.createRow(HEADER_ROW);
            for (int i = 0; i < INDIVIDUAL_HEADERS.length; i++) {
                header.createCell(i).setCellValue(INDIVIDUAL_HEADERS[i]);
            }

            int rowIndex = FIRST_DATA_ROW;
            for (IndividualExportRow row : rows) {
                ApplicationMember member = row.member();
                Row excelRow = sheet.createRow(rowIndex++);
                setCell(excelRow, 0, String.valueOf(rowIndex - FIRST_DATA_ROW));
                setCell(excelRow, 1, member.getEnglishName());
                setCell(excelRow, 2, member.getBirthDate() == null ? null : member.getBirthDate().toString());
                setCell(excelRow, 3, member.getNationality());
                setCell(excelRow, 4, member.getBirthTime() == null ? null : member.getBirthTime().toString());
                setCell(excelRow, 5, member.getBirthRegion());
                setCell(excelRow, 6, member.getGender() == null ? null : member.getGender().name());
                setCell(excelRow, 7, member.getEntryDate() == null ? null : member.getEntryDate().toString());
                setCell(excelRow, 8, row.email());
                setCell(excelRow, 9, row.phone());
                setCell(excelRow, 10, member.getAddress());
                setCell(excelRow, 11, member.getStudentId());
                setCell(excelRow, 12, member.getDepartment());
                setCell(excelRow, 13, member.getName());
                setCell(excelRow, 14, member.getChineseName());
            }

            return toBytes(workbook);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * GROUP 신청 원본 제출 ZIP(bytes)에서 엑셀을 찾아 이름·한자 컬럼을 append한다.
     * 이메일·전화번호로 원본 행과 {@code members}를 매칭한다(위치가 아니라 값 기반 —
     * {@link NamingResultExcelParser}와 동일한 매칭 전략). 미확정(이름 없음) 멤버는 공란으로 둔다.
     */
    byte[] appendNamesToGroupWorkbook(byte[] originalZipBytes, List<ApplicationMember> members) {
        byte[] xlsxBytes = extractXlsxFromZip(originalZipBytes);

        try (InputStream is = new ByteArrayInputStream(xlsxBytes); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(HEADER_ROW);
            if (header == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            DataFormatter formatter = new DataFormatter();
            int emailCol = -1;
            int phoneCol = -1;
            int lastCol = -1;
            for (Cell cell : header) {
                String text = formatter.formatCellValue(cell).trim();
                if ("이메일".equals(text)) {
                    emailCol = cell.getColumnIndex();
                } else if ("전화번호".equals(text)) {
                    phoneCol = cell.getColumnIndex();
                }
                lastCol = Math.max(lastCol, cell.getColumnIndex());
            }
            if (emailCol < 0 || phoneCol < 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            int nameCol = lastCol + 1;
            int hanjaCol = lastCol + 2;
            header.createCell(nameCol).setCellValue("이름");
            header.createCell(hanjaCol).setCellValue("한자");

            Map<String, ApplicationMember> byEmailPhone = members.stream()
                    .filter(m -> m.getEmail() != null && m.getPhone() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            m -> m.getEmail() + "|" + m.getPhone(), m -> m, (a, b) -> a));

            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String email = stringValue(row, emailCol, formatter);
                String phone = stringValue(row, phoneCol, formatter);
                if (email == null || phone == null) {
                    continue;
                }
                ApplicationMember member = byEmailPhone.get(email + "|" + phone);
                if (member == null) {
                    continue;
                }
                setCell(row, nameCol, member.getName());
                setCell(row, hanjaCol, member.getChineseName());
            }

            return toBytes(workbook);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private byte[] extractXlsxFromZip(byte[] zipBytes) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            byte[] found = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || name.contains("/") || name.contains("\\")) {
                    continue;
                }
                if (name.toLowerCase().endsWith(".xlsx")) {
                    if (found != null) {
                        throw new CustomException(ErrorCode.INVALID_INPUT);
                    }
                    found = zipInputStream.readAllBytes();
                }
            }
            if (found == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            return found;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void setCell(Row row, int columnIndex, String value) {
        if (value == null) {
            return;
        }
        row.createCell(columnIndex).setCellValue(value);
    }

    private String stringValue(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    record IndividualExportRow(ApplicationMember member, String email, String phone) {
    }
}
