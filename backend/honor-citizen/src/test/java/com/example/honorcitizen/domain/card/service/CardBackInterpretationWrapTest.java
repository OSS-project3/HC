package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    // composeBack()과 같은 drawBackTextWrapped()/wrapByWidth()를 쓰지만 폰트 크기(8f)·배경(S3 원본
    // 템플릿)이 달라 별도로 확인한다.
    @Test
    void studentBackAlsoWrapsLongInterpretation() throws Exception {
        new File(OUT_DIR).mkdirs();
        byte[] templateBack;
        try (FileInputStream in = new FileInputStream(
                "D:/HC-worktrees/saju/시안/시안/학생증/아트보드 8 사본 15.png")) {
            templateBack = in.readAllBytes();
        }
        String longest = "화평하고 공평한 태도로 소통을 주관하여 무리가 따르다. 평온과 결단의 기운을 모아 천상의 조화를 이루며, 치우치지 않는 사고와 넓은 마음으로 사람을 안는 지혜로운 자가 되다.";

        CardMemberData data = new CardMemberData(
                "김", "성노", "Jordan Smith", "星爐", "별 성(星) 풀무 노(爐)", longest,
                null, "ROK-90088-0001", null, LocalDate.now(), null, null, null,
                null, CardDesignOrientation.LANDSCAPE, null, null, null, null, templateBack);

        byte[] png = compositor.composeBack(CardTypeCode.STUDENT, 1, data);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();

        try (FileOutputStream out = new FileOutputStream(OUT_DIR + "student-longest-back.png")) {
            out.write(png);
        }
    }

    private void assertRendersAndExport(String label, String interpretation) throws Exception {
        CardMemberData data = new CardMemberData(
                "김", "성노", "Jordan Smith", "星爐", "별 성(星) 풀무 노(爐)", interpretation,
                null, "ROK-90088-0001", null, LocalDate.now(), null, null, null);

        byte[] png = compositor.composeBack(CardTypeCode.HONOR_KOREAN, 1, data);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isGreaterThan(0);

        try (FileOutputStream out = new FileOutputStream(OUT_DIR + label + "-back.png")) {
            out.write(png);
        }
    }
}
