package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewImageValidatorTest {

    // 10x10 단색 손실(lossy) webp — Pillow(libwebp)로 생성한 실제 유효 파일 바이너리.
    private static final byte[] WEBP_10X10_RED = {
            82, 73, 70, 70, 60, 0, 0, 0, 87, 69, 66, 80, 86, 80, 56, 32, 48, 0, 0, 0, -48, 1, 0, -99, 1, 42, 10, 0,
            10, 0, 1, 64, 38, 37, -96, 2, 116, -70, 1, -8, 0, 3, -80, 0, -2, -14, -21, 127, -4, -40, 21, -51, 115,
            -17, -9, -1, -46, -32, -3, 46, 15, -46, -32, -1, -46, -112, 0, 0
    };

    private final ReviewImageValidator validator = new ReviewImageValidator();

    @Test
    void acceptsJpeg() {
        assertThatCode(() -> validator.validate(image("photo.jpg", "image/jpeg", "jpg")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsPng() {
        assertThatCode(() -> validator.validate(image("photo.png", "image/png", "png")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsWebp() {
        // build.gradle에 twelvemonkeys imageio-webp를 추가한 이유 — ImageIO 표준만으로는 webp를 디코딩할 수 없다.
        // ImageIO에는 webp 인코더(writer)가 없어(twelvemonkeys도 reader만 제공) 실제 webp 바이너리를 그대로 fixture로 사용한다.
        MockMultipartFile file = new MockMultipartFile("image", "photo.webp", "image/webp", WEBP_10X10_RED);

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void rejectsFileLargerThanTwoMebibytes() {
        byte[] bytes = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", bytes);

        assertError(() -> validator.validate(file), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertError(() -> validator.validate(image("photo.gif", "image/gif", "png")), ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsSignatureThatDoesNotMatchExtensionAndMime() {
        assertError(() -> validator.validate(image("photo.jpg", "image/jpeg", "png")), ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void rejectsUndecodableImage() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "not-image".getBytes());

        assertError(() -> validator.validate(file), ErrorCode.INVALID_IMAGE_FILE);
    }

    private void assertError(ThrowingCall call, ErrorCode errorCode) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private MockMultipartFile image(String filename, String mime, String format) {
        return new MockMultipartFile("image", filename, mime, imageBytes(format));
    }

    private byte[] imageBytes(String format) {
        try {
            BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}
