package com.example.honorcitizen.domain.bulk.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.bulk.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.bulk.dto.BulkDownloadResponse;
import com.example.honorcitizen.domain.bulk.entity.BulkOrder;
import com.example.honorcitizen.domain.bulk.repository.BulkOrderRepository;
import com.example.honorcitizen.domain.citizencard.entity.CitizenCard;
import com.example.honorcitizen.domain.citizencard.repository.CitizenCardRepository;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkApplicationService {

    private static final long MAX_ZIP_SIZE = 500 * 1024 * 1024L;
    private static final long PRESIGNED_URL_EXPIRY_SECONDS = 7 * 24 * 3600L;

    // XLSX 컬럼 순서 (0-based)
    private static final int COL_NAME_EN       = 0;
    private static final int COL_NATIONALITY   = 1;
    private static final int COL_BIRTH_DATE    = 2;
    private static final int COL_BIRTH_TIME    = 3;
    private static final int COL_BIRTH_REGION  = 4;
    private static final int COL_GENDER        = 5;
    private static final int COL_PHOTO_FILE    = 6;

    private final BulkOrderRepository bulkOrderRepository;
    private final ApplicationRepository applicationRepository;
    private final CitizenCardRepository citizenCardRepository;
    private final UserService userService;
    private final StorageService storageService;

    @Transactional
    public BulkApplicationCreateResponse createBulk(Long userId, MultipartFile zipFile) {
        userService.validateTermsAgreed(userId);

        if (zipFile.getSize() > MAX_ZIP_SIZE) {
            throw new CustomException(ErrorCode.ZIP_TOO_LARGE);
        }

        Map<String, byte[]> contents;
        try {
            contents = extractZip(zipFile.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_ZIP);
        }

        byte[] xlsxBytes = contents.entrySet().stream()
                .filter(e -> e.getKey().endsWith(".xlsx") && !e.getKey().contains("__MACOSX"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.EXCEL_NOT_FOUND));

        List<XlsxRow> rows;
        try {
            rows = parseXlsx(xlsxBytes);
        } catch (Exception e) {
            log.warn("XLSX 파싱 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }

        if (rows.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_PARSE_ERROR);
        }

        List<PreparedApplication> prepared = new ArrayList<>();
        List<BulkApplicationCreateResponse.FailedRow> failures = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            XlsxRow row = rows.get(i);
            int excelRowNum = i + 2;
            try {
                byte[] photoBytes = Optional.ofNullable(contents.get(row.photoFileName()))
                        .orElseThrow(() -> new IllegalArgumentException("사진 파일 없음: " + row.photoFileName()));

                String photoId = "photo_" + UUID.randomUUID().toString().replace("-", "");
                String ext = extractExtension(row.photoFileName());
                String photoPath = "photos/bulk/" + photoId + "." + ext;
                storageService.uploadBytes(photoPath, photoBytes, toContentType(ext));

                prepared.add(new PreparedApplication(row, photoId, photoPath));
            } catch (Exception e) {
                failures.add(new BulkApplicationCreateResponse.FailedRow(excelRowNum, row.nameEn(), e.getMessage()));
            }
        }

        if (prepared.isEmpty()) {
            throw new CustomException(ErrorCode.ALL_FAILED);
        }

        BulkOrder bulkOrder = bulkOrderRepository.save(
                BulkOrder.create(userId, prepared.size(), zipFile.getOriginalFilename()));

        prepared.forEach(p -> applicationRepository.save(
                Application.createBulk(userId, p.row().nameEn(), p.row().nationality(),
                        p.row().birthDate(), p.row().birthTime(), p.row().birthRegion(),
                        p.row().gender(), p.photoId(), p.photoPath(), bulkOrder.getId())));

        return BulkApplicationCreateResponse.of(bulkOrder.getId(), prepared.size(), failures);
    }

    @Transactional
    public BulkDownloadResponse getBulkDownloadUrl(Long userId, Long bulkOrderId) {
        BulkOrder bulkOrder = bulkOrderRepository.findById(bulkOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.BULK_ORDER_NOT_FOUND));
        Optional.of(bulkOrder)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));

        List<Application> applications = applicationRepository
                .findAllByBulkOrderIdOrderByIdAsc(bulkOrderId);

        Map<Long, CitizenCard> cardsByAppId = applications.stream()
                .map(app -> citizenCardRepository.findByApplicationId(app.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(CitizenCard::getApplicationId, c -> c));

        if (cardsByAppId.isEmpty()) {
            throw new CustomException(ErrorCode.NO_CARDS_ISSUED);
        }

        byte[] bulkZipBytes = createBulkZip(applications, cardsByAppId);
        String bulkZipPath = "zips/bulk_" + bulkOrderId + ".zip";
        storageService.uploadBytes(bulkZipPath, bulkZipBytes, "application/zip");

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(PRESIGNED_URL_EXPIRY_SECONDS);
        String downloadUrl = storageService.generatePresignedUrl(bulkZipPath, PRESIGNED_URL_EXPIRY_SECONDS);

        return BulkDownloadResponse.of(bulkOrderId, downloadUrl, expiresAt,
                "bulk_" + bulkOrderId + ".zip", cardsByAppId.size(), applications.size());
    }

    // --- ZIP 생성 ---

    private byte[] createBulkZip(List<Application> applications,
                                  Map<Long, CitizenCard> cardsByAppId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            int seq = 1;
            for (Application app : applications) {
                CitizenCard card = cardsByAppId.get(app.getId());
                if (card == null) continue;

                String folder = String.format("%02d_%s/", seq++, sanitizeName(app.getNameEn()));

                byte[] cardBytes = storageService.download(card.getImagePath());
                zos.putNextEntry(new ZipEntry(folder + card.getCardNumber() + ".png"));
                zos.write(cardBytes);
                zos.closeEntry();

                if (card.getNameMeaningPath() != null) {
                    byte[] meaningBytes = storageService.download(card.getNameMeaningPath());
                    zos.putNextEntry(new ZipEntry(folder + "meaning_" + card.getCardNumber() + ".png"));
                    zos.write(meaningBytes);
                    zos.closeEntry();
                }
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Bulk ZIP 생성 실패 ", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    // --- ZIP 파싱 ---

    private Map<String, byte[]> extractZip(byte[] zipBytes) throws IOException {
        Map<String, byte[]> contents = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    contents.put(entry.getName(), zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
        return contents;
    }

    // --- XLSX 파싱 ---

    private List<XlsxRow> parseXlsx(byte[] xlsxBytes) throws IOException {
        List<XlsxRow> rows = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            var sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                rows.add(new XlsxRow(
                        requireString(row, COL_NAME_EN, "영어이름"),
                        requireString(row, COL_NATIONALITY, "국적"),
                        parseDate(row, COL_BIRTH_DATE),
                        parseTimeOrDefault(row, COL_BIRTH_TIME),
                        requireString(row, COL_BIRTH_REGION, "출생지역"),
                        parseGender(row, COL_GENDER),
                        requireString(row, COL_PHOTO_FILE, "사진파일명")
                ));
            }
        }
        return rows;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = COL_NAME_EN; c <= COL_PHOTO_FILE; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String requireString(Row row, int col, String fieldName) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException(fieldName + " 필수");
        }
        return cell.getStringCellValue().trim();
    }

    private LocalDate parseDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) throw new IllegalArgumentException("생년월일 필수");
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            return LocalDate.parse(cell.getStringCellValue().trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        throw new IllegalArgumentException("생년월일 형식 오류 (yyyy-MM-dd)");
    }

    private LocalTime parseTimeOrDefault(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() == CellType.BLANK) return LocalTime.MIDNIGHT;
        if (cell.getCellType() == CellType.NUMERIC) {
            int totalMinutes = (int) Math.round(cell.getNumericCellValue() * 24 * 60);
            return LocalTime.of(totalMinutes / 60 % 24, totalMinutes % 60);
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            return s.isEmpty() ? LocalTime.MIDNIGHT
                    : LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"));
        }
        return LocalTime.MIDNIGHT;
    }

    private Gender parseGender(Row row, int col) {
        String s = requireString(row, col, "성별").toUpperCase();
        try {
            return Gender.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("성별은 MALE/FEMALE/OTHER 중 하나여야 합니다.");
        }
    }

    // --- 유틸 ---

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String toContentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private String sanitizeName(String name) {
        return name == null ? "unknown" : name.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    // --- 내부 레코드 ---

    private record XlsxRow(String nameEn, String nationality, LocalDate birthDate,
                            LocalTime birthTime, String birthRegion, Gender gender,
                            String photoFileName) {}

    private record PreparedApplication(XlsxRow row, String photoId, String photoPath) {}
}
