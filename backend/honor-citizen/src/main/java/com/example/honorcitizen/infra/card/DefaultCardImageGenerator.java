package com.example.honorcitizen.infra.card;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class DefaultCardImageGenerator implements CardImageGenerator {

    // --- 시민증 카드 좌표 (580x370px 기준, 템플릿 해상도에 맞게 조정 필요) ---
    private static final int PHOTO_X = 28;
    private static final int PHOTO_Y = 68;
    private static final int PHOTO_W = 138;
    private static final int PHOTO_H = 173;

    private static final int ZODIAC_X = 452;
    private static final int ZODIAC_Y = 55;
    private static final int ZODIAC_SIZE = 100;

    private static final int TEXT_X = 190;
    private static final int KOREAN_NAME_Y = 103;
    private static final int ENGLISH_NAME_Y = 138;
    private static final int CARD_NUMBER_Y = 173;
    private static final int ADDRESS_Y = 210;
    private static final int ISSUE_DATE_X = 358;
    private static final int ISSUE_DATE_Y = 300;

    private static final Color TEXT_COLOR = new Color(30, 30, 30);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private Font baseKoreanFont;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/NanumGothic.ttf")) {
            if (is != null) {
                baseKoreanFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseKoreanFont);
                log.info("NanumGothic 폰트 로딩 성공");
            } else {
                log.warn("NanumGothic.ttf 없음 — src/main/resources/fonts/ 에 파일을 추가하세요. 시스템 폰트로 대체합니다.");
                baseKoreanFont = new Font("Malgun Gothic", Font.PLAIN, 12);
            }
        } catch (FontFormatException | IOException e) {
            log.warn("폰트 로딩 실패, 시스템 폰트로 대체: {}", e.getMessage());
            baseKoreanFont = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    @Override
    public byte[] generateCitizenCard(CardImageContext context) {
        try {
            BufferedImage template = ImageIO.read(new ByteArrayInputStream(context.templateBytes()));
            Graphics2D g = template.createGraphics();
            applyRenderingHints(g);

            if (context.photoBytes() != null) {
                BufferedImage photo = ImageIO.read(new ByteArrayInputStream(context.photoBytes()));
                g.drawImage(photo, PHOTO_X, PHOTO_Y, PHOTO_W, PHOTO_H, null);
            }

            if (context.zodiacBytes() != null) {
                BufferedImage zodiac = ImageIO.read(new ByteArrayInputStream(context.zodiacBytes()));
                g.drawImage(zodiac, ZODIAC_X, ZODIAC_Y, ZODIAC_SIZE, ZODIAC_SIZE, null);
            }

            CardImageData d = context.data();
            g.setColor(TEXT_COLOR);

            g.setFont(koreanNameFont());
            String nameWithHanja = d.getNameOrigin() != null
                    ? d.getFullNameKo() + " (" + d.getNameOrigin() + ")"
                    : d.getFullNameKo();
            g.drawString(nameWithHanja, TEXT_X, KOREAN_NAME_Y);

            g.setFont(englishNameFont());
            g.drawString(d.getFullNameEn(), TEXT_X, ENGLISH_NAME_Y);

            g.setFont(bodyFont());
            g.drawString(d.getCardNumber(), TEXT_X, CARD_NUMBER_Y);
            g.drawString(d.getBirthRegion(), TEXT_X, ADDRESS_Y);

            g.setFont(smallFont());
            g.drawString("발급일자: " + d.getIssuedDate().format(DATE_FORMATTER), ISSUE_DATE_X, ISSUE_DATE_Y);

            g.dispose();
            return toPngBytes(template);
        } catch (IOException e) {
            log.error("시민증 이미지 생성 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public byte[] generateNameMeaningCard(CardImageContext context) {
        try {
            CardImageData d = context.data();
            BufferedImage image = new BufferedImage(580, 370, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            applyRenderingHints(g);

            g.setColor(new Color(255, 252, 245));
            g.fillRect(0, 0, 580, 370);
            g.setColor(new Color(180, 140, 80));
            g.setStroke(new BasicStroke(3));
            g.drawRect(15, 15, 550, 340);

            g.setColor(TEXT_COLOR);
            g.setFont(koreanNameFont());
            g.drawString(d.getFullNameKo(), 40, 80);

            g.setFont(englishNameFont());
            g.drawString(d.getFullNameEn(), 40, 115);

            if (d.getNameOrigin() != null) {
                g.setFont(bodyFont());
                g.drawString(d.getNameOrigin(), 40, 150);
            }

            g.setFont(smallFont());
            int meaningY = wrapText(g, d.getMeaning(), 40, 200, 500, 22);

            g.setColor(new Color(120, 120, 120));
            g.drawString(d.getCardNumber(), 40, meaningY + 40);

            g.dispose();
            return toPngBytes(image);
        } catch (IOException e) {
            log.error("이름 의미 카드 생성 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void applyRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private int wrapText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth) {
                g.drawString(line.toString(), x, currentY);
                currentY += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, currentY);
            currentY += lineHeight;
        }
        return currentY;
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private Font koreanNameFont() { return baseKoreanFont.deriveFont(Font.BOLD, 22f); }
    private Font englishNameFont() { return new Font("SansSerif", Font.BOLD, 14); }
    private Font bodyFont()       { return baseKoreanFont.deriveFont(Font.PLAIN, 13f); }
    private Font smallFont()      { return baseKoreanFont.deriveFont(Font.PLAIN, 11f); }
}
