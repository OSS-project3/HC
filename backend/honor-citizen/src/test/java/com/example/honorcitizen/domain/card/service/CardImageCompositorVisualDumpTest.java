package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;

// 육안 검증용 — 자동 스위트에서는 실행하지 않는다(@Disabled). 필요할 때만 수동 실행.
@Disabled
class CardImageCompositorVisualDumpTest {

    private final CardImageCompositor compositor = new CardImageCompositor();

    @Test
    void dumpSamples() throws Exception {
        File outDir = new File("C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/card-out");
        outDir.mkdirs();
        CardMemberData data = new CardMemberData("김학생", "Kim Hak-saeng", samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25));

        dump(outDir, CardTypeCode.HONOR_KOREAN, 1, data);
        dump(outDir, CardTypeCode.HONOR_CITIZEN, 1, data);
        dump(outDir, CardTypeCode.VISITOR, 2, data);
        dump(outDir, CardTypeCode.HONOR_KOREAN, 6, data);
    }

    private void dump(File outDir, CardTypeCode type, int design, CardMemberData data) throws Exception {
        byte[] png = compositor.composeFront(type, design, data);
        File out = new File(outDir, type.name() + "-" + design + ".png");
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
}
