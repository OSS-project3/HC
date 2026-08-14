package com.example.honorcitizen.domain.board.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardAttachmentValidatorTest {

    private final BoardAttachmentValidator validator = new BoardAttachmentValidator();

    @Test
    void acceptsPdfDocument() {
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "notice.pdf", "application/pdf", "pdf-bytes".getBytes());

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void acceptsHwpDocumentEvenWithOctetStreamMime() {
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "notice.hwp", "application/octet-stream", "hwp-bytes".getBytes());

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void acceptsValidJpegImage() {
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "photo.jpg", "image/jpeg", jpegBytes());

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void rejectsFileLargerThanTenMebibytes() {
        byte[] bytes = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "big.pdf", "application/pdf", bytes);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "script.exe", "application/octet-stream", "exe-bytes".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsImageWithMismatchedSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "fake.jpg", "image/jpeg", "not-an-image".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE_FILE);
    }

    private byte[] jpegBytes() {
        try {
            BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
