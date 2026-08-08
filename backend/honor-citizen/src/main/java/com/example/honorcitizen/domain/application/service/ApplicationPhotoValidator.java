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

/**
 * 증명사진과 학교 자산(로고·직인) 이미지의 유효성을 검증하는 컴포넌트.
 *
 * [검증 단계 — 증명사진 기준 순서]
 * 1. 파일 존재 여부
 * 2. 파일 크기 (5MB 이하)
 * 3. 확장자 허용 목록 (jpg, jpeg, png)
 * 4. Content-Type(MIME) 허용 목록 (image/jpeg, image/png)
 * 5. 실제 바이너리 시그니처 — 확장자·MIME와 시그니처가 모두 일치해야 통과
 * 6. ImageIO 디코딩 — 손상된 파일 걸러냄
 * 7. 해상도 최소값 검증 (증명사진만 해당, EXIF orientation 반영)
 *
 * [학교 자산 vs 증명사진 차이]
 * 학교 로고·직인은 카드에 비율 축소해 삽입되므로 해상도 하한이 의미 없다.
 * validateSchoolAsset은 7번(해상도) 검증을 생략하고 나머지 단계는 동일하게 수행한다.
 *
 * [보안 관점]
 * 확장자와 Content-Type만으로는 부족하다. 클라이언트가 확장자를 바꾸거나
 * HTTP 요청의 Content-Type 헤더를 조작해 악성 파일을 이미지로 위장할 수 있다.
 * 5번 단계에서 파일 내용의 첫 바이트(magic number)로 실제 포맷을 판별해 이를 차단한다.
 *
 * [외부 라이브러리 미사용]
 * EXIF 파싱을 별도 라이브러리(예: metadata-extractor) 없이 직접 구현했다.
 * 이유: 이미지 방향 정보(orientation)만 필요하고 전체 메타데이터 파싱이 불필요해
 *       의존성을 추가하지 않는 편이 유지보수에 유리하다.
 */
@Component
class ApplicationPhotoValidator {

    // 5MB: 증명사진이 이 크기를 초과하면 압축되지 않은 RAW 이미지일 가능성이 높다.
    // 서버 메모리 부담과 S3 저장 비용을 고려해 5MB로 제한한다.
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    // 증명사진 최소 해상도 (px) — 카드 인쇄 품질 기준.
    // 300x400 미만이면 카드 출력 시 흐릿하게 인쇄된다.
    // 스마트폰 셀카는 보통 훨씬 높은 해상도이므로 이 기준을 통과하지 못하는 경우는 드물다.
    private static final int MIN_FACE_WIDTH = 300;
    private static final int MIN_FACE_HEIGHT = 400;

    // webp는 허용하지 않는다 — 카드 생성 모듈(DefaultCardImageGenerator)이 JPEG/PNG만 처리하기 때문이다.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png");

    /**
     * 증명사진을 검증한다.
     * 파일 크기·형식·바이너리 시그니처·최소 해상도(EXIF 방향 반영)를 모두 확인한다.
     */
    void validateFacePhoto(MultipartFile file) {
        validate(file, true);
    }

    /**
     * 학교 로고·직인 이미지를 검증한다.
     * 파일 크기·형식·바이너리 시그니처만 확인하고 해상도 하한은 검사하지 않는다.
     * 이유: 학교 공식 로고가 소형 PNG인 경우가 많아 해상도 기준 적용이 부적절하다.
     */
    void validateSchoolAsset(MultipartFile file) {
        validate(file, false);
    }

    private void validate(MultipartFile file, boolean validateMinimumResolution) {
        // 1단계: 파일 존재 여부
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 2단계: 파일 크기 — Content-Length 헤더가 아닌 실제 크기로 검증한다.
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = file.getContentType();

        // 3·4단계: 확장자와 Content-Type 허용 목록 검증
        // 두 조건을 AND로 체크하는 이유: 확장자는 jpg인데 MIME가 image/png인 경우처럼
        // 불일치 케이스를 차단한다. 일치하는지 여부는 5단계(시그니처)에서 다시 검증한다.
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        // 파일 내용을 메모리에 읽는다. 이후 단계에서 여러 번 사용한다.
        byte[] bytes = bytesOf(file);

        // 5단계: 바이너리 시그니처 검증 — magic number로 실제 파일 포맷을 판별한다.
        // 예: PNG 파일의 확장자를 .jpg로 바꿔 올리면 이 단계에서 탐지된다.
        ImageFormat signatureFormat = detectSignature(bytes);
        if (!signatureFormat.matches(extension, mimeType)) {
            // 확장자·MIME·시그니처 세 가지가 모두 일치하지 않으면 위변조로 간주한다.
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }

        // 6단계: 실제 디코딩 — 확장자와 시그니처는 맞지만 내용이 손상된 파일을 걸러낸다.
        BufferedImage image = decode(bytes);

        if (validateMinimumResolution) {
            // 7단계: 해상도 최소값 검증 (증명사진 전용)
            //
            // [EXIF orientation 처리가 필요한 이유]
            // 스마트폰 카메라는 가로로 찍어도 EXIF orientation 태그로 "세로로 보이게" 표시한다.
            // ImageIO.read()는 orientation을 무시하고 픽셀 배열 그대로 읽으므로
            // 실제 세로 사진이 가로로 반환될 수 있다.
            //
            // EXIF orientation 값 의미:
            //   1: 정방향 (변환 없음)
            //   3: 180도 회전
            //   6: 시계방향 90도 회전 (이 때 실제 width·height가 뒤바뀜)
            //   8: 반시계방향 90도 회전 (이 때 실제 width·height가 뒤바뀜)
            //   5, 7: 반전 + 90도 회전 (이 때도 뒤바뀜)
            // orientation 5~8은 가로·세로가 서로 뒤바뀐 경우이므로 swap 후 검사한다.
            //
            // PNG는 EXIF를 가지지 않으므로 orientation=1(기본값)로 처리한다.
            int orientation = signatureFormat == ImageFormat.JPEG ? readExifOrientation(bytes) : 1;
            int width = image.getWidth();
            int height = image.getHeight();
            if (orientation >= 5 && orientation <= 8) {
                // 가로·세로 교환: getWidth()가 실제로는 height를 반환하고 있는 상태이므로 swap한다.
                int swapped = width;
                width = height;
                height = swapped;
            }
            if (width < MIN_FACE_WIDTH || height < MIN_FACE_HEIGHT) {
                throw new CustomException(ErrorCode.INVALID_IMAGE);
            }
        }
    }

    // 파일명에서 마지막 '.' 이후를 소문자로 추출한다. 점이 없으면 빈 문자열을 반환한다.
    // Locale.ROOT를 쓰는 이유: 터키어 같은 일부 로케일에서 'I'의 소문자가 'ı'(점 없는 i)가 돼
    //   "JPG".toLowerCase()가 "jpg"가 아닌 "jpg"와 다른 문자가 될 수 있어 비교 오류가 생긴다.
    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    // MultipartFile에서 바이트 배열을 읽는다. 읽기 실패는 손상된 파일로 간주한다.
    private byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    /**
     * 파일 앞부분의 바이트(magic number)로 실제 이미지 포맷을 판별한다.
     *
     * JPEG magic number: FF D8 FF (3바이트)
     *   JPEG 파일은 항상 이 3바이트로 시작한다. 이후 바이트는 JFIF/EXIF 마커 등이다.
     *
     * PNG magic number: 89 50 4E 47 0D 0A 1A 0A (8바이트)
     *   89: PNG 파일을 7비트 ASCII 인식 도구가 잘못 처리하는 것을 방지
     *   50 4E 47: "PNG" ASCII
     *   0D 0A: CRLF (Windows 줄바꿈)
     *   1A: Ctrl-Z (DOS EOF)
     *   0A: LF (Unix 줄바꿈)
     *   이 8바이트 조합은 전송 과정에서 줄바꿈 변환으로 파일이 손상됐는지도 감지한다.
     */
    private ImageFormat detectSignature(byte[] bytes) {
        // JPEG 시그니처: FF D8 FF
        if (bytes.length >= 3 && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF) {
            return ImageFormat.JPEG;
        }
        // PNG 시그니처: 89 50 4E 47 0D 0A 1A 0A
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

    /**
     * ImageIO로 이미지를 실제 디코딩한다.
     *
     * null 반환: ImageIO가 알 수 없는 포맷이거나 손상된 경우 예외 없이 null을 반환할 수 있다.
     * null 체크가 필요한 이유: IOException이 발생하지 않아도 decode 실패 케이스가 있기 때문이다.
     *
     * 이 단계까지 통과하면 실제로 렌더링 가능한 유효한 이미지임이 보장된다.
     */
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

    /**
     * JPEG 파일에서 EXIF orientation 태그(0x0112) 값을 파싱한다.
     * 파싱에 실패하거나 EXIF가 없으면 기본값 1(정방향)을 반환한다.
     *
     * [JPEG 파일 구조]
     * JPEG는 마커(2바이트) + 세그먼트 길이(2바이트) + 데이터 구조로 이루어진다.
     * SOI(Start of Image): FF D8 — 파일 시작
     * APP1(0xE1): EXIF 데이터가 담기는 세그먼트. "Exif\0\0" 헤더로 식별한다.
     * SOS(0xDA): 실제 이미지 스캔 데이터 시작. 이 이후엔 EXIF가 없다.
     * EOI(0xD9): 파일 끝.
     *
     * [파싱 방식]
     * SOI(2바이트)를 건너뛴 후 APP1 세그먼트를 찾을 때까지 순회한다.
     * APP1을 찾으면 TIFF 구조의 IFD(Image File Directory)에서 0x0112 태그를 조회한다.
     */
    private int readExifOrientation(byte[] bytes) {
        int offset = 2; // SOI(FF D8) 2바이트를 건너뛰고 첫 번째 마커부터 시작
        while (offset + 4 <= bytes.length) {
            // 마커는 항상 FF로 시작해야 한다. 아니면 파일 구조가 잘못된 것이다.
            if (unsigned(bytes[offset]) != 0xFF) {
                break;
            }
            int marker = unsigned(bytes[offset + 1]);
            offset += 2; // 마커 2바이트 소비

            // SOS(Start of Scan, 0xDA) 또는 EOI(End of Image, 0xD9)에 도달하면
            // 이후에는 EXIF 세그먼트가 없으므로 파싱을 중단한다.
            if (marker == 0xDA || marker == 0xD9) {
                break;
            }
            if (offset + 2 > bytes.length) {
                break;
            }

            // 세그먼트 길이는 빅엔디언(JPEG 표준)으로 읽는다. 길이 값에는 자신(2바이트)도 포함된다.
            int segmentLength = readUnsignedShort(bytes, offset, false);
            if (segmentLength < 2 || offset + segmentLength > bytes.length) {
                break;
            }

            // APP1(0xE1) 세그먼트이고 "Exif\0\0" 시그니처가 있으면 TIFF 파싱을 수행한다.
            // offset+2는 세그먼트 길이 2바이트 다음, offset+8은 TIFF 헤더 시작점이다.
            if (marker == 0xE1 && segmentLength >= 10 && isExif(bytes, offset + 2)) {
                return parseTiffOrientation(bytes, offset + 8, offset + segmentLength);
            }
            offset += segmentLength; // 세그먼트 전체를 건너뛰고 다음 마커로 이동
        }
        return 1; // EXIF 없음 또는 파싱 실패 → 기본값(정방향)
    }

    // APP1 세그먼트 데이터가 "Exif\0\0" 으로 시작하는지 확인한다.
    // 이 6바이트 식별자가 있어야 EXIF 데이터를 담은 APP1임을 확신할 수 있다.
    private boolean isExif(byte[] bytes, int offset) {
        return offset + 6 <= bytes.length && bytes[offset] == 'E' && bytes[offset + 1] == 'x'
                && bytes[offset + 2] == 'i' && bytes[offset + 3] == 'f'
                && bytes[offset + 4] == 0 && bytes[offset + 5] == 0;
    }

    /**
     * TIFF IFD(Image File Directory)에서 orientation 태그(0x0112)를 파싱한다.
     *
     * [TIFF 구조]
     * TIFF 헤더:
     *   - 바이트 순서: "II"(리틀엔디언, Intel) 또는 "MM"(빅엔디언, Motorola)
     *   - 매직 넘버: 42 (항상 이 값)
     *   - IFD 오프셋: 헤더 시작 기준의 첫 번째 IFD 위치
     *
     * IFD:
     *   - 엔트리 수(2바이트) + 엔트리 배열
     *   - 각 엔트리: 태그(2) + 타입(2) + 카운트(4) + 값/오프셋(4) = 12바이트 고정
     *
     * 0x0112 태그: TIFF Orientation 태그 번호
     *   값 1~8이 유효하며 각각 회전/반전 방향을 나타낸다.
     *
     * @param tiff TIFF 헤더 시작 오프셋 (bytes 배열 내 절대 위치)
     * @param end  이 EXIF 세그먼트의 끝 오프셋 (경계 초과 방지용)
     */
    private int parseTiffOrientation(byte[] bytes, int tiff, int end) {
        if (tiff + 8 > end) {
            return 1; // TIFF 헤더를 읽기에 충분한 바이트가 없음
        }
        boolean littleEndian;
        // "II" = Intel = 리틀엔디언, "MM" = Motorola = 빅엔디언
        if (bytes[tiff] == 'I' && bytes[tiff + 1] == 'I') {
            littleEndian = true;
        } else if (bytes[tiff] == 'M' && bytes[tiff + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1; // 알 수 없는 바이트 순서
        }

        // IFD 오프셋은 TIFF 헤더 시작(tiff)으로부터의 상대 오프셋이다.
        int ifdOffset = readInt(bytes, tiff + 4, littleEndian);
        int ifd = tiff + ifdOffset;
        if (ifd < tiff || ifd + 2 > end) {
            return 1; // IFD 위치가 세그먼트 범위를 벗어남
        }

        int count = readUnsignedShort(bytes, ifd, littleEndian); // IFD 엔트리 수
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12; // 각 엔트리는 12바이트 고정
            if (entry + 12 > end) {
                return 1; // 엔트리가 세그먼트 범위를 벗어남
            }
            // 태그 번호가 0x0112(Orientation)인 엔트리를 찾는다.
            if (readUnsignedShort(bytes, entry, littleEndian) == 0x0112) {
                // 값은 엔트리 offset+8 위치에 있다 (태그2 + 타입2 + 카운트4 = 8바이트 이후)
                int orientation = readUnsignedShort(bytes, entry + 8, littleEndian);
                return orientation >= 1 && orientation <= 8 ? orientation : 1;
            }
        }
        return 1; // orientation 태그를 찾지 못함
    }

    // 바이트 순서(엔디언)에 따라 2바이트를 unsigned short로 읽는다.
    private int readUnsignedShort(byte[] bytes, int offset, boolean littleEndian) {
        return littleEndian
                ? unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8
                : unsigned(bytes[offset]) << 8 | unsigned(bytes[offset + 1]);
    }

    // 바이트 순서(엔디언)에 따라 4바이트를 int로 읽는다.
    private int readInt(byte[] bytes, int offset, boolean littleEndian) {
        return littleEndian
                ? unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8
                        | unsigned(bytes[offset + 2]) << 16 | unsigned(bytes[offset + 3]) << 24
                : unsigned(bytes[offset]) << 24 | unsigned(bytes[offset + 1]) << 16
                        | unsigned(bytes[offset + 2]) << 8 | unsigned(bytes[offset + 3]);
    }

    // Java byte는 signed(-128~127)이므로 비트 AND로 unsigned 값(0~255)으로 변환한다.
    // 예: (byte)0xFF = -1 이지만 unsigned(0xFF) = 255
    private int unsigned(byte value) {
        return value & 0xFF;
    }

    /**
     * 파일 형식(바이너리 시그니처)을 나타내는 내부 열거형.
     *
     * matches() 메서드는 시그니처로 감지한 포맷이 확장자·MIME와 모두 일치하는지 검사한다.
     * 세 가지 모두 일치해야 PASS:
     *   - 시그니처: 파일의 실제 내용
     *   - 확장자: 파일명의 의도
     *   - MIME: HTTP 요청의 선언
     * 셋 중 하나라도 다르면 위변조 가능성이 있어 거부한다.
     */
    private enum ImageFormat {
        JPEG, PNG, UNKNOWN;

        boolean matches(String extension, String mimeType) {
            return switch (this) {
                // JPEG: 확장자는 jpg 또는 jpeg 모두 허용 (동일 포맷의 두 가지 확장자)
                case JPEG -> (extension.equals("jpg") || extension.equals("jpeg")) && mimeType.equals("image/jpeg");
                case PNG -> extension.equals("png") && mimeType.equals("image/png");
                case UNKNOWN -> false; // 알 수 없는 포맷은 항상 거부
            };
        }
    }
}
