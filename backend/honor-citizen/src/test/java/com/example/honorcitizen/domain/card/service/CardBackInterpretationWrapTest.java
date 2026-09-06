package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 2026-09-05: 뒷면 뜻풀이(nameInterpretation)가 한 줄로만 그려져 카드 밖으로 잘려나가던 버그
// (실제 렌더링 결과를 사용자가 직접 확인해 발견) — drawBackTextWrapped()로 단어 경계 줄바꿈 +
// 블록 세로중앙정렬을 추가한 수정을 검증한다. 자동 검증은 "예외 없이 디코딩 가능한 이미지가
// 나오는지"까지만 확인하고, 실제 줄바꿈 위치·가독성은 프로젝트 관행대로 렌더링 결과를 파일로
// 남겨 육안 확인한다.
class CardBackInterpretationWrapTest {

    private static final String OUT_DIR =
            "C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/e2e-render/wrap-check/";

    private final CardImageCompositor compositor = new CardImageCompositor();

    @Test
    void longInterpretationWrapsIntoMultipleLinesInsteadOfOverflowing() throws Exception {
        new File(OUT_DIR).mkdirs();

        // 실제 700개 추천 데이터셋 중 가장 긴 meaning(97자)과 대표적인 길이(약 85자) 둘 다 확인.
        String longest = "화평하고 공평한 태도로 소통을 주관하여 무리가 따르다. 평온과 결단의 기운을 모아 천상의 조화를 이루며, 치우치지 않는 사고와 넓은 마음으로 사람을 안는 지혜로운 자가 되다.";
        String typical = "사람마다 가진 장점을 살려내고 열정에 불을 지펴 별처럼 빛나게 돕다. 씨앗의 가능성을 모르는 이들에게 싹을 틔우고 큰 나무로 키워내는 세상의 큰 스승이 되다.";
        String shortOne = "밝고 지혜롭게 살다.";

        assertRendersAndExport("longest", longest);
        assertRendersAndExport("typical", typical);
        assertRendersAndExport("short", shortOne);
    }

    // VISITOR는 세 카드종류 중 유일하게 세로형(baseWidth=156, baseHeight=235)이다 — 가로형만
    // 확인하고 세로형을 안 봤다는 지적을 받아 추가(2026-09-05). 같은 INTERPRETATION_WIDTH_RATIO를
    // 쓰지만 baseWidth 자체가 다르므로 실제 줄바꿈 결과를 따로 확인해야 한다.
    @Test
    void visitorPortraitBackAlsoWrapsLongInterpretation() throws Exception {
        String longest = "화평하고 공평한 태도로 소통을 주관하여 무리가 따르다. 평온과 결단의 기운을 모아 천상의 조화를 이루며, 치우치지 않는 사고와 넓은 마음으로 사람을 안는 지혜로운 자가 되다.";
        assertRendersAndExport(CardTypeCode.VISITOR, 2, "visitor-portrait-longest", longest);

        // 사용자에게 세로형 카드 앞/뒤 한 쌍을 온전히 보여주기 위해 앞면도 함께 렌더링(사진 자리는
        // 실제 얼굴 사진 대신 회색 사각형 — 구조·문구 확인용, 이전 세션 관행과 동일).
        CardMemberData data = new CardMemberData("김", "성노", "Jordan Smith", "星爐",
                "별 성(星) 풀무 노(爐)", longest, solidPng(300, 400, Color.LIGHT_GRAY),
                "ROK-90088-0001", "대한민국 전라북도 전주시", LocalDate.now(), "인", null, null);
        byte[] frontPng = compositor.composeFront(CardTypeCode.VISITOR, 2, data);
        try (FileOutputStream out = new FileOutputStream(OUT_DIR + "visitor-portrait-longest-front.png")) {
            out.write(frontPng);
        }
    }

    // 2026-09-06: 사용자가 "명예한국인증만 타이틀 이미지가 없어서 흰 폴백 텍스트가 안 보인다"는 걸
    // 발견한 뒤, 명예시민증(HONOR_CITIZEN)도 실제로 타이틀.png가 반영되는지 확인해달라고 해서 추가.
    @Test
    void honorCitizenFrontTitleImageIsApplied() throws Exception {
        new File(OUT_DIR).mkdirs();
        CardMemberData data = new CardMemberData("김", "성노", "Jordan Smith", "星爐",
                "별 성(星) 풀무 노(爐)", "밝고 지혜롭게 살다.", solidPng(300, 400, Color.LIGHT_GRAY),
                "ROK-90088-0001", "대한민국 전라북도 전주시", LocalDate.now(), "인", null, null);
        byte[] frontPng = compositor.composeFront(CardTypeCode.HONOR_CITIZEN, 1, data);
        try (FileOutputStream out = new FileOutputStream(OUT_DIR + "honor-citizen-front.png")) {
            out.write(frontPng);
        }
    }

    private byte[] solidPng(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    // composeBack()과 같은 drawBackTextWrapped()/wrapByWidth()를 쓰지만 폰트 크기(8f)·배경(S3 원본
    // 템플릿)이 달라 별도로 확인한다. 가로형(LANDSCAPE)·세로형(PORTRAIT) 둘 다 확인(2026-09-05,
    // 가로만 보고 세로를 안 봤다는 지적을 받아 세로형 추가).
    @Test
    void studentBackAlsoWrapsLongInterpretationBothOrientations() throws Exception {
        new File(OUT_DIR).mkdirs();
        String longest = "화평하고 공평한 태도로 소통을 주관하여 무리가 따르다. 평온과 결단의 기운을 모아 천상의 조화를 이루며, 치우치지 않는 사고와 넓은 마음으로 사람을 안는 지혜로운 자가 되다.";

        renderStudentBack(CardDesignOrientation.LANDSCAPE, "아트보드 8 사본 15.png", "student-landscape-longest", longest);
        renderStudentBack(CardDesignOrientation.PORTRAIT, "아트보드 8 사본 10.png", "student-portrait-longest", longest);
    }

    private void renderStudentBack(CardDesignOrientation orientation, String templateFile, String label,
            String interpretation) throws Exception {
        byte[] templateBack;
        try (FileInputStream in = new FileInputStream("D:/HC-worktrees/saju/시안/시안/학생증/" + templateFile)) {
            templateBack = in.readAllBytes();
        }
        CardMemberData data = new CardMemberData(
                "김", "성노", "Jordan Smith", "星爐", "별 성(星) 풀무 노(爐)", interpretation,
                null, "ROK-90088-0001", null, LocalDate.now(), null, null, null,
                null, orientation, null, null, null, null, templateBack);

        byte[] png = compositor.composeBack(CardTypeCode.STUDENT, 1, data);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();

        try (FileOutputStream out = new FileOutputStream(OUT_DIR + label + "-back.png")) {
            out.write(png);
        }
    }

    private void assertRendersAndExport(String label, String interpretation) throws Exception {
        assertRendersAndExport(CardTypeCode.HONOR_KOREAN, 1, label, interpretation);
    }

    private void assertRendersAndExport(CardTypeCode cardType, int design, String label, String interpretation)
            throws Exception {
        new File(OUT_DIR).mkdirs();
        CardMemberData data = new CardMemberData(
                "김", "성노", "Jordan Smith", "星爐", "별 성(星) 풀무 노(爐)", interpretation,
                null, "ROK-90088-0001", null, LocalDate.now(), null, null, null);

        byte[] png = compositor.composeBack(cardType, design, data);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isGreaterThan(0);

        try (FileOutputStream out = new FileOutputStream(OUT_DIR + label + "-back.png")) {
            out.write(png);
        }
    }
}
