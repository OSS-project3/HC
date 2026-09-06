package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.SchoolType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

// 2026-09-06: 이름 자간(음절 사이 스페이스) + KoPub Batang Bold 폰트 적용을 앞/뒤면·일반/학생증
// 4개 지점 전부에서 실제로 다른 파일로 남겨 육안 확인한다(이 프로젝트 관행).
class NameSpacingRenderTest {

    private static final String OUT_DIR =
            "C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/e2e-render/wrap-check/";

    private final CardImageCompositor compositor = new CardImageCompositor();

    @Test
    void rendersNameSpacingAcrossFrontAndBack() throws Exception {
        new File(OUT_DIR).mkdirs();

        CardMemberData nonStudent = new CardMemberData("정", "은성", "Jung Eun-seong", "恩星",
                "은혜 은(恩) 별 성(星)", "은혜롭고 별처럼 빛나는 삶을 산다.", null, "ROK-90099-0002",
                "대한민국 전라북도 전주시", LocalDate.now(), "인", null, null, null, null, null, null,
                null, null, null, 1);
        byte[] front = compositor.composeFront(CardTypeCode.HONOR_CITIZEN, 1, nonStudent);
        write("name-spacing-nonstudent-front.png", front);
        byte[] back = compositor.composeBack(CardTypeCode.HONOR_CITIZEN, 1, nonStudent);
        write("name-spacing-nonstudent-back.png", back);

        CardMemberData student = new CardMemberData("정", "은성", "Jung Eun-seong", "恩星",
                "은혜 은(恩) 별 성(星)", "은혜롭고 별처럼 빛나는 삶을 산다.", samplePhoto(), "ROK-90099-0003",
                "대한민국 전라북도 전주시", LocalDate.now(), "인", null, null, SchoolType.UNIVERSITY,
                CardDesignOrientation.PORTRAIT, "202512345", "컴퓨터공학과", null,
                readTemplate("card-templates/STUDENT/1/앞면.png"),
                blankTemplate(651, 981), 1);
        byte[] studentFront = compositor.composeFront(CardTypeCode.STUDENT, 1, student);
        write("name-spacing-student-front.png", studentFront);
        byte[] studentBack = compositor.composeBack(CardTypeCode.STUDENT, 1, student);
        write("name-spacing-student-back.png", studentBack);
    }

    private byte[] samplePhoto() throws Exception {
        var img = new java.awt.image.BufferedImage(300, 400, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(new java.awt.Color(210, 210, 220));
        g.fillRect(0, 0, 300, 400);
        g.dispose();
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private byte[] blankTemplate(int width, int height) throws Exception {
        var img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private byte[] readTemplate(String classpathPath) throws Exception {
        try (var in = new org.springframework.core.io.ClassPathResource(classpathPath).getInputStream()) {
            return in.readAllBytes();
        }
    }

    private void write(String name, byte[] png) throws Exception {
        try (FileOutputStream out = new FileOutputStream(OUT_DIR + name)) {
            out.write(png);
        }
    }
}
