package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

// 4-D: 학생증 카드 템플릿 업로드(front/back 각각)의 파일 검증 — MIME/시그니처 검증은
// EventImageValidator/BoardAttachmentValidator와 같은 패턴(매직바이트 확인)이지만, 이 프로젝트는
// "검증기는 도메인마다 독립"이 관례라(EventImageValidator 자체 주석 참고) 재사용하지 않고 새로 만든다.
// 카드 템플릿은 기존 3종 카드와 동일하게 PNG만 허용하고, 추가로 카드 비율에 맞는 해상도인지도 본다.
@Component
class SchoolCardTemplateValidator {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    // 235:156 ≈ 1.5064 — 다른 3종 카드가 이미 이 비율의 기준 캔버스를 쓴다(CardLayouts 참고).
    private static final double CARD_RATIO = 235.0 / 156.0;
    private static final double RATIO_TOLERANCE = 0.05;
    private static final int MIN_LONG_EDGE = 800;

    void validate(MultipartFile file, CardDesignOrientation orientation) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = file.getContentType();
        if (!"png".equals(extension) || !"image/png".equals(mimeType)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        byte[] bytes = bytesOf(file);
        if (!isPngSignature(bytes)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        BufferedImage image = decode(bytes);
        validateResolution(image, orientation);
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

    private boolean isPngSignature(byte[] bytes) {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < png.length) {
            return false;
        }
        for (int i = 0; i < png.length; i++) {
            if (bytes[i] != png[i]) {
                return false;
            }
        }
        return true;
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
            }
            return image;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    // 잠정값(TODO.md 4-D 참고, 실제 디자이너 산출물 기준 조정 가능) — LANDSCAPE는 가로가 더 길고
    // PORTRAIT는 세로가 더 길어야 하며, 그 비율이 카드 실물 비율(235:156)의 ±5% 안에 들어야 한다.
    private void validateResolution(BufferedImage image, CardDesignOrientation orientation) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean landscape = orientation == CardDesignOrientation.LANDSCAPE;
        int longEdge = landscape ? width : height;
        int shortEdge = landscape ? height : width;

        if (longEdge < shortEdge) {
            throw new CustomException(ErrorCode.CARD_TEMPLATE_INVALID_RESOLUTION);
        }
        if (longEdge < MIN_LONG_EDGE) {
            throw new CustomException(ErrorCode.CARD_TEMPLATE_INVALID_RESOLUTION);
        }
        double ratio = (double) longEdge / shortEdge;
        double lowerBound = CARD_RATIO * (1 - RATIO_TOLERANCE);
        double upperBound = CARD_RATIO * (1 + RATIO_TOLERANCE);
        if (ratio < lowerBound || ratio > upperBound) {
            throw new CustomException(ErrorCode.CARD_TEMPLATE_INVALID_RESOLUTION);
        }
    }
}
