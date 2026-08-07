package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.exception.ValidationErrorDetail;
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
 * 단체 신청 ZIP(엑셀 + 사진) 파서.
 *
 * 기대 구조:
 *  - ZIP 루트에 .xlsx 파일 정확히 1개(2개 이상이면 전체 실패), 하위 폴더의 엑셀은 무시
 *  - ZIP 루트에 ID로 매칭되는 사진 파일들(예: 1.jpg, 2.png — 대소문자/확장자 무시), 하위 폴더 사진은 무시
 *  - __MACOSX, .DS_Store는 무시
 *  - 엑셀 1행: A열 "공통 입국날짜" 라벨, B열 값(선택)
 *  - 엑셀 3행: 헤더, 4행부터 데이터 (ID 열이 빈 행은 중간/마지막 상관없이 무시)
 *  - 열 순서: ID, 영문명, 생년월일, 국적, 출생시간, 출생지역, 성별, 개별입국날짜, 이메일, 전화번호, 주소, [학번, 학과]
 *  - 오류는 하나라도 있으면 부분 성공 없이 전체 실패, 모든 오류를 errors[]로 함께 반환한다.
 */
@Component
class BulkExcelParser {

    private static final int COMMON_ENTRY_DATE_ROW = 0;
    private static final int HEADER_ROW = 2;
    private static final int FIRST_DATA_ROW = 3;

    List<BulkMemberRow> parse(MultipartFile zipFile, boolean isStudent) {
        Map<String, PhotoEntry> photosById = new HashMap<>();
        List<byte[]> excelCandidates = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !isRootEntry(name) || isIgnoredEntry(name)) {
                    continue;
                }
                if (name.toLowerCase().endsWith(".xlsx")) {
                    excelCandidates.add(readAll(zipInputStream));
                } else {
                    String id = stripExtension(name);
                    photosById.put(id.toLowerCase(), new PhotoEntry(name, readAll(zipInputStream)));
                }
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_ZIP);
        }

        if (excelCandidates.isEmpty()) {
            throw singleError(null, "submitFile", "EXCEL_NOT_FOUND", "ZIP 루트에 엑셀 파일이 없습니다.");
        }
        if (excelCandidates.size() > 1) {
            throw singleError(null, "submitFile", "EXCEL_DUPLICATE", "ZIP 루트에 엑셀 파일이 2개 이상입니다.");
        }

        return parseExcel(excelCandidates.get(0), photosById, isStudent);
    }

    private boolean isRootEntry(String name) {
        return !name.contains("/");
    }

    private boolean isIgnoredEntry(String name) {
        return name.equals(".DS_Store");
    }

    private List<BulkMemberRow> parseExcel(byte[] excelBytes, Map<String, PhotoEntry> photosById, boolean isStudent) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            LocalDate commonEntryDate = readCommonEntryDateCell(sheet, formatter);

            List<BulkMemberRow> rows = new ArrayList<>();
            List<ValidationErrorDetail> errors = new ArrayList<>();
            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = FIRST_DATA_ROW; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String id = row == null ? null : stringValue(row, 0, formatter);
                if (id == null || id.isBlank()) {
                    continue;
                }

                BulkMemberRow parsed = parseRow(row, id, commonEntryDate, photosById, isStudent, formatter, errors);
                if (parsed != null) {
                    rows.add(parsed);
                }
            }

            if (rows.isEmpty() && errors.isEmpty()) {
                errors.add(new ValidationErrorDetail(null, "submitFile", "EMPTY_EXCEL", "엑셀에 유효한 데이터 행이 없습니다."));
            }
            if (!errors.isEmpty()) {
                throw new BulkValidationException(errors);
            }
            return rows;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw singleError(null, "submitFile", "EXCEL_UNREADABLE", "엑셀 파일을 읽을 수 없습니다.");
        }
    }

    private BulkMemberRow parseRow(Row row, String id, LocalDate commonEntryDate, Map<String, PhotoEntry> photosById,
            boolean isStudent, DataFormatter formatter, List<ValidationErrorDetail> errors) {
        int rowNumber = row.getRowNum() + 1;
        String englishName = requireText(stringValue(row, 1, formatter), rowNumber, "englishName", errors);
        LocalDate birthDate = readDateCellForRow(row.getSheet(), row.getRowNum(), 2, formatter, rowNumber, "birthDate", true, errors);
        String nationality = requireText(stringValue(row, 3, formatter), rowNumber, "nationality", errors);
        LocalTime birthTime = readTimeCell(row, 4, formatter, rowNumber, errors);
        String birthRegion = stringValue(row, 5, formatter);
        Gender gender = requireGender(stringValue(row, 6, formatter), rowNumber, errors);
        LocalDate rowEntryDate = readDateCellForRow(row.getSheet(), row.getRowNum(), 7, formatter, rowNumber, "entryDate", false, errors);
        LocalDate entryDate = rowEntryDate != null ? rowEntryDate : commonEntryDate;
        String email = requireText(stringValue(row, 8, formatter), rowNumber, "email", errors);
        String phone = requireText(stringValue(row, 9, formatter), rowNumber, "phone", errors);
        String address = stringValue(row, 10, formatter);

        String studentId = null;
        String department = null;
        if (isStudent) {
            studentId = requireText(stringValue(row, 11, formatter), rowNumber, "studentId", errors);
            if (studentId != null && !isValidStudentId(studentId)) {
                errors.add(new ValidationErrorDetail(rowNumber, "studentId", "INVALID_FORMAT", "학번은 최대 10자·숫자만 허용합니다."));
                studentId = null;
            }
            department = requireText(stringValue(row, 12, formatter), rowNumber, "department", errors);
        }

        PhotoEntry photo = photosById.get(id.toLowerCase());
        if (photo == null) {
            errors.add(new ValidationErrorDetail(rowNumber, "photo", "PHOTO_NOT_FOUND", "ID에 매칭되는 사진을 찾을 수 없습니다."));
        }

        boolean hasRowError = englishName == null || birthDate == null || nationality == null || gender == null
                || email == null || phone == null || photo == null
                || (isStudent && (studentId == null || department == null));
        if (hasRowError) {
            return null;
        }

        return new BulkMemberRow(id, englishName, birthDate, nationality, birthTime, birthRegion, gender,
                entryDate, email, phone, address, studentId, department, photo.bytes(), photo.fileName());
    }

    private boolean isValidStudentId(String studentId) {
        return studentId.matches("\\d{1,10}");
    }

    private String requireText(String value, int rowNumber, String field, List<ValidationErrorDetail> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ValidationErrorDetail(rowNumber, field, "REQUIRED", field + " 값이 없습니다."));
            return null;
        }
        return value;
    }

    private Gender requireGender(String value, int rowNumber, List<ValidationErrorDetail> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ValidationErrorDetail(rowNumber, "gender", "REQUIRED", "gender 값이 없습니다."));
            return null;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add(new ValidationErrorDetail(rowNumber, "gender", "INVALID_FORMAT", "gender 값이 올바르지 않습니다."));
            return null;
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

    private LocalDate readCommonEntryDateCell(Sheet sheet, DataFormatter formatter) {
        Row row = sheet.getRow(COMMON_ENTRY_DATE_ROW);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(1);
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
            throw singleError(null, "commonEntryDate", "INVALID_FORMAT", "공통 입국날짜 형식이 올바르지 않습니다.");
        }
    }

    private LocalDate readDateCellForRow(Sheet sheet, int rowIndex, int columnIndex, DataFormatter formatter,
            int rowNumber, String field, boolean required, List<ValidationErrorDetail> errors) {
        Row row = sheet.getRow(rowIndex);
        Cell cell = row == null ? null : row.getCell(columnIndex);
        if (cell == null) {
            if (required) {
                errors.add(new ValidationErrorDetail(rowNumber, field, "REQUIRED", field + " 값이 없습니다."));
            }
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            if (required) {
                errors.add(new ValidationErrorDetail(rowNumber, field, "REQUIRED", field + " 값이 없습니다."));
            }
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            errors.add(new ValidationErrorDetail(rowNumber, field, "INVALID_FORMAT", field + " 형식이 올바르지 않습니다."));
            return null;
        }
    }

    private LocalTime readTimeCell(Row row, int columnIndex, DataFormatter formatter, int rowNumber, List<ValidationErrorDetail> errors) {
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
            errors.add(new ValidationErrorDetail(rowNumber, "birthTime", "INVALID_FORMAT", "birthTime 형식이 올바르지 않습니다."));
            return null;
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

    private BulkValidationException singleError(Integer row, String field, String code, String message) {
        return new BulkValidationException(List.of(new ValidationErrorDetail(row, field, code, message)));
    }

    private record PhotoEntry(String fileName, byte[] bytes) {
    }
}
