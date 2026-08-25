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

    // 앞면 배경/사진 자리 파일명이 디자인마다 다르게 export되어 있어(디자이너 파일명 비표준) 후보를
    // 순서대로 시도한다. TODO.md "카드 이미지 합성" 섹션의 파일명 불일치 조사 결과.
    private static final List<String> FRONT_CANDIDATES = List.of("앞면.png", "대지 1.png");
    private static final List<String> PHOTO_CANDIDATES = List.of("사진.png", "아트보드 2.png", "캐릭터.png");

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
            drawText(g, data.name(), dotumBold, 8f, Color.BLACK, layout.name(), layout, scaleX, scaleY);
            drawText(g, data.englishName(), dotumMedium, 5f, Color.DARK_GRAY, layout.englishName(), layout, scaleX, scaleY);
            drawText(g, data.cardNumber(), dotumMedium, 5f, Color.DARK_GRAY, layout.cardNumber(), layout, scaleX, scaleY);
            drawText(g, data.address(), dotumMedium, 4.5f, Color.DARK_GRAY, layout.address(), layout, scaleX, scaleY);
            drawText(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumMedium, 4.5f, Color.DARK_GRAY,
                    layout.issueDate(), layout, scaleX, scaleY);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 방문증/1은 앞면.png/뒷면.png가 없고 "대지 1.png" 파일 하나뿐이라 그게 앞면인지 뒷면인지
    // 확인이 안 됐다(TODO.md 참고). FRONT_CANDIDATES에 "대지 1.png"가 있어 이 디자인도 별다른
    // 오류 없이 "성공"해버리므로, 잘못된 이미지로 카드가 만들어지는 걸 막기 위해 명시적으로 막는다.
    private boolean isUnverifiedDesign(CardTypeCode cardType, int design) {
        return cardType == CardTypeCode.VISITOR && design == 1;
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

    private BufferedImage resolveImage(String dir, List<String> candidates) {
        for (String candidate : candidates) {
            String path = dir + candidate;
            if (resourceExists(path)) {
                return loadImage(path);
            }
        }
        throw new CustomException(ErrorCode.INVALID_INPUT);
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
