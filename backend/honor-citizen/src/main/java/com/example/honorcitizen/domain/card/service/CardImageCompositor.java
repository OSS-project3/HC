package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;

/**
 * 신청서 확정 정보를 카드 템플릿(디자이너 제공 시안, `resources/card-templates/`)에 좌표 기반으로
 * 합성해 카드 앞면 이미지를 만든다(DESIGN.md/card.md가 "미구현"이라고 적어뒀던 CardFieldDefinition).
 *
 * 직인·발행처 필드는 실제 지자체 관인을 무단으로 인쇄하게 되는 문제가 있어 이번 범위에서 제외했다
 * (TODO.md "카드 이미지 합성" 섹션 정책 참고).
 *
 * [좌표계] {@link CardLayouts}의 오프셋은 위치값.jpg 표를 그대로 옮긴 값 — 기준 캔버스(카드종류별
 * baseWidth×baseHeight) 중심(0,0)으로부터의 거리다. 실제 템플릿 PNG는 해상도가 더 커서
 * (실제크기/기준크기) 배율을 곱해 변환한다. 이 해석은 명예한국인증 디자인 1을 실제 렌더링해
 * `시안_최종.jpg`와 육안 대조로 검증했다(TODO.md 참고, 분석적으로 유도한 값이 아니다).
 *
 * [팔레트 PNG 색상 버그] 템플릿 다수가 인덱스 컬러 PNG인데 {@code ImageIO.read()}로 읽으면
 * 색이 깨진다(빨강/파랑 반전 도트 패턴). {@link Toolkit} 경로로 우회해서 읽어야 한다 — 이 클래스의
 * 모든 이미지 로딩은 반드시 {@link #loadImage}를 거친다.
 */
@Component
class CardImageCompositor {

    private static final String TEMPLATE_ROOT = "card-templates/";
    private static final DateTimeFormatter ISSUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // 앞·뒷면 배경/사진/로고/직인 자리 파일명이 디자인마다 다르게 export되어 있어(디자이너 파일명
    // 비표준) 후보를 순서대로 시도한다. TODO.md "카드 이미지 합성"/2-B 섹션의 파일명 불일치 조사 결과.
    private static final List<String> FRONT_CANDIDATES = List.of("앞면.png", "대지 1.png");
    private static final List<String> BACK_CANDIDATES = List.of("뒷면.png", "대지 1 사본.png");
    private static final List<String> PHOTO_CANDIDATES = List.of("사진.png", "아트보드 2.png");
    private static final List<String> LOGO_CANDIDATES = List.of("발행처로고.png", "로고.png", "발행처 로고.png");
    private static final List<String> SEAL_CANDIDATES = List.of("직인.png", "아트보드 6.png");
    // 띠 아이콘은 디자인별 슬롯 에셋이 없어(HONOR_CITIZEN/1의 캐릭터.png가 유일한 참고용) 슬롯 크기에
    // coverFit하지 않고, 기준 캔버스 스케일로 이 논리 너비(pt)만큼 그린다 — 시안 목업 비율을 육안 참고.
    private static final double ZODIAC_BASE_WIDTH = 9d;

    private final Font dotumMedium;
    private final Font dotumBold;
    private final Font batangBold;
    private final ConcurrentHashMap<String, BufferedImage> imageCache = new ConcurrentHashMap<>();

    CardImageCompositor() {
        this.dotumMedium = loadFont("KoPub Dotum_Pro Medium.otf");
        this.dotumBold = loadFont("KoPub Dotum_Pro Bold.otf");
        this.batangBold = loadFont("KoPub Batang_Pro Bold.otf");
    }

    byte[] composeFront(CardTypeCode cardType, int design, CardMemberData data) {
        CardLayout layout = CardLayouts.FRONT.get(cardType);
        if (layout == null || isUnverifiedDesign(cardType, design)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String dir = TEMPLATE_ROOT + cardType.name() + "/" + design + "/";
        BufferedImage bg = copy(resolveImage(dir, FRONT_CANDIDATES));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            drawTitle(g, cardType, layout, scaleX, scaleY);
            drawPhoto(g, dir, data.photo(), layout, scaleX, scaleY);
            drawText(g, data.fullName(), dotumBold, 8f, Color.BLACK, layout.name(), layout, scaleX, scaleY);
            // HONOR_CITIZEN/HONOR_KOREAN 위치값 표는 영문명/카드번호/주소를 각자 다른 x에서 중앙
            // 정렬하는 걸 전제로 한 듯한데, 문자열 길이가 서로 달라(특히 주소) 실제 렌더링해보면
            // 왼쪽 시작점이 들쭉날쭉했다(사용자 확인, 두 카드종류 모두 동일 원칙 적용 요청). 이름의
            // 왼쪽 끝을 기준선으로 계산해 그 지점에 왼쪽 정렬한다. 각 필드 자신의 y좌표·폰트 크기는
            // 그대로 쓴다. VISITOR는 요청 범위 밖이라 기존 중앙 정렬 유지.
            if (cardType == CardTypeCode.HONOR_CITIZEN || cardType == CardTypeCode.HONOR_KOREAN) {
                double nameLeftEdge = leftEdgeX(data.fullName(), dotumBold, 8f, layout.name(), layout, scaleX);
                drawTextAtPixelX(g, data.englishName(), dotumMedium, 5f, Color.DARK_GRAY, nameLeftEdge,
                        layout.englishName(), layout, scaleX, scaleY);
                drawTextAtPixelX(g, data.cardNumber(), dotumMedium, 5f, Color.DARK_GRAY, nameLeftEdge,
                        layout.cardNumber(), layout, scaleX, scaleY);
                drawTextAtPixelX(g, data.address(), dotumMedium, 4.5f, Color.DARK_GRAY, nameLeftEdge,
                        layout.address(), layout, scaleX, scaleY);
            } else {
                drawText(g, data.englishName(), dotumMedium, 5f, Color.DARK_GRAY, layout.englishName(), layout, scaleX, scaleY);
                drawText(g, data.cardNumber(), dotumMedium, 5f, Color.DARK_GRAY, layout.cardNumber(), layout, scaleX, scaleY);
                drawText(g, data.address(), dotumMedium, 4.5f, Color.DARK_GRAY, layout.address(), layout, scaleX, scaleY);
            }
            // 발급일자는 HONOR_CITIZEN만 왼쪽 열에 속해(x=-76.7) 이름 기준선에 맞추고, HONOR_KOREAN은
            // 카드 우측(x=+79.96)에 별도로 배치되는 디자인이라 중앙 정렬을 그대로 둔다(요청 범위에
            // 발급일자가 포함되지 않았고, 실제 렌더링해보면 우측 배치가 의도된 것으로 보임).
            if (cardType == CardTypeCode.HONOR_CITIZEN) {
                double nameLeftEdge = leftEdgeX(data.fullName(), dotumBold, 8f, layout.name(), layout, scaleX);
                drawTextAtPixelX(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumMedium, 4.5f, Color.DARK_GRAY,
                        nameLeftEdge, layout.issueDate(), layout, scaleX, scaleY);
            } else {
                drawText(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumMedium, 4.5f, Color.DARK_GRAY,
                        layout.issueDate(), layout, scaleX, scaleY);
            }
            drawZodiac(g, data.zodiacBranch(), layout.zodiac(), layout, scaleX, scaleY);
            drawSlotImage(g, dir, LOGO_CANDIDATES, data.logo(), layout.issuerLogo(), layout, scaleX, scaleY);
            drawSlotImage(g, dir, SEAL_CANDIDATES, data.seal(), layout.seal(), layout, scaleX, scaleY);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 뒷면(한국이름풀이) — 배경 위에 이름·(있으면)한자·영문명·(있으면)한자뜻음·풀이를 얹는다.
    // 배경 자체(뒷면.png)는 신청자 정보와 무관한 디자인 고정 그래픽이라 필드가 없다.
    byte[] composeBack(CardTypeCode cardType, int design, CardMemberData data) {
        CardBackLayout layout = CardLayouts.BACK.get(cardType);
        if (layout == null || isUnverifiedDesign(cardType, design)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String dir = TEMPLATE_ROOT + cardType.name() + "/" + design + "/";
        BufferedImage bg = copy(resolveImage(dir, BACK_CANDIDATES));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();
        CardBackVariant variant = data.hasHanja() ? layout.hanjaVariant() : layout.noHanjaVariant();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            drawBackText(g, titleFallbackBackText(), batangBold, 8f, Color.BLACK,
                    layout.title(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            drawBackText(g, data.fullName(), dotumBold, 7f, Color.BLACK,
                    variant.name(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            drawBackText(g, data.englishName(), dotumMedium, 4.5f, Color.DARK_GRAY,
                    variant.englishName(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            if (data.hasHanja()) {
                drawBackText(g, "(" + data.chineseName() + ")", batangBold, 5.5f, Color.DARK_GRAY,
                        variant.hanja(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
                drawBackText(g, data.nameMeaning(), dotumMedium, 4f, Color.DARK_GRAY,
                        variant.hanjaMeaning(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            }
            drawBackText(g, data.nameInterpretation(), dotumMedium, 4f, Color.DARK_GRAY,
                    variant.interpretation(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 세 카드종류 모두 뒷면 타이틀은 "한국이름풀이"로 동일하다(HONOR_KOREAN/HONOR_CITIZEN/VISITOR
    // 시안_최종.jpg 전부 육안 확인). STUDENT는 CardLayouts.BACK에 없어 이 메서드까지 오지 않는다.
    private String titleFallbackBackText() {
        return "한 국 이 름 풀 이";
    }

    // 방문증/1은 앞면.png/뒷면.png가 없고 "대지 1.png" 파일 하나뿐이라 그게 앞면인지 뒷면인지
    // 확인이 안 됐다(TODO.md 참고). FRONT_CANDIDATES에 "대지 1.png"가 있어 이 디자인도 별다른
    // 오류 없이 "성공"해버리므로, 잘못된 이미지로 카드가 만들어지는 걸 막기 위해 명시적으로 막는다.
    private boolean isUnverifiedDesign(CardTypeCode cardType, int design) {
        return cardType == CardTypeCode.VISITOR && design == 1;
    }

    private void drawZodiac(Graphics2D g, String zodiacBranch, CardFieldOffset offset, CardLayout layout,
            double scaleX, double scaleY) {
        if (zodiacBranch == null || offset == null) {
            return;
        }
        String path = ZodiacIcon.resourcePathFor(zodiacBranch);
        if (path == null || !resourceExists(path)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        BufferedImage icon = copy(loadImage(path));
        double scale = (ZODIAC_BASE_WIDTH * scaleX) / icon.getWidth();
        int targetW = (int) Math.round(icon.getWidth() * scale);
        int targetH = (int) Math.round(icon.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(icon, 0, 0, targetW, targetH, null);
        sg.dispose();
        drawImageCentered(g, scaled, offset, layout, scaleX, scaleY);
    }

    // 로고·직인은 신청자가 업로드한 이미지를 슬롯 크기에 coverFit해서 그린다. bytes가 null이면(정책상
    // 로고·직인이 없는 신청) 그리지 않는다. 슬롯 참고 파일이 이 디자인에 없으면(에셋 누락, 예:
    // HONOR_CITIZEN/2엔 직인.png가 없음) 조용히 건너뛴다 — 렌더링 자체를 막을 이유는 아니다.
    private void drawSlotImage(Graphics2D g, String dir, List<String> candidates, byte[] bytes,
            CardFieldOffset offset, CardLayout layout, double scaleX, double scaleY) {
        if (bytes == null || bytes.length == 0 || offset == null) {
            return;
        }
        BufferedImage slot = resolveOptionalImage(dir, candidates);
        if (slot == null) {
            return;
        }
        BufferedImage fitted = coverFit(readPhoto(bytes), slot.getWidth(), slot.getHeight());
        drawImageCentered(g, fitted, offset, layout, scaleX, scaleY);
    }

    private void drawBackText(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            CardFieldOffset offset, double baseWidth, double baseHeight, double scaleX, double scaleY) {
        if (text == null || text.isBlank() || offset == null) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setFont(font);
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(text, frc);
        double cx = (baseWidth / 2 + offset.x()) * scaleX;
        double cy = (baseHeight / 2 + offset.y()) * scaleY;
        double x = cx - bounds.getWidth() / 2.0;
        double y = cy + bounds.getHeight() / 2.0 - font.getLineMetrics(text, frc).getDescent();
        g.drawString(text, (float) x, (float) y);
    }

    private String formatIssueDate(LocalDate issueDate) {
        return issueDate == null ? "" : issueDate.format(ISSUE_DATE_FORMAT);
    }

    private void drawTitle(Graphics2D g, CardTypeCode cardType, CardLayout layout, double scaleX, double scaleY) {
        String titlePath = TEMPLATE_ROOT + cardType.name() + "/타이틀.png";
        if (resourceExists(titlePath)) {
            drawImageCentered(g, copy(loadImage(titlePath)), layout.title(), layout, scaleX, scaleY);
        } else {
            // 명예한국인증은 앞면 타이틀 그래픽(로고+텍스트)이 시안에서 아직 누락돼 텍스트로 대체한다.
            drawText(g, titleFallbackText(cardType), batangBold, 9f, Color.WHITE, layout.title(), layout, scaleX, scaleY);
        }
    }

    private String titleFallbackText(CardTypeCode cardType) {
        return switch (cardType) {
            case HONOR_KOREAN -> "명 예 한 국 인 증";
            case HONOR_CITIZEN -> "한 국 명 예 시 민 증";
            case VISITOR -> "방 문 증";
            case STUDENT -> "학 생 증";
        };
    }

    private void drawPhoto(Graphics2D g, String dir, byte[] photoBytes, CardLayout layout, double scaleX, double scaleY) {
        if (photoBytes == null || photoBytes.length == 0) {
            return;
        }
        BufferedImage slot = resolveImage(dir, PHOTO_CANDIDATES);
        BufferedImage photo = readPhoto(photoBytes);
        BufferedImage fitted = coverFit(photo, slot.getWidth(), slot.getHeight());
        drawImageCentered(g, fitted, layout.photo(), layout, scaleX, scaleY);
    }

    // 신청자 사진(임의 크기)을 템플릿 사진 자리 크기에 꽉 채워 중앙 크롭한다(cover-fit).
    private BufferedImage coverFit(BufferedImage src, int targetW, int targetH) {
        double scale = Math.max(targetW / (double) src.getWidth(), targetH / (double) src.getHeight());
        int scaledW = (int) Math.ceil(src.getWidth() * scale);
        int scaledH = (int) Math.ceil(src.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, scaledW, scaledH, null);
        g.dispose();
        int x = (scaledW - targetW) / 2;
        int y = (scaledH - targetH) / 2;
        return scaled.getSubimage(Math.max(x, 0), Math.max(y, 0),
                Math.min(targetW, scaledW), Math.min(targetH, scaledH));
    }

    private void drawImageCentered(Graphics2D g, BufferedImage img, CardFieldOffset offset,
            CardLayout layout, double scaleX, double scaleY) {
        double cx = (layout.baseWidth() / 2 + offset.x()) * scaleX;
        double cy = (layout.baseHeight() / 2 + offset.y()) * scaleY;
        int x = (int) Math.round(cx - img.getWidth() / 2.0);
        int y = (int) Math.round(cy - img.getHeight() / 2.0);
        g.drawImage(img, x, y, null);
    }

    private void drawText(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            CardFieldOffset offset, CardLayout layout, double scaleX, double scaleY) {
        if (text == null || text.isBlank()) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setFont(font);
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(text, frc);
        double cx = (layout.baseWidth() / 2 + offset.x()) * scaleX;
        double cy = (layout.baseHeight() / 2 + offset.y()) * scaleY;
        double x = cx - bounds.getWidth() / 2.0;
        double y = cy + bounds.getHeight() / 2.0 - font.getLineMetrics(text, frc).getDescent();
        g.drawString(text, (float) x, (float) y);
    }

    // 가운데 정렬(drawText)로 그렸을 때의 왼쪽 끝 픽셀 x를 계산한다 — 짝을 이루는 아래 줄들을 그
    // x에 왼쪽 정렬로 맞추기 위한 기준점. 서로 다른 좌표에서 각자 중앙 정렬하면 문자열 길이가 다를 때
    // 왼쪽 끝이 어긋난다(HONOR_CITIZEN에서 주소·발급일자가 이름보다 오른쪽으로 밀리는 문제, 실제
    // 렌더링 후 발견).
    private double leftEdgeX(String text, Font baseFont, float sizeAtBaseScale, CardFieldOffset offset,
            CardLayout layout, double scaleX) {
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D bounds = font.getStringBounds(text, frc);
        double cx = (layout.baseWidth() / 2 + offset.x()) * scaleX;
        return cx - bounds.getWidth() / 2.0;
    }

    // 짝을 이루는 위 줄(이름)의 왼쪽 끝에 맞춰 왼쪽 정렬로 그린다. y좌표·폰트 크기는 이 필드 자신의
    // 값을 그대로 쓰고, x만 pixelX로 고정한다.
    private void drawTextAtPixelX(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            double pixelX, CardFieldOffset offset, CardLayout layout, double scaleX, double scaleY) {
        if (text == null || text.isBlank()) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setFont(font);
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        double cy = (layout.baseHeight() / 2 + offset.y()) * scaleY;
        double y = cy + font.getStringBounds(text, frc).getHeight() / 2.0 - font.getLineMetrics(text, frc).getDescent();
        g.drawString(text, (float) pixelX, (float) y);
    }

    private BufferedImage resolveImage(String dir, List<String> candidates) {
        BufferedImage image = resolveOptionalImage(dir, candidates);
        if (image == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return image;
    }

    // 슬롯 참고 파일이 이 디자인에 아예 없어도(에셋 누락) 예외를 던지지 않고 null을 돌려준다 —
    // 로고·직인처럼 없어도 렌더링 자체는 계속 가능한 선택적 필드에 쓴다.
    private BufferedImage resolveOptionalImage(String dir, List<String> candidates) {
        for (String candidate : candidates) {
            String path = dir + candidate;
            if (resourceExists(path)) {
                return loadImage(path);
            }
        }
        return null;
    }

    private boolean resourceExists(String classpathPath) {
        return new ClassPathResource(classpathPath).exists();
    }

    // 팔레트(PLTE) PNG를 ImageIO.read()가 이 환경에서 색을 깨뜨려 읽는 문제가 있어(빨강/파랑 반전
    // 도트 패턴), Toolkit 경로(AWT의 다른 디코더)로 읽는다. 캐시는 원본 로딩 결과 — 그리기 전엔
    // 항상 {@link #copy}로 복제해서 캐시 원본이 변형되지 않게 한다.
    private BufferedImage loadImage(String classpathPath) {
        return imageCache.computeIfAbsent(classpathPath, path -> {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                byte[] bytes = in.readAllBytes();
                Image awt = Toolkit.getDefaultToolkit().createImage(bytes);
                MediaTracker tracker = new MediaTracker(new Panel());
                tracker.addImage(awt, 0);
                tracker.waitForID(0);
                BufferedImage buffered = new BufferedImage(
                        awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = buffered.createGraphics();
                g.drawImage(awt, 0, 0, null);
                g.dispose();
                return buffered;
            } catch (IOException | InterruptedException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        });
    }

    private BufferedImage readPhoto(byte[] photoBytes) {
        try {
            Image awt = Toolkit.getDefaultToolkit().createImage(photoBytes);
            MediaTracker tracker = new MediaTracker(new Panel());
            tracker.addImage(awt, 0);
            tracker.waitForID(0);
            BufferedImage buffered = new BufferedImage(
                    awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffered.createGraphics();
            g.drawImage(awt, 0, 0, null);
            g.dispose();
            return buffered;
        } catch (InterruptedException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private BufferedImage copy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private Font loadFont(String fileName) {
        try (InputStream in = new ClassPathResource(TEMPLATE_ROOT + "fonts/" + fileName).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | java.awt.FontFormatException e) {
            throw new IllegalStateException("카드 폰트를 읽을 수 없습니다: " + fileName, e);
        }
    }

    private byte[] toPngBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
