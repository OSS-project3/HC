package com.example.honorcitizen.domain.card.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

// 임시 탐색용 — 학생증 디자이너 에셋이 saju 리포(D:\HC-worktrees\saju\시안\시안\학생증)에 아직
// 부분적으로만 있어(뒷면 빈 배경 없음), CardImageCompositor/CardLayouts를 정식으로 건드리기 전에
// 실제 위치값.jpg 좌표가 맞는지부터 4개 조합(고등학교/대학교 x 가로/세로)으로 검증한다.
// 프로덕션 코드가 아니라 확인 끝나면 삭제할 파일이다.
@Disabled
class StudentCardExploratoryRenderTest {

    private static final String SAJU_ASSET_ROOT = "D:/HC-worktrees/saju/\uc2dc\uc548/\uc2dc\uc548/\ud559\uc0dd\uc99d/";
    private static final String OUT_DIR = "C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/student-card-out/";
    private static final DateTimeFormatter ISSUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private Font dotumMedium;
    private Font dotumBold;
    private Font batangBold;

    private record Offset(double x, double y) {
    }

    private record FrontLayout(
            double baseWidth, double baseHeight,
            Offset title, Offset name, Offset englishName, Offset photo,
            Offset studentIdOrBirth, Offset department, Offset issueDate,
            Offset zodiac, Offset logo, Offset seal) {
    }

    // 가설 검증: 위치값.jpg 좌표가 mm 단위라고 보고 1mm=2.8346pt로 환산해 캔버스 단위(pt)로 바꿨다.
    // 폰트 크기는 표에 pt로 명시돼 있어 그대로 두고 건드리지 않았다 — 좌표만 환산 대상.
    private static final double MM_TO_PT = 2.8346;

    private static Offset mm(double x, double y) {
        return new Offset(x * MM_TO_PT, y * MM_TO_PT);
    }

    private static final FrontLayout PORTRAIT = new FrontLayout(
            156, 235,
            mm(0, -31.592),
            mm(0, 7.471),
            mm(0, 12.418),
            mm(0, -10.353),
            mm(-16.078, 17.046),
            mm(-13.197, 21.068),
            mm(-12.055, 29.988),
            mm(17.253, 15.935),
            mm(0, 36.378),
            mm(10.75, 9.893));

    // 세로형 고등학교 생년월일 전용 — 원래 학번(생년월일) 칸의 x(-16.078)가 발급일자(-12.055)보다
    // 왼쪽으로 치우쳐 있어 "생년월일 YYYY.MM.DD" 문자열이 캔버스 밖으로 잘렸다. 발급일자 x 근처로
    // 옮겨 재렌더링 후 시안과 비교해 조정한다 — 대학교(학번) 쪽 좌표는 그대로 둔다.
    private static final Offset PORTRAIT_BIRTHDATE = mm(-12.055, 17.046);

    private static final FrontLayout LANDSCAPE = new FrontLayout(
            235, 156,
            mm(0, -20.629),
            mm(-3.373, -4.766),
            mm(-1.198, -0.279),
            mm(-27.077, 2.95),
            mm(-3.278, 7.022),
            mm(-1.867, 11.028),
            mm(-27.031, 22.499),
            mm(30.48, 1.856),
            mm(18.177, 22.287),
            mm(10.75, 9.893));

    @Test
    void renderFourCombinations() throws Exception {
        new File(OUT_DIR).mkdirs();
        loadFonts();

        // 고등학교: 학번/학과 없음, 생년월일 있음.
        render("고등학교-세로", "아트보드 8.png", PORTRAIT, "조 동 주", "Jo Dong-ju", null, null,
                "2014.02.05", "인");
        render("고등학교-가로", "아트보드 8 사본 12.png", LANDSCAPE, "조 그 린 나 래", "Jo Grinnarae", null, null,
                "2009.12.08", "해");

        // 대학교: 학번/학과 있음, 생년월일 없음.
        render("대학교-세로", "아트보드 8 사본 2.png", PORTRAIT, "김 가 람", "Park Hae-yul", "200900135", "경상대학 경영학과",
                null, "오");
        render("대학교-가로", "아트보드 8 사본 13.png", LANDSCAPE, "정 슬 찬", "Jeong Seul-chan", "202500225", "인문대학 사회복지학과",
                null, "술");
    }

    private void render(String label, String bgFileName, FrontLayout layout, String name, String englishName,
            String studentId, String department, String birthDate, String zodiacBranch) throws Exception {
        BufferedImage bg = copy(loadImage(SAJU_ASSET_ROOT + bgFileName));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            drawImageCentered(g, copy(loadImage(SAJU_ASSET_ROOT + "학생증_앞면타이틀.png")), layout.title(), layout, scaleX, scaleY);
            drawPhotoPlaceholder(g, layout.photo(), layout, scaleX, scaleY);
            drawText(g, name, batangBold, 12.11f, Color.BLACK, layout.name(), layout, scaleX, scaleY);
            // 영문명은 이름과 각자 다른 좌표에서 가운데 정렬하면 문자열 길이가 달라 왼쪽 끝이 어긋난다
            // (예: 고등학교·가로 "조그린나래"/"Jo Grinnarae" — J가 조보다 오른쪽으로 밀림). 이름의
            // 왼쪽 끝에 맞춰 왼쪽 정렬한다. y좌표·폰트 크기(6.7pt)는 영문명 자신의 값 그대로.
            double nameLeftEdge = leftEdgeX(name, batangBold, 12.11f, layout.name(), layout, scaleX);
            drawTextAtPixelX(g, englishName, dotumBold, 6.7f, Color.DARK_GRAY, nameLeftEdge, layout.englishName(),
                    layout, scaleX, scaleY);
            if (studentId != null) {
                drawText(g, studentId, dotumMedium, 7.6f, Color.BLACK, layout.studentIdOrBirth(), layout, scaleX, scaleY);
                // 학과도 같은 이유로 학번의 왼쪽 끝에 맞춰 왼쪽 정렬한다(예: 대학교·가로 "202500225"/
                // "인문대학 사회복지학과" — 학과가 학번보다 왼쪽으로 삐져나옴). y좌표·폰트 크기(7.6pt)는
                // 학과 자신의 값 그대로.
                double studentIdLeftEdge = leftEdgeX(studentId, dotumMedium, 7.6f, layout.studentIdOrBirth(), layout, scaleX);
                drawTextAtPixelX(g, department, dotumMedium, 7.6f, Color.BLACK, studentIdLeftEdge, layout.department(),
                        layout, scaleX, scaleY);
            } else {
                Offset birthOffset = layout == PORTRAIT ? PORTRAIT_BIRTHDATE : layout.studentIdOrBirth();
                drawText(g, "생년월일 " + birthDate, dotumMedium, 7.6f, Color.BLACK, birthOffset, layout, scaleX, scaleY);
            }
            drawText(g, "발급일자 " + LocalDate.of(2026, 3, 2).format(ISSUE_DATE_FORMAT), dotumBold, 7f, Color.BLACK,
                    layout.issueDate(), layout, scaleX, scaleY);
            drawZodiac(g, zodiacBranch, layout.zodiac(), layout, scaleX, scaleY);
        } finally {
            g.dispose();
        }

        File out = new File(OUT_DIR, label + ".png");
        ImageIO.write(bg, "png", out);
    }

    private void drawPhotoPlaceholder(Graphics2D g, Offset offset, FrontLayout layout, double scaleX, double scaleY) {
        // 실제 사진 슬롯 크기 참고 파일이 없어 임시 크기(위치 확인용, mm 환산 후 간격 기준 대략치).
        int w = (int) Math.round(50 * scaleX);
        int h = (int) Math.round(66 * scaleY);
        BufferedImage placeholder = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D pg = placeholder.createGraphics();
        pg.setColor(new Color(210, 210, 220));
        pg.fillRect(0, 0, w, h);
        pg.dispose();
        drawImageCentered(g, placeholder, offset, layout, scaleX, scaleY);
    }

    private void drawZodiac(Graphics2D g, String branch, Offset offset, FrontLayout layout, double scaleX, double scaleY)
            throws Exception {
        String path = ZodiacIcon.resourcePathFor(branch, 1);
        File f = new File("D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/resources/" + path);
        BufferedImage icon = copy(loadImage(f.getAbsolutePath()));
        double scale = (9d * scaleX) / icon.getWidth();
        int tw = (int) Math.round(icon.getWidth() * scale);
        int th = (int) Math.round(icon.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(icon, 0, 0, tw, th, null);
        sg.dispose();
        drawImageCentered(g, scaled, offset, layout, scaleX, scaleY);
    }

    private void drawImageCentered(Graphics2D g, BufferedImage img, Offset offset, FrontLayout layout,
            double scaleX, double scaleY) {
        double cx = (layout.baseWidth() / 2 + offset.x()) * scaleX;
        double cy = (layout.baseHeight() / 2 + offset.y()) * scaleY;
        int x = (int) Math.round(cx - img.getWidth() / 2.0);
        int y = (int) Math.round(cy - img.getHeight() / 2.0);
        g.drawImage(img, x, y, null);
    }

    private void drawText(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            Offset offset, FrontLayout layout, double scaleX, double scaleY) {
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

    // 학번/학과/발급일자(생년월일)처럼 카드 왼쪽에 몰린 필드용 — 좌표를 텍스트 시작점(왼쪽)으로 보고
    // 오른쪽으로 뻗어나가게 그린다. 이름/영문명처럼 카드 중앙에 오는 필드는 여전히 drawText(가운데 정렬).
    private void drawTextLeft(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            Offset offset, FrontLayout layout, double scaleX, double scaleY) {
        if (text == null || text.isBlank()) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setFont(font);
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(text, frc);
        double x = (layout.baseWidth() / 2 + offset.x()) * scaleX;
        double cy = (layout.baseHeight() / 2 + offset.y()) * scaleY;
        double y = cy + bounds.getHeight() / 2.0 - font.getLineMetrics(text, frc).getDescent();
        g.drawString(text, (float) x, (float) y);
    }

    // 가운데 정렬(drawText)로 그렸을 때의 왼쪽 끝 픽셀 x를 계산한다 — 짝을 이루는 아래 줄(학과/영문명)을
    // 그 x에 왼쪽 정렬로 맞추기 위한 기준점. 표의 좌표값을 서로 다르게 잡아 각자 중앙 정렬하면 문자열
    // 길이가 다를 때 왼쪽 끝이 어긋난다(학과가 학번보다 왼쪽으로 삐져나오거나 영문명이 이름보다
    // 오른쪽으로 밀리는 문제) — 실제 렌더링 후 발견.
    private double leftEdgeX(String text, Font baseFont, float sizeAtBaseScale, Offset offset, FrontLayout layout,
            double scaleX) {
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D bounds = font.getStringBounds(text, frc);
        double cx = (layout.baseWidth() / 2 + offset.x()) * scaleX;
        return cx - bounds.getWidth() / 2.0;
    }

    // 짝을 이루는 위 줄(이름/학번)의 왼쪽 끝에 맞춰 왼쪽 정렬로 그린다. y좌표·폰트 크기는 이 필드
    // 자신의 값을 그대로 쓰고(자간/장평도 이 필드 자체 설정을 따름, 이번엔 미적용 상태 유지), x만
    // pixelX로 고정한다.
    private void drawTextAtPixelX(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            double pixelX, Offset offset, FrontLayout layout, double scaleX, double scaleY) {
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

    private BufferedImage loadImage(String absolutePath) throws Exception {
        try (InputStream in = new FileInputStream(absolutePath)) {
            byte[] bytes = in.readAllBytes();
            Image awt = java.awt.Toolkit.getDefaultToolkit().createImage(bytes);
            MediaTracker tracker = new MediaTracker(new Panel());
            tracker.addImage(awt, 0);
            tracker.waitForID(0);
            BufferedImage buffered = new BufferedImage(awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffered.createGraphics();
            g.drawImage(awt, 0, 0, null);
            g.dispose();
            return buffered;
        }
    }

    private BufferedImage copy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private void loadFonts() throws Exception {
        String root = "D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/resources/card-templates/fonts/";
        dotumMedium = loadFont(root + "KoPub Dotum_Pro Medium.otf");
        dotumBold = loadFont(root + "KoPub Dotum_Pro Bold.otf");
        batangBold = loadFont(root + "KoPub Batang_Pro Bold.otf");
    }

    private Font loadFont(String path) throws Exception {
        try (InputStream in = new FileInputStream(path)) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        }
    }
}
