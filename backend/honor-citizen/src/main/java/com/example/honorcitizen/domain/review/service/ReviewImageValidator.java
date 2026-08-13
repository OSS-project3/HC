package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

// 후기 첨부 이미지 검증기. ApplicationPhotoValidator(5 MiB, jpg/jpeg/png, 해상도 하한 있음)와
// 기준이 달라 재사용하지 않는다 — 2 MiB, webp 포함, 해상도 하한 없음(docs/specs/review/data-model.md §4).
// 파일이 없는 경우(사진은 선택)는 호출측에서 아예 이 검증기를 호출하지 않는다.
@Component
class ReviewImageValidator {

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = file.getContentType();

        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        byte[] bytes = bytesOf(file);

        ImageFormat signatureFormat = detectSignature(bytes);
        if (!signatureFormat.matches(extension, mimeType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        decode(bytes);
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private ImageFormat detectSignature(byte[] bytes) {
        // JPEG: FF D8 FF
        if (bytes.length >= 3 && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF) {
            return ImageFormat.JPEG;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length >= png.length && startsWith(bytes, png)) {
            return ImageFormat.PNG;
        }
        // WEBP: "RIFF" + 4바이트 파일크기(검증 대상 아님) + "WEBP"
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ImageFormat.WEBP;
        }
        return ImageFormat.UNKNOWN;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private enum ImageFormat {
        JPEG, PNG, WEBP, UNKNOWN;

        boolean matches(String extension, String mimeType) {
            return switch (this) {
                case JPEG -> (extension.equals("jpg") || extension.equals("jpeg")) && mimeType.equals("image/jpeg");
                case PNG -> extension.equals("png") && mimeType.equals("image/png");
                case WEBP -> extension.equals("webp") && mimeType.equals("image/webp");
                case UNKNOWN -> false;
            };
        }
    }
}
