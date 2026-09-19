package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationPhotoValidatorTest {

    private final ApplicationPhotoValidator validator = new ApplicationPhotoValidator();

    @Test
    void acceptsFacePhotoAtMinimumResolution() {
        assertThatCode(() -> validator.validateFacePhoto(image("face.jpg", "image/jpeg", 300, 400, "jpg")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsFileLargerThanFiveMebibytes() {
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("photo", "face.jpg", "image/jpeg", bytes);

        assertError(() -> validator.validateFacePhoto(file), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedExtensionOrMime() {
        assertError(() -> validator.validateFacePhoto(image("face.webp", "image/webp", 300, 400, "png")),
                ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsSignatureThatDoesNotMatchExtensionAndMime() {
        assertError(() -> validator.validateFacePhoto(image("face.jpg", "image/jpeg", 300, 400, "png")),
                ErrorCode.INVALID_IMAGE);
    }

    @Test
    void rejectsUndecodableImage() {
        MockMultipartFile file = new MockMultipartFile("photo", "face.jpg", "image/jpeg", "not-image".getBytes());
        assertError(() -> validator.validateFacePhoto(file), ErrorCode.INVALID_IMAGE);
    }

    @Test
    void rejectsFacePhotoBelowMinimumResolution() {
        assertError(() -> validator.validateFacePhoto(image("face.png", "image/png", 299, 400, "png")),
                ErrorCode.INVALID_IMAGE);
    }

    @Test
    void appliesExifOrientationBeforeCheckingResolution() {
        byte[] landscapeJpeg = imageBytes(400, 300, "jpg");
        byte[] oriented = withExifOrientation(landscapeJpeg, 6);
        MockMultipartFile file = new MockMultipartFile("photo", "face.jpg", "image/jpeg", oriented);

        assertThatCode(() -> validator.validateFacePhoto(file)).doesNotThrowAnyException();
    }

    @Test
    void schoolAssetDoesNotRequireMinimumResolution() {
        assertThatCode(() -> validator.validateSchoolAsset(image("schoolLogo.png", "image/png", 50, 50, "png")))
                .doesNotThrowAnyException();
    }

    // 단체(ZIP) 경로 — MultipartFile이 아니라 byte[]+파일명으로 들어오는 사진 검증(2026-09-19).
    // MultipartFile 경로와 동일한 magic number/디코딩/해상도 검증을 거친다. MIME은 HTTP 헤더가
    // 없으므로 확장자에서 유도한다 — 그래도 시그니처 대조는 그대로 동작한다(예: png를 .jpg로
    // 위장하면 유도 MIME=image/jpeg와 실제 시그니처 PNG가 불일치해 그대로 걸러진다).
    @Test
    void acceptsFacePhotoBytesAtMinimumResolution() {
        assertThatCode(() -> validator.validateFacePhotoBytes(imageBytes(300, 400, "jpg"), "1.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsFacePhotoBytesLargerThanFiveMebibytes() {
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];
        assertError(() -> validator.validateFacePhotoBytes(bytes, "1.jpg"), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsFacePhotoBytesWithUnsupportedExtension() {
        assertError(() -> validator.validateFacePhotoBytes(imageBytes(300, 400, "png"), "1.webp"),
                ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsFacePhotoBytesWithSignatureNotMatchingExtension() {
        assertError(() -> validator.validateFacePhotoBytes(imageBytes(300, 400, "png"), "1.jpg"),
                ErrorCode.INVALID_IMAGE);
    }

    @Test
    void rejectsUndecodableFacePhotoBytes() {
        assertError(() -> validator.validateFacePhotoBytes("not-image".getBytes(), "1.jpg"),
                ErrorCode.INVALID_IMAGE);
    }

    @Test
    void rejectsFacePhotoBytesBelowMinimumResolution() {
        assertError(() -> validator.validateFacePhotoBytes(imageBytes(299, 400, "png"), "1.png"),
                ErrorCode.INVALID_IMAGE);
    }

    private void assertError(ThrowingCall call, ErrorCode errorCode) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private MockMultipartFile image(String filename, String mime, int width, int height, String format) {
        return new MockMultipartFile("file", filename, mime, imageBytes(width, height, format));
    }

    private byte[] imageBytes(int width, int height, String format) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] exif = new byte[] {
                (byte) 0xFF, (byte) 0xE1, 0, 34,
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0, 42, 0, 0, 0, 8,
                0, 1,
                1, 18, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                0, 0, 0, 0
        };
        byte[] result = new byte[jpeg.length + exif.length];
        System.arraycopy(jpeg, 0, result, 0, 2);
        System.arraycopy(exif, 0, result, 2, exif.length);
        System.arraycopy(jpeg, 2, result, 2 + exif.length, jpeg.length - 2);
        return result;
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}