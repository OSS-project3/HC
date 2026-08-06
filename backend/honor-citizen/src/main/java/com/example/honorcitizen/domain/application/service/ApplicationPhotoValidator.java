package com.example.honorcitizen.domain.application.service;

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

@Component
class ApplicationPhotoValidator {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MIN_FACE_WIDTH = 300;
    private static final int MIN_FACE_HEIGHT = 400;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png");

    void validateFacePhoto(MultipartFile file) {
        validate(file, true);
    }

    void validateSchoolAsset(MultipartFile file) {
        validate(file, false);
    }

    private void validate(MultipartFile file, boolean validateMinimumResolution) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
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
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }

        BufferedImage image = decode(bytes);
        if (validateMinimumResolution) {
            int orientation = signatureFormat == ImageFormat.JPEG ? readExifOrientation(bytes) : 1;
            int width = image.getWidth();
            int height = image.getHeight();
            if (orientation >= 5 && orientation <= 8) {
                int swapped = width;
                width = height;
                height = swapped;
            }
            if (width < MIN_FACE_WIDTH || height < MIN_FACE_HEIGHT) {
                throw new CustomException(ErrorCode.INVALID_IMAGE);
            }
        }
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
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    private ImageFormat detectSignature(byte[] bytes) {
        if (bytes.length >= 3 && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF) {
            return ImageFormat.JPEG;
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length >= png.length) {
            for (int i = 0; i < png.length; i++) {
                if (bytes[i] != png[i]) {
                    return ImageFormat.UNKNOWN;
                }
            }
            return ImageFormat.PNG;
        }
        return ImageFormat.UNKNOWN;
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new CustomException(ErrorCode.INVALID_IMAGE);
            }
            return image;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    private int readExifOrientation(byte[] bytes) {
        int offset = 2;
        while (offset + 4 <= bytes.length) {
            if (unsigned(bytes[offset]) != 0xFF) {
                break;
            }
            int marker = unsigned(bytes[offset + 1]);
            offset += 2;
            if (marker == 0xDA || marker == 0xD9) {
                break;
            }
            if (offset + 2 > bytes.length) {
                break;
            }
            int segmentLength = readUnsignedShort(bytes, offset, false);
            if (segmentLength < 2 || offset + segmentLength > bytes.length) {
                break;
            }
            if (marker == 0xE1 && segmentLength >= 10 && isExif(bytes, offset + 2)) {
                return parseTiffOrientation(bytes, offset + 8, offset + segmentLength);
            }
            offset += segmentLength;
        }
        return 1;
    }

    private boolean isExif(byte[] bytes, int offset) {
        return offset + 6 <= bytes.length && bytes[offset] == 'E' && bytes[offset + 1] == 'x'
                && bytes[offset + 2] == 'i' && bytes[offset + 3] == 'f'
                && bytes[offset + 4] == 0 && bytes[offset + 5] == 0;
    }

    private int parseTiffOrientation(byte[] bytes, int tiff, int end) {
        if (tiff + 8 > end) {
            return 1;
        }
        boolean littleEndian;
        if (bytes[tiff] == 'I' && bytes[tiff + 1] == 'I') {
            littleEndian = true;
        } else if (bytes[tiff] == 'M' && bytes[tiff + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1;
        }
        int ifdOffset = readInt(bytes, tiff + 4, littleEndian);
        int ifd = tiff + ifdOffset;
        if (ifd < tiff || ifd + 2 > end) {
            return 1;
        }
        int count = readUnsignedShort(bytes, ifd, littleEndian);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (readUnsignedShort(bytes, entry, littleEndian) == 0x0112) {
                int orientation = readUnsignedShort(bytes, entry + 8, littleEndian);
                return orientation >= 1 && orientation <= 8 ? orientation : 1;
            }
        }
        return 1;
    }

    private int readUnsignedShort(byte[] bytes, int offset, boolean littleEndian) {
        return littleEndian
                ? unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8
                : unsigned(bytes[offset]) << 8 | unsigned(bytes[offset + 1]);
    }

    private int readInt(byte[] bytes, int offset, boolean littleEndian) {
        return littleEndian
                ? unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8
                        | unsigned(bytes[offset + 2]) << 16 | unsigned(bytes[offset + 3]) << 24
                : unsigned(bytes[offset]) << 24 | unsigned(bytes[offset + 1]) << 16
                        | unsigned(bytes[offset + 2]) << 8 | unsigned(bytes[offset + 3]);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private enum ImageFormat {
        JPEG, PNG, UNKNOWN;

        boolean matches(String extension, String mimeType) {
            return switch (this) {
                case JPEG -> (extension.equals("jpg") || extension.equals("jpeg")) && mimeType.equals("image/jpeg");
                case PNG -> extension.equals("png") && mimeType.equals("image/png");
                case UNKNOWN -> false;
            };
        }
    }
}
