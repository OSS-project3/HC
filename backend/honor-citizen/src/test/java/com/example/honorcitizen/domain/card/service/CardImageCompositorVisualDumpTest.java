package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 육안 검증용 — 자동 스위트에서는 실행하지 않는다(@Disabled). 필요할 때만 수동 실행.
// 2-B: 활성 디자인 17개(HONOR_KOREAN 1~6, HONOR_CITIZEN 1~6, VISITOR 2~6) 앞·뒷면 34개 전체를 덤프한다.
@Disabled
class CardImageCompositorVisualDumpTest {

    private static final Map<CardTypeCode, List<Integer>> ACTIVE_DESIGNS = Map.of(
            CardTypeCode.HONOR_KOREAN, List.of(1, 2, 3, 4, 5, 6),
            CardTypeCode.HONOR_CITIZEN, List.of(1, 2, 3, 4, 5, 6),
            CardTypeCode.VISITOR, List.of(2, 3, 4, 5, 6));

    private final CardImageCompositor compositor = new CardImageCompositor();

    @Test
    void dumpAllActiveDesigns() throws Exception {
        File outDir = new File("C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/card-out");
        outDir.mkdirs();
        CardMemberData hanjaData = new CardMemberData("김", "학생", "Kim Hak-saeng", "學生",
                "배울 학(學) 날 생(生)", "배우고 익히며 성장하는 삶을 산다.", samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), "인",
                sampleSquare(), sampleSquare());
        CardMemberData noHanjaData = new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null,
                samplePhoto(), "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), "말",
                sampleSquare(), sampleSquare());

        for (Map.Entry<CardTypeCode, List<Integer>> entry : ACTIVE_DESIGNS.entrySet()) {
            for (int design : entry.getValue()) {
                dumpFront(outDir, entry.getKey(), design, hanjaData);
                dumpBack(outDir, entry.getKey(), design, hanjaData, "hanja");
                dumpBack(outDir, entry.getKey(), design, noHanjaData, "nohanja");
            }
        }
    }

    private void dumpFront(File outDir, CardTypeCode type, int design, CardMemberData data) throws Exception {
        byte[] png = compositor.composeFront(type, design, data);
        write(outDir, type.name() + "-" + design + "-FRONT.png", png);
    }

    private void dumpBack(File outDir, CardTypeCode type, int design, CardMemberData data, String suffix)
            throws Exception {
        byte[] png = compositor.composeBack(type, design, data);
        write(outDir, type.name() + "-" + design + "-BACK-" + suffix + ".png", png);
    }

    private void write(File outDir, String name, byte[] png) throws Exception {
        File out = new File(outDir, name);
        try (var fos = new java.io.FileOutputStream(out)) {
            fos.write(png);
        }
    }

    private byte[] samplePhoto() throws Exception {
        BufferedImage img = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(210, 210, 220));
        g.fillRect(0, 0, 300, 400);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private byte[] sampleSquare() throws Exception {
        BufferedImage img = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 60, 60));
        g.fillOval(0, 0, 120, 120);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
