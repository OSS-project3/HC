package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.exception.ValidationErrorDetail;
import com.example.honorcitizen.domain.application.dto.validation.ApplicationFieldFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 단체 신청 ZIP 파일(엑셀 + 사진)을 파싱하는 컴포넌트.
 *
 * [기대하는 ZIP 파일 구조]
 * ZIP 루트/
 *   ├── members.xlsx       ← 반드시 1개, 위치는 루트 폴더 직속
 *   ├── 1.jpg              ← 엑셀 사진 번호 열 값과 파일명(확장자 제외)이 매칭
 *   ├── 2.png
 *   └── 3.jpg
 * (하위 폴더의 파일, __MACOSX, .DS_Store는 모두 무시)
 *
 * [엑셀 행 구조]
 *   1행: A열="공통 입국날짜" 라벨, B열=날짜 값(선택) — 멤버 전체에 적용될 기본 입국날짜
 *   2행: (빈 행 또는 부제목)
 *   3행: 헤더 (읽지 않음, 상수 HEADER_ROW)
 *   4행~: 실제 데이터 (FIRST_DATA_ROW = 3, 0-indexed)
 *
 * [열 순서]
 *   0:사진 번호  1:영문명  2:생년월일  3:국적  4:출생시간  5:출생지역  6:성별
 *   7:개별입국날짜  8:이메일  9:전화번호  10:주소  [11:학번  12:학과]
 *   학번·학과는 학생증 카드(isStudent=true)일 때만 필수로 읽는다.
 *   주소는 개인 신청과 동일한 정책(2026-09-13 통일) — 학생증이면 값이 있으면 거절, 그 외
 *   카드종류는 필수.
 *
 * [오류 처리 정책 — 전체 실패(all-or-nothing)]
 * 오류가 하나라도 있으면 성공한 행도 버리고 전체를 실패 처리한다.
 * 이유: 일부 행만 신청이 생성되면 나머지 멤버를 처리하기 위해 다시 ZIP을 올려야 하는데
 *       이 때 중복 신청이 발생할 위험이 있다.
 *       전체 실패 정책을 쓰면 재시도가 항상 안전하다.
 *
 * 오류는 모든 행을 검사한 후 한 번에 반환한다(BulkValidationException.errors[]).
 * 이유: 첫 오류에서 중단하면 사용자가 한 번에 하나씩만 수정해야 하므로 여러 번 업로드해야 한다.
 *       모든 오류를 한 번에 보여주면 1번의 수정으로 재시도할 수 있다.
 */
@Component
class BulkExcelParser {

    // 0-indexed 행 번호 상수 (Apache POI는 0-based 인덱스 사용)
    private static final int COMMON_ENTRY_DATE_ROW = 0; // 1행: 공통 입국날짜
    private static final int HEADER_ROW = 2;             // 3행: 헤더
    private static final int FIRST_DATA_ROW = 3;         // 4행: 첫 번째 데이터 행

    // BULK_EXCEL_TEMPLATE_POLICY.md §9 — 업로드 한도(2026-09-20 확정, QA 체크리스트 11번).
    private static final long DEFAULT_MAX_EXCEL_BYTES = 5L * 1024 * 1024; // Excel 5 MiB
    private static final long DEFAULT_MAX_DECOMPRESSED_TOTAL_BYTES = 250L * 1024 * 1024; // 해제 후 누적 250 MiB
    private static final int MAX_ZIP_ENTRIES = 110; // ZIP 내부 유효 파일 최대 110개
    private static final int MAX_MEMBERS = 100; // 단체 신청 구성원 최대 100명
    private static final int READ_BUFFER_SIZE = 8192;

    // BULK_EXCEL_TEMPLATE_POLICY.md §4.1/4.2 — 헤더명·열 개수·열 순서 계약(2026-09-20 확정,
    // QA 체크리스트 12번). 일반카드·고등학교 학생증은 동일한 11열을 쓰고, 대학교 학생증만
    // 학번·학과 2열이 추가된 13열을 쓴다.
    private static final String[] COMMON_HEADERS_11 = {
            "사진 번호", "영문명", "생년월일", "국적", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소"
    };
    private static final String[] UNIVERSITY_HEADERS_13 = {
            "사진 번호", "영문명", "생년월일", "국적", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소", "학번", "학과"
    };

    private final ApplicationPhotoValidator applicationPhotoValidator;
    private final long maxExcelBytes;
    private final long maxDecompressedTotalBytes;

    // 생성자가 2개라 Spring이 어느 쪽을 쓸지 스스로 못 정한다(둘 다 같은 접근제어자 — 명시하지
    // 않으면 NoSuchMethodException으로 빈 생성 자체가 실패한다) — 운영 빈은 항상 이 생성자를 쓴다.
    @Autowired
    BulkExcelParser(ApplicationPhotoValidator applicationPhotoValidator) {
        this(applicationPhotoValidator, DEFAULT_MAX_EXCEL_BYTES, DEFAULT_MAX_DECOMPRESSED_TOTAL_BYTES);
    }

    // 테스트에서 실제 5MiB/250MiB를 채우지 않고도 누적 상한 로직을 검증할 수 있도록 연 생성자.
    // Spring이 관리하는 운영 빈은 항상 위 1-인자 생성자(=정책값)만 쓴다.
    BulkExcelParser(ApplicationPhotoValidator applicationPhotoValidator, long maxExcelBytes, long maxDecompressedTotalBytes) {
        this.applicationPhotoValidator = applicationPhotoValidator;
        this.maxExcelBytes = maxExcelBytes;
        this.maxDecompressedTotalBytes = maxDecompressedTotalBytes;
    }

    /**
     * ZIP 파일을 파싱해 멤버 목록(BulkMemberRow)을 반환한다.
     *
     * [처리 흐름]
     * 1. ZipInputStream을 한 번 순회해 .xlsx 파일과 사진 파일을 분리 수집한다.
     *    두 번 순회하지 않는 이유: ZipInputStream은 forward-only 스트림이어서 되감기가 불가능하다.
     *    파일이 크면 byte[]로 읽어두는 메모리 비용이 발생하지만, 단체 신청의 경우 파일 크기가
     *    제한적이므로 허용 가능한 트레이드오프다.
     *
     * 2. 엑셀이 정확히 1개인지 검증한다.
     *    2개 이상이면 어느 파일이 기준인지 알 수 없어 전체 실패 처리한다.
     *
     * 3. parseExcel()로 엑셀을 파싱하고 사진을 사진 번호로 매칭한다.
     *
     * @param isStudent 학생증 카드 여부 — true이면 학번·학과 컬럼도 파싱·검증한다.
     * @param schoolType 학교구분(개인 신청과 동일한 정책) — UNIVERSITY일 때만 학번·학과를 요구한다.
     *                   HIGH_SCHOOL이면 학번·학과 열에 값이 있어도 거절한다(isStudent=false면 무시).
     */
    List<BulkMemberRow> parse(MultipartFile zipFile, boolean isStudent, SchoolType schoolType) {
        // 사진을 사진 번호 → PhotoEntry 맵으로 관리해 엑셀 파싱 시 O(1) 매칭이 가능하게 한다.
        Map<String, PhotoEntry> photosById = new HashMap<>();
        List<byte[]> excelCandidates = new ArrayList<>();
        long[] decompressedTotal = {0L};
        int entryCount = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();

                // 디렉터리와 macOS 아티팩트(__MACOSX/ 하위 전체, .DS_Store)는 유효 파일 수 자체에서
                // 제외한다(BULK_EXCEL_TEMPLATE_POLICY.md §9). 나머지는 하위 폴더 파일이라도 일단
                // 유효 엔트리 수에 포함시켜 압축 폭탄성 "대량 소파일" 시도를 조기에 차단한다.
                if (entry.isDirectory() || isMacosxArtifact(name)) {
                    continue;
                }
                entryCount++;
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw singleError(null, "submitFile", "ZIP_TOO_MANY_ENTRIES",
                            "ZIP 내부 유효 파일 수가 최대 " + MAX_ZIP_ENTRIES + "개를 초과했습니다.");
                }

                // 하위 폴더 파일은 위에서 이미 카운트했지만 내용은 처리하지 않는다(루트 직속 파일만
                // 신청 데이터로 인정).
                if (!isRootEntry(name)) {
                    continue;
                }

                if (name.toLowerCase().endsWith(".xlsx")) {
                    // .xls (구 형식)은 지원하지 않는다 — WorkbookFactory로 읽을 수는 있으나
                    // 엑셀 템플릿을 .xlsx로 제공하므로 사용자가 .xlsx로 제출해야 한다.
                    byte[] excelBytes = readAllBounded(zipInputStream, decompressedTotal);
                    if (excelBytes.length > maxExcelBytes) {
                        throw singleError(null, "submitFile", "EXCEL_TOO_LARGE",
                                "Excel 파일 크기가 최대 " + (maxExcelBytes / (1024 * 1024)) + "MiB를 초과했습니다.");
                    }
                    excelCandidates.add(excelBytes);
                } else {
                    // 파일명에서 확장자를 제거한 값을 사진 번호 키로 사용한다.
                    // "1.jpg" → ID="1", "photo_001.PNG" → ID="photo_001"
                    // toLowerCase()로 통일해 "1.JPG"와 "1.jpg"가 같은 사진 번호로 처리되도록 한다.
                    String id = stripExtension(name);
                    String normalizedId = id.toLowerCase();
                    if (photosById.containsKey(normalizedId)) {
                        throw singleError(null, "photo", "PHOTO_DUPLICATE", "동일 사진 번호에 대한 사진 파일이 2개 이상입니다.");
                    }
                    photosById.put(normalizedId, new PhotoEntry(name, readAllBounded(zipInputStream, decompressedTotal)));
                }
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_ZIP);
        }

        // 엑셀이 없거나 2개 이상이면 파싱 자체가 불가능하므로 단일 오류로 즉시 반환한다.
        if (excelCandidates.isEmpty()) {
            throw singleError(null, "submitFile", "EXCEL_NOT_FOUND", "ZIP 루트에 엑셀 파일이 없습니다.");
        }
        if (excelCandidates.size() > 1) {
            throw singleError(null, "submitFile", "EXCEL_DUPLICATE", "ZIP 루트에 엑셀 파일이 2개 이상입니다.");
        }

        return parseExcel(excelCandidates.get(0), photosById, isStudent, schoolType);
    }

    // ZIP 엔트리 이름에 '/'가 포함되면 하위 폴더 항목이다. 루트 직속 파일만 신청 데이터로 처리한다.
    private boolean isRootEntry(String name) {
        return !name.contains("/");
    }

    // macOS가 자동 생성하는 .DS_Store 파일과 __MACOSX/ 하위 전체를 무시한다(유효 파일 수 집계에서도 제외).
    private boolean isMacosxArtifact(String name) {
        return name.equals(".DS_Store") || name.startsWith("__MACOSX/");
    }

    /**
     * ZIP 엔트리를 byte[]로 읽으면서 압축 해제 후 누적 크기를 실시간으로 제한한다(압축 폭탄 방어,
     * BULK_EXCEL_TEMPLATE_POLICY.md §9 — "압축 해제 후 전체 크기"는 다 읽은 뒤가 아니라 읽는 도중
     * 제한해야 한다). 여러 엔트리에 걸친 누적 합계이므로 호출자가 공유하는 decompressedTotal을
     * 통해 상태를 이어간다(이 클래스는 싱글턴 빈이라 인스턴스 필드로 요청별 상태를 못 둔다).
     */
    private byte[] readAllBounded(InputStream inputStream, long[] decompressedTotal) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[READ_BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
            decompressedTotal[0] += read;
            if (decompressedTotal[0] > maxDecompressedTotalBytes) {
                throw singleError(null, "submitFile", "ZIP_DECOMPRESSED_TOO_LARGE",
                        "ZIP 압축 해제 후 전체 크기가 최대 " + (maxDecompressedTotalBytes / (1024 * 1024)) + "MiB를 초과했습니다.");
            }
        }
        return buffer.toByteArray();
    }

    /**
     * 엑셀 바이트 배열을 파싱해 BulkMemberRow 목록을 반환한다.
     *
     * [오류 누적 전략]
     * 각 행의 파싱 오류는 즉시 throw하지 않고 errors 리스트에 누적한다.
     * 모든 행을 파싱한 후 errors가 비어있으면 정상 반환, 있으면 BulkValidationException으로 한 번에 던진다.
     * 이 방식으로 사용자는 파일을 여러 번 올리지 않고 한 번에 모든 오류를 확인할 수 있다.
     *
     * [빈 행 처리]
     * ID 열(A열)이 비어있는 행은 데이터 없는 행으로 간주하고 건너뛴다.
     * 중간에 빈 행이 있어도 이후 데이터 행을 계속 읽는다.
     * 이유: 사용자가 가독성을 위해 중간에 빈 행을 넣는 경우가 있어서다.
     *
     * [예외 처리]
     * CustomException(우리 비즈니스 예외)은 그대로 전파하고,
     * 나머지 모든 예외(POI 파싱 오류 등)는 EXCEL_UNREADABLE로 변환해 사용자에게 명확한 메시지를 준다.
     */
    private List<BulkMemberRow> parseExcel(byte[] excelBytes, Map<String, PhotoEntry> photosById, boolean isStudent,
            SchoolType schoolType) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트만 읽는다
            DataFormatter formatter = new DataFormatter();

            // 헤더명·열 개수·열 순서가 공식 양식과 정확히 일치하는지 먼저 확인한다(2026-09-20
            // 확정 정책, QA 체크리스트 12번) — 열 위치 기반으로 이후 모든 셀을 읽으므로, 헤더가
            // 어긋난 상태로 데이터를 계속 읽으면 엉뚱한 열을 다른 필드로 오인해 저장할 수 있다.
            validateHeader(sheet, formatter, isStudent, schoolType);

            // 1행 B열에서 공통 입국날짜를 읽는다. 없으면 null이고 개별 행에서 대체된다.
            LocalDate commonEntryDate = readCommonEntryDateCell(sheet, formatter);

            List<BulkMemberRow> rows = new ArrayList<>();
            List<ValidationErrorDetail> errors = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();
            int applicantRowCount = 0;
            boolean memberCountExceeded = false;

            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = FIRST_DATA_ROW; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                // 사진 번호(A열)는 공식 양식에 001~100이 미리 채워져 있다.
                // 따라서 A열 값만으로 신청자 행을 판단하지 않고, 사용자가 입력하는 B열 이후에
                // 값이 하나라도 있는 행만 실제 신청 데이터로 검증·처리한다.
                if (!hasApplicantInput(row, isStudent, schoolType, formatter)) {
                    continue;
                }

                applicantRowCount++;
                if (applicantRowCount > MAX_MEMBERS) {
                    // 이미 전체 실패가 확정된 사안이라 초과분 각 행까지 개별 검증할 필요는 없다 —
                    // 오류 하나만 남기고(중복 누적 방지) 나머지 행은 건너뛴다.
                    if (!memberCountExceeded) {
                        errors.add(new ValidationErrorDetail(null, "submitFile", "MEMBER_COUNT_EXCEEDED",
                                "단체 신청 구성원은 최대 " + MAX_MEMBERS + "명까지 가능합니다."));
                        memberCountExceeded = true;
                    }
                    continue;
                }

                String photoNumber = stringValue(row, 0, formatter);
                if (photoNumber == null || photoNumber.isBlank()) {
                    errors.add(new ValidationErrorDetail(
                            rowIndex + 1, "photoNumber", "REQUIRED", "사진 번호가 없습니다."));
                    continue;
                }

                String normalizedPhotoNumber = photoNumber.toLowerCase();
                if (!seenIds.add(normalizedPhotoNumber)) {
                    errors.add(new ValidationErrorDetail(
                            rowIndex + 1, "photoNumber", "DUPLICATE_ID", "사진 번호가 중복되었습니다."));
                    continue;
                }

                // parseRow는 오류가 있으면 errors에 누적하고 null을 반환한다.
                BulkMemberRow parsed = parseRow(
                        row, photoNumber, commonEntryDate, photosById, isStudent, schoolType, formatter, errors);
                if (parsed != null) {
                    rows.add(parsed);
                }
            }

            for (PhotoEntry remainingPhoto : photosById.values()) {
                errors.add(new ValidationErrorDetail(null, "photo", "PHOTO_UNMATCHED", "신청자 행의 사진 번호와 매칭되지 않는 사진 파일입니다: " + remainingPhoto.fileName()));
            }

            // 유효한 행도 없고 오류도 없으면 → 데이터가 없는 빈 파일
            if (rows.isEmpty() && errors.isEmpty()) {
                errors.add(new ValidationErrorDetail(null, "submitFile", "EMPTY_EXCEL", "엑셀에 유효한 데이터 행이 없습니다."));
            }

            // 오류가 하나라도 있으면 성공한 행도 함께 버리고 전체 실패 처리한다.
            if (!errors.isEmpty()) {
                throw new BulkValidationException(errors);
            }
            return rows;

        } catch (CustomException e) {
            // BulkValidationException(CustomException 하위)은 그대로 전파한다.
            throw e;
        } catch (Exception e) {
            // POI 파싱 오류, 암호화된 파일 등 모든 예외를 사용자 친화적 메시지로 변환한다.
            throw singleError(null, "submitFile", "EXCEL_UNREADABLE", "엑셀 파일을 읽을 수 없습니다.");
        }
    }

    /**
     * 헤더명·열 개수·열 순서가 공식 양식과 정확히 일치하는지 검증한다(BULK_EXCEL_TEMPLATE_POLICY.md
     * §3.2 "헤더명, 열 개수, 열 순서는 공식 양식과 정확히 일치해야 한다", 2026-09-20 확정).
     * 공백만 다른 경우까지 거절하면 사용자 실수에 지나치게 엄격하므로 앞뒤 공백만 제거한 뒤 비교한다
     * (stringValue가 이미 trim 처리). 정의되지 않은 추가 열도 허용하지 않는다.
     */
    private void validateHeader(Sheet sheet, DataFormatter formatter, boolean isStudent, SchoolType schoolType) {
        String[] expected = expectedHeaders(isStudent, schoolType);
        Row headerRow = sheet.getRow(HEADER_ROW);
        if (headerRow == null) {
            throw singleError(null, "submitFile", "HEADER_MISMATCH", "엑셀 헤더가 공식 양식과 일치하지 않습니다.");
        }
        for (int i = 0; i < expected.length; i++) {
            String actual = stringValue(headerRow, i, formatter);
            if (!expected[i].equals(actual)) {
                throw singleError(null, "submitFile", "HEADER_MISMATCH",
                        (i + 1) + "번째 열 헤더가 공식 양식과 일치하지 않습니다.");
            }
        }
        if (stringValue(headerRow, expected.length, formatter) != null) {
            throw singleError(null, "submitFile", "HEADER_MISMATCH", "정의되지 않은 추가 열이 있습니다.");
        }
    }

    // 일반카드·고등학교 학생증은 동일한 11열, 대학교 학생증만 학번·학과가 추가된 13열이다
    // (BULK_EXCEL_TEMPLATE_POLICY.md §4.2 — 고등학교 양식에는 학번·학과 열 자체가 없다).
    private String[] expectedHeaders(boolean isStudent, SchoolType schoolType) {
        if (isStudent && schoolType == SchoolType.UNIVERSITY) {
            return UNIVERSITY_HEADERS_13;
        }
        return COMMON_HEADERS_11;
    }

    /**
     * 사진 번호를 제외한 사용자 입력 열에 값이 있는지 확인한다.
     *
     * 공식 양식은 4~103행 A열에 001~100을 미리 채우므로 사진 번호만 있는 행은 빈 행이다.
     * 반면 B열 이후에 값이 하나라도 있으면 부분 작성 행으로 보고 parseRow에서 필수값 오류까지 수집한다.
     */
    private boolean hasApplicantInput(Row row, boolean isStudent, SchoolType schoolType, DataFormatter formatter) {
        if (row == null) {
            return false;
        }

        // 학번·학과 열(11~12)은 학생증+대학교일 때만 실제로 존재한다 — 그 외(비학생증, 학생증+고등학교)는
        // 10열까지만 확인해도 충분하다.
        int lastApplicantColumn = isStudent && schoolType == SchoolType.UNIVERSITY ? 12 : 10;
        for (int columnIndex = 1; columnIndex <= lastApplicantColumn; columnIndex++) {
            if (stringValue(row, columnIndex, formatter) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 엑셀의 단일 행을 파싱해 BulkMemberRow를 생성한다.
     *
     * [오류 누적 패턴]
     * 각 필드를 파싱할 때 오류가 발생하면 즉시 throw하지 않고 errors에 추가한 뒤 null을 반환한다.
     * 이 방식으로 한 행에 여러 오류가 있어도 모두 수집할 수 있다.
     *
     * 행 파싱이 모두 끝난 후 hasRowError를 확인한다.
     * 필수 필드 중 하나라도 null이면 이 행은 실패로 간주하고 null을 반환한다.
     * 오류 내용은 이미 errors에 쌓여 있으므로 중복 추가하지 않는다.
     *
     * [입국날짜 대체 로직]
     * 개별 행의 입국날짜(7열)가 없으면 공통 입국날짜(1행 B열)로 대체한다.
     * 공통 날짜도 없으면 null로 저장되며, 이는 허용된 상태다(입국날짜는 선택 필드).
     *
     * @param photoNumber 엑셀 A열의 사진 번호 (사진 파일명 매칭 키)
     * @param errors 오류를 누적할 리스트 (호출자와 공유됨)
     * @return 파싱 성공 시 BulkMemberRow, 필수 필드 오류 시 null
     */
    private BulkMemberRow parseRow(Row row, String photoNumber, LocalDate commonEntryDate, Map<String, PhotoEntry> photosById,
            boolean isStudent, SchoolType schoolType, DataFormatter formatter, List<ValidationErrorDetail> errors) {

        // Apache POI의 getRowNum()은 0-based이므로 사용자에게 보여줄 1-based 행 번호로 변환한다.
        int rowNumber = row.getRowNum() + 1;

        String englishName = requireText(stringValue(row, 1, formatter), rowNumber, "englishName", errors);
        englishName = checkMaxLength(englishName, 100, rowNumber, "englishName", errors);
        LocalDate birthDate = readDateCellForRow(row.getSheet(), row.getRowNum(), 2, formatter, rowNumber, "birthDate", true, errors);
        birthDate = checkPastBirthDate(birthDate, rowNumber, errors);
        String nationality = requireText(stringValue(row, 3, formatter), rowNumber, "nationality", errors);
        nationality = checkValidNationality(nationality, rowNumber, errors);
        LocalTime birthTime = readTimeCell(row, 4, formatter, rowNumber, errors); // 출생시간: 선택 항목
        String birthRegion = requireText(stringValue(row, 5, formatter), rowNumber, "birthRegion", errors);
        birthRegion = checkMaxLength(birthRegion, 200, rowNumber, "birthRegion", errors); // 출생지역: 필수 도시명
        Gender gender = requireGender(stringValue(row, 6, formatter), rowNumber, errors);

        // 개별 입국날짜가 없으면 공통 입국날짜로 폴백한다.
        LocalDate rowEntryDate = readDateCellForRow(row.getSheet(), row.getRowNum(), 7, formatter, rowNumber, "entryDate", false, errors);
        LocalDate entryDate = rowEntryDate != null ? rowEntryDate : commonEntryDate;

        String email = requireText(stringValue(row, 8, formatter), rowNumber, "email", errors);
        email = checkValidEmail(email, rowNumber, errors);
        // phone 형식 검증은 보류: 국제 전화번호 정책(외국인 신청자 고려)이 아직 확정되지 않아
        // 국내향 정규식을 임의로 적용하지 않는다(PENDING_DECISIONS.md 참고). 필수 여부만 확인한다.
        String phone = requireText(stringValue(row, 9, formatter), rowNumber, "phone", errors);
        // 주소는 개인 신청과 동일한 정책(admin-saju.md 확정, 2026-09-13 단체까지 통일) — 학생증은
        // 카드에 주소를 표시하지 않으므로 값이 있으면 거절하고, 그 외 카드종류는 필수로 받는다.
        String address = null;
        if (isStudent) {
            if (stringValue(row, 10, formatter) != null) {
                errors.add(new ValidationErrorDetail(rowNumber, "address", "INVALID_INPUT", "학생증은 주소를 입력할 수 없습니다."));
            }
        } else {
            address = requireText(stringValue(row, 10, formatter), rowNumber, "address", errors);
            address = checkMaxLength(address, 255, rowNumber, "address", errors);
        }

        String studentId = null;
        String department = null;
        if (isStudent && schoolType == SchoolType.UNIVERSITY) {
            // 학번·학과는 학생증+대학교에서만 필수로 검증한다(개인 신청과 동일한 정책).
            studentId = requireText(stringValue(row, 11, formatter), rowNumber, "studentId", errors);
            if (studentId != null && !isValidStudentId(studentId)) {
                // requireText를 통과해도 형식이 맞지 않으면 추가 검증한다.
                errors.add(new ValidationErrorDetail(rowNumber, "studentId", "INVALID_FORMAT", "학번은 최대 10자·숫자만 허용합니다."));
                studentId = null; // 형식 오류이므로 null로 처리해 hasRowError에서 감지되도록 한다.
            }
            department = requireText(stringValue(row, 12, formatter), rowNumber, "department", errors);
            department = checkMaxLength(department, 100, rowNumber, "department", errors);
        } else if (isStudent && schoolType == SchoolType.HIGH_SCHOOL) {
            // 고등학교는 학번·학과 자체가 없는 개념이라 값이 있으면 오류로 취급한다(개인 신청과 동일).
            if (stringValue(row, 11, formatter) != null || stringValue(row, 12, formatter) != null) {
                errors.add(new ValidationErrorDetail(rowNumber, "studentId", "INVALID_INPUT", "고등학교는 학번·학과를 입력할 수 없습니다."));
            }
        }

        // 실제 신청 데이터가 입력된 행의 사진 번호로 ZIP 루트 사진을 정확히 매칭한다.
        // 번호만 미리 채워진 빈 행은 이 메서드에 도달하지 않으므로 해당 번호의 사진도 요구하지 않는다.
        PhotoEntry photo = photosById.remove(photoNumber.toLowerCase());
        if (photo == null) {
            errors.add(new ValidationErrorDetail(rowNumber, "photo", "PHOTO_NOT_FOUND", "사진 번호에 매칭되는 사진을 찾을 수 없습니다."));
        } else {
            // 사진 내용 검증(2026-09-19, requirements.md 5-1) — 이전까지는 ZIP 안의 사진이 실제
            // 이미지인지 전혀 확인하지 않아, 손상되거나 이미지가 아닌 파일도 그대로 통과해 카드
            // 생성 단계에서야 원인 불명의 오류로 터졌다. 개인 신청과 동일한 검증(용량·확장자·
            // 매직넘버·디코딩·최소해상도)을 여기서 선행 적용한다. 다른 행 오류와 동일하게
            // 즉시 던지지 않고 errors에 모아 all-or-nothing 정책을 유지한다.
            try {
                applicationPhotoValidator.validateFacePhotoBytes(photo.bytes(), photo.fileName());
            } catch (CustomException e) {
                errors.add(new ValidationErrorDetail(rowNumber, "photo", e.getErrorCode().name(), e.getErrorCode().getMessage()));
                photo = null;
            }
        }

        // 필수 필드 중 하나라도 null이면 이 행은 실패로 간주한다.
        // errors에는 이미 해당 필드의 오류 메시지가 추가되어 있다.
        boolean hasRowError = englishName == null || birthDate == null || nationality == null || birthRegion == null || gender == null
                || email == null || phone == null || photo == null
                || (!isStudent && address == null)
                || (isStudent && stringValue(row, 10, formatter) != null)
                || (isStudent && schoolType == SchoolType.UNIVERSITY && (studentId == null || department == null))
                || (isStudent && schoolType == SchoolType.HIGH_SCHOOL
                        && (stringValue(row, 11, formatter) != null || stringValue(row, 12, formatter) != null));
        if (hasRowError) {
            return null; // null을 반환해 이 행이 실패했음을 호출자에게 알린다.
        }

        return new BulkMemberRow(photoNumber, englishName, birthDate, nationality, birthTime, birthRegion, gender,
                entryDate, email, phone, address, studentId, department, photo.bytes(), photo.fileName());
    }

    // 학번 형식 검증: 숫자만, 최대 10자리.
    // 국내 대학 학번은 대부분 8~10자리 숫자이므로 이 범위로 제한한다.
    private boolean isValidStudentId(String studentId) {
        return studentId.matches("\\d{1,10}");
    }

    /**
     * 개인 신청 DTO(ApplicationCreateRequest)와 동일한 판정 로직(ApplicationFieldFormats)을
     * 재사용해 개인/단체 신청 간 검증 정책이 갈라지지 않도록 한다.
     */
    private String checkValidEmail(String email, int rowNumber, List<ValidationErrorDetail> errors) {
        if (email != null && !ApplicationFieldFormats.isValidEmail(email)) {
            errors.add(new ValidationErrorDetail(rowNumber, "email", "INVALID_FORMAT", "이메일 형식이 올바르지 않습니다."));
            return null;
        }
        return email;
    }

    private String checkValidNationality(String nationality, int rowNumber, List<ValidationErrorDetail> errors) {
        if (nationality != null && !ApplicationFieldFormats.isValidNationality(nationality)) {
            errors.add(new ValidationErrorDetail(rowNumber, "nationality", "INVALID_FORMAT", "국적은 ISO 3166-1 alpha-2 국가 코드여야 합니다."));
            return null;
        }
        return nationality;
    }

    // 개인 신청 DTO의 @Past에 대응하는 검증 — 미래 날짜만 막는다(최소연도 제한은 비즈니스 근거가 없어 두지 않는다).
    private LocalDate checkPastBirthDate(LocalDate birthDate, int rowNumber, List<ValidationErrorDetail> errors) {
        if (birthDate == null) {
            return null;
        }
        if (birthDate.isAfter(LocalDate.now())) {
            errors.add(new ValidationErrorDetail(rowNumber, "birthDate", "INVALID_FORMAT", "birthDate는 미래일 수 없습니다."));
            return null;
        }
        return birthDate;
    }

    private String checkMaxLength(String value, int max, int rowNumber, String field, List<ValidationErrorDetail> errors) {
        if (value != null && value.length() > max) {
            errors.add(new ValidationErrorDetail(rowNumber, field, "INVALID_FORMAT", field + "은 최대 " + max + "자까지 허용합니다."));
            return null;
        }
        return value;
    }

    /**
     * 필수 텍스트 필드를 검증한다.
     * 값이 null이거나 공백이면 errors에 REQUIRED 오류를 추가하고 null을 반환한다.
     * 값이 있으면 trim된 값을 그대로 반환한다 (stringValue에서 이미 trim됨).
     */
    private String requireText(String value, int rowNumber, String field, List<ValidationErrorDetail> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ValidationErrorDetail(rowNumber, field, "REQUIRED", field + " 값이 없습니다."));
            return null;
        }
        return value;
    }

    /**
     * Gender enum 값을 파싱한다.
     *
     * 허용 입력값: "MALE", "FEMALE", "male", "Male" 등 대소문자 무관
     *   (trim().toUpperCase()로 정규화 후 Gender.valueOf로 파싱)
     *
     * 오류 종류:
     * - 빈 값: REQUIRED
     * - 잘못된 값(예: "남성"): INVALID_FORMAT
     */
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

    /**
     * 셀 값을 문자열로 읽는다.
     *
     * DataFormatter를 사용하는 이유:
     * Apache POI에서 날짜 셀은 NUMERIC 타입으로 저장된다.
     * Cell.getStringCellValue()를 쓰면 숫자 직렬값(예: 45678)이 반환된다.
     * DataFormatter는 셀에 적용된 포맷(날짜 형식 등)을 그대로 표시하므로
     * 사용자가 엑셀에서 보는 값과 동일한 문자열을 얻을 수 있다.
     * 다만, 날짜·시간 파싱은 NUMERIC 타입을 직접 처리하는 readDateCellForRow, readTimeCell이 담당한다.
     *
     * 빈 셀(null)과 공백 문자열은 null로 반환해 이후 검증에서 "값 없음"으로 처리한다.
     */
    private String stringValue(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }

    /**
     * 엑셀 1행 B열에서 공통 입국날짜를 읽는다.
     *
     * [두 가지 입력 방식 지원]
     * 1. 엑셀 날짜 셀(NUMERIC 타입): 날짜 피커로 입력한 경우. getLocalDateTimeCellValue()로 직접 변환.
     * 2. 텍스트 셀: ISO 8601 형식(yyyy-MM-dd)으로 입력한 경우. LocalDate.parse()로 변환.
     *
     * 공통 입국날짜는 선택 항목이므로 없으면 null을 반환한다.
     * 단, 입력이 있는데 형식이 잘못된 경우 행별 오류가 아닌 파일 전체 오류로 처리한다.
     * 이유: 공통 날짜가 잘못되면 모든 행에 영향을 미쳐 행별 오류로 표시하기 부적절하기 때문이다.
     */
    private LocalDate readCommonEntryDateCell(Sheet sheet, DataFormatter formatter) {
        Row row = sheet.getRow(COMMON_ENTRY_DATE_ROW);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(1); // B열(0-based index=1)
        if (cell == null) {
            return null;
        }
        // 엑셀 날짜 셀은 내부적으로 숫자(1900년 이후 일수)로 저장된다.
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text); // ISO 8601 형식(yyyy-MM-dd) 파싱
        } catch (Exception e) {
            // 공통 날짜 오류는 전체 파일 실패 — 행 번호 없이 단일 오류로 처리한다.
            throw singleError(null, "commonEntryDate", "INVALID_FORMAT", "공통 입국날짜 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 특정 행·열의 날짜 셀을 파싱한다.
     *
     * [두 가지 입력 방식 지원]
     * 1. NUMERIC 타입: 엑셀 날짜 피커로 입력. getLocalDateTimeCellValue()로 변환.
     * 2. 텍스트 타입: ISO 8601 문자열(yyyy-MM-dd). LocalDate.parse()로 변환.
     *
     * @param required true이면 빈 값을 REQUIRED 오류로 처리, false이면 null 반환(선택 필드)
     */
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

    /**
     * 출생시간 셀을 파싱한다.
     *
     * 출생시간은 선택 항목이므로 빈 값은 오류 없이 null을 반환한다.
     * 값이 있지만 형식이 잘못된 경우만 INVALID_FORMAT 오류를 누적한다.
     *
     * [두 가지 입력 방식 지원]
     * 1. NUMERIC 타입: 엑셀 시간 셀. getLocalDateTimeCellValue().toLocalTime()으로 변환.
     * 2. 텍스트 타입: ISO 8601 시간 문자열(HH:mm 또는 HH:mm:ss). LocalTime.parse()로 변환.
     */
    private LocalTime readTimeCell(Row row, int columnIndex, DataFormatter formatter, int rowNumber, List<ValidationErrorDetail> errors) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null; // 선택 항목이므로 null 반환
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalTime();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null; // 선택 항목이므로 null 반환
        }
        try {
            return LocalTime.parse(text);
        } catch (Exception e) {
            errors.add(new ValidationErrorDetail(rowNumber, "birthTime", "INVALID_FORMAT", "birthTime 형식이 올바르지 않습니다."));
            return null;
        }
    }

    // 파일명에서 마지막 '.' 이후를 제거해 ID 키를 추출한다.
    // 예: "1.jpg" → "1", "photo_001.PNG" → "photo_001", "noextension" → "noextension"
    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }


    /**
     * 단일 오류를 BulkValidationException으로 래핑하는 헬퍼 메서드.
     *
     * 파일 구조 오류(엑셀 없음, 엑셀 2개 등)처럼 행 번호가 없는 파일 수준 오류에 사용한다.
     * 행별 오류는 parseRow에서 직접 errors 리스트에 추가한다.
     */
    private BulkValidationException singleError(Integer row, String field, String code, String message) {
        return new BulkValidationException(List.of(new ValidationErrorDetail(row, field, code, message)));
    }

    /**
     * ZIP에서 읽은 사진 파일의 임시 보관 레코드.
     *
     * fileName: ZIP 내 원본 파일명 (예: "1.jpg") — S3 업로드 시 sanitize된 파일명으로 사용한다.
     * bytes: 파일 내용 전체 — ZipInputStream은 되감기가 불가능해 미리 byte[]로 읽어둔다.
     *        메모리에 올리므로 단일 사진이 너무 크면 OOM이 발생할 수 있다.
     *        (업로드 크기 제한을 Spring Multipart 설정으로 관리한다)
     */
    private record PhotoEntry(String fileName, byte[] bytes) {
    }
}
