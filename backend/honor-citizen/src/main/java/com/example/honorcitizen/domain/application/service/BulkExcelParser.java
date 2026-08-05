package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 단체 신청 ZIP(엑셀 + photos/ 사진) 파서.
 *
 * 기대 구조:
 *  - ZIP 루트에 .xlsx 파일 1개
 *  - "photos/" 하위에 ID로 매칭되는 사진 파일들 (예: 1.jpg, 2.png — 대소문자/확장자 무시)
 *  - 엑셀 1행: A열 "공통 입국날짜" 라벨, B열 값(선택)
 *  - 엑셀 3행: 헤더, 4행부터 데이터 (ID 열이 비면 종료)
 *  - 열 순서: ID, 영문명, 생년월일, 국적, 출생시간, 출생지역, 성별, 개별입국날짜, 이메일, 전화번호, 주소, [학번, 학과]
 */
@Component
class BulkExcelParser {

    private static final int COMMON_ENTRY_DATE_ROW = 0;
    private static final int HEADER_ROW = 2;
    private static final int FIRST_DATA_ROW = 3;

    List<BulkMemberRow> parse(MultipartFile zipFile, boolean isStudent) {
        Map<String, PhotoEntry> photosById = new HashMap<>();
        byte[] excelBytes = null;

        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (name.toLowerCase().endsWith(".xlsx") && excelBytes == null) {
                    excelBytes = readAll(zipInputStream);
                } else if (name.toLowerCase().startsWith("photos/")) {
                    String fileName = name.substring("photos/".length());
                    if (fileName.isBlank()) {
                        continue;
                    }
                    String id = stripExtension(fileName);
                    photosById.put(id.toLowerCase(), new PhotoEntry(fileName, readAll(zipInputStream)));
                }
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_ZIP);
        }

        if (excelBytes == null) {
            throw new CustomException(ErrorCode.EXCEL_NOT_FOUND);
        }

        return parseExcel(excelBytes, photosById, isStudent);
    }

    private List<BulkMemberRow> parseExcel(byte[] excelBytes, Map<String, PhotoEntry> photosById, boolean isStudent) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            LocalDate commonEntryDate = readDateCell(sheet, COMMON_ENTRY_DATE_ROW, 1, formatter);

            List<BulkMemberRow> rows = new ArrayList<>();
            int rowIndex = FIRST_DATA_ROW;
            while (true) {
                Row row = sheet.getRow(rowIndex);
                String id = row == null ? null : stringValue(row, 0, formatter);
                if (id == null || id.isBlank()) {
                    break;
                }

                BulkMemberRow parsed = parseRow(row, id, commonEntryDate, photosById, isStudent, formatter);
                rows.add(parsed);
                rowIndex++;
            }

            if (rows.isEmpty()) {
                throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
            }
            return rows;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
    }

    private BulkMemberRow parseRow(Row row, String id, LocalDate commonEntryDate,
            Map<String, PhotoEntry> photosById, boolean isStudent, DataFormatter formatter) {
        String englishName = requireText(stringValue(row, 1, formatter));
        LocalDate birthDate = requireDate(readDateCell(row.getSheet(), row.getRowNum(), 2, formatter));
        String nationality = requireText(stringValue(row, 3, formatter));
        LocalTime birthTime = readTimeCell(row, 4, formatter);
        String birthRegion = stringValue(row, 5, formatter);
        Gender gender = requireGender(stringValue(row, 6, formatter));
        LocalDate rowEntryDate = readDateCell(row.getSheet(), row.getRowNum(), 7, formatter);
        LocalDate entryDate = rowEntryDate != null ? rowEntryDate : commonEntryDate;
        String email = requireText(stringValue(row, 8, formatter));
        String phone = requireText(stringValue(row, 9, formatter));
        String address = stringValue(row, 10, formatter);

        String studentId = isStudent ? requireText(stringValue(row, 11, formatter)) : null;
        String department = isStudent ? requireText(stringValue(row, 12, formatter)) : null;

        PhotoEntry photo = photosById.get(id.toLowerCase());
        if (photo == null) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }

        return new BulkMemberRow(id, englishName, birthDate, nationality, birthTime, birthRegion, gender,
                entryDate, email, phone, address, studentId, department, photo.bytes(), photo.fileName());
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
        return value;
    }

    private LocalDate requireDate(LocalDate value) {
        if (value == null) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
        return value;
    }

    private Gender requireGender(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
    }

    private String stringValue(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }

    private LocalDate readDateCell(Sheet sheet, int rowIndex, int columnIndex, DataFormatter formatter) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
    }

    private LocalTime readTimeCell(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalTime();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(text);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        inputStream.transferTo(buffer);
        return buffer.toByteArray();
    }

    private record PhotoEntry(String fileName, byte[] bytes) {
    }
}
