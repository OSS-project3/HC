package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.exception.ValidationErrorDetail;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * saju 프로그램이 되돌려준 "사주이름 포함" 엑셀을 파싱한다.
 *
 * [입력 파일 특성]
 * saju는 HC가 단체 신청 제출용으로 내보낸 원본 엑셀(members.xlsx)의 다른 열은 하나도 건드리지
 * 않고 "사주이름" 열 하나만 채워서 그대로 돌려준다(saju web BatchDonePage.tsx buildWorkbook 참고).
 * 즉 이 파일의 이메일·전화번호 값은 신청 당시 제출된 원본 그대로다 — 지금 파서(BulkExcelParser)가
 * 이메일·전화번호를 정규화하지 않고 원문 그대로 저장하므로(§13 체크리스트 미완료 항목), 여기서도
 * 별도 정규화 없이 trim만 하고 원문 그대로 비교한다.
 *
 * 열 위치가 양식마다 다르다(일반카드/고등학교 11열, 대학교 13열 + saju가 끝에 붙인 사주이름 1열).
 * 그래서 고정 인덱스가 아니라 헤더 텍스트("이메일"/"전화번호"/"사주이름")로 열을 찾는다.
 *
 * [정책 — 전체 실패(all-or-nothing), BulkExcelParser와 동일 원칙]
 * 이메일·전화번호가 없거나 매칭 대상이 없는 행, "사주이름" 형식이 이상한 행이 하나라도 있으면
 * 전체 반영을 거절한다(BulkValidationException.errors[]로 전부 반환). 아직 이름이 지정되지 않아
 * "사주이름" 셀이 빈 행은 오류가 아니라 그냥 건너뛴다.
 */
@Component
class NamingResultExcelParser {

    private static final int HEADER_ROW = 2; // 3행(1-base)
    private static final int FIRST_DATA_ROW = 3; // 4행(1-base)부터 데이터

    // "홍길동(洪吉東)" → 이름=홍길동, 한자=洪吉東. 괄호가 없으면 한자 없이 이름만.
    // 이름·한자 부분엔 괄호를 허용하지 않아서(짝이 안 맞는 괄호, 중첩 등) 형식이 이상한 값은
    // 매칭 자체가 실패해 아래에서 오류로 처리된다.
    private static final Pattern SAJU_NAME_PATTERN = Pattern.compile("^([^()]+)(?:\\(([^()]+)\\))?$");

    record NamingResultRow(int rowNumber, String email, String phone, String name, String chineseName) {
    }

    List<NamingResultRow> parse(MultipartFile file) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
        List<NamingResultRow> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(HEADER_ROW);
            if (header == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            DataFormatter formatter = new DataFormatter();
            int emailCol = -1;
            int phoneCol = -1;
            int sajuNameCol = -1;
            for (Cell cell : header) {
                String text = formatter.formatCellValue(cell).trim();
                if ("이메일".equals(text)) {
                    emailCol = cell.getColumnIndex();
                } else if ("전화번호".equals(text)) {
                    phoneCol = cell.getColumnIndex();
                } else if ("사주이름".equals(text)) {
                    sajuNameCol = cell.getColumnIndex();
                }
            }
            if (emailCol < 0 || phoneCol < 0 || sajuNameCol < 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String email = stringValue(row, emailCol, formatter);
                String phone = stringValue(row, phoneCol, formatter);
                String sajuName = stringValue(row, sajuNameCol, formatter);
                int rowNumber = r + 1;

                if (email == null && phone == null && sajuName == null) {
                    continue; // 완전히 빈 행
                }
                if (sajuName == null) {
                    continue; // 아직 이름 미지정 — 오류 아님, 이번엔 건너뜀
                }
                if (email == null || phone == null) {
                    errors.add(new ValidationErrorDetail(rowNumber, "email_phone", "REQUIRED",
                            "이메일 또는 전화번호가 없어 대상을 특정할 수 없습니다."));
                    continue;
                }

                Matcher matcher = SAJU_NAME_PATTERN.matcher(sajuName);
                if (!matcher.matches() || matcher.group(1).isBlank()) {
                    errors.add(new ValidationErrorDetail(rowNumber, "sajuName", "INVALID_FORMAT",
                            "사주이름 형식이 올바르지 않습니다."));
                    continue;
                }
                rows.add(new NamingResultRow(rowNumber, email, phone, matcher.group(1).trim(), matcher.group(2)));
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (!errors.isEmpty()) {
            throw new BulkValidationException(errors);
        }
        if (rows.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return rows;
    }

    private String stringValue(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }
}
