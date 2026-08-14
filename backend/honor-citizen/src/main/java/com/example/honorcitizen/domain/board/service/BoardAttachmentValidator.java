package com.example.honorcitizen.domain.board.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

// 공지사항 첨부파일(NOTICE 전용) 검증기 — data-model.md §5. ApplicationPhotoValidator/ReviewImageValidator와
// 달리 이미지가 아니라 일반 문서(pdf/hwp/docx/xlsx 등)가 주 대상이라 별도 컴포넌트로 분리한다.
// 검증 단계: 파일 크기 → 확장자/MIME 허용목록 → (이미지 확장자인 경우만) 바이너리 시그니처.
// 문서 포맷(pdf/hwp/office 계열)은 이미지처럼 표준화된 magic number 판별이 마땅치 않아
// 시그니처 검증을 이미지에 한해서만 적용한다 — data-model.md §5.1에 명시된 그대로.
@Component
class BoardAttachmentValidator {

    static final int MAX_ATTACHMENT_COUNT = 10;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "hwp", "hwpx", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png");

    // hwp는 브라우저마다 표준 MIME이 없어 application/octet-stream으로 오는 경우가 흔하다 —
    // 확장자 허용목록과 함께 걸어야 하는 검증이라 여기서는 실용적으로 허용한다.
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/x-hwp", "application/haansofthwp", "application/vnd.hancom.hwp", "application/octet-stream",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "image/jpeg", "image/png");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        if (IMAGE_EXTENSIONS.contains(extension)) {
            validateImageSignature(file, extension, mimeType);
        }
    }

    private void validateImageSignature(MultipartFile file, String extension, String mimeType) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        boolean isJpeg = bytes.length >= 3 && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF;
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        boolean isPng = bytes.length >= pngSignature.length && startsWith(bytes, pngSignature);

        boolean matches = (isJpeg && (extension.equals("jpg") || extension.equals("jpeg")) && mimeType.equals("image/jpeg"))
                || (isPng && extension.equals("png") && mimeType.equals("image/png"));
        if (!matches) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
