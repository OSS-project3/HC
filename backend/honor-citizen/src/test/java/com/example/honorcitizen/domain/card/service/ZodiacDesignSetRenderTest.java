package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

// 2026-09-06: 십이간지 캐릭터 디자인 세트(1~3) 실제 반영 확인 — 같은 "인"(호랑이) 연주로 세 세트를
// 각각 렌더링해 실제로 다른 파일(다른 스타일)이 그려지는지 파일로 남겨 육안 확인한다(이 프로젝트 관행).
class ZodiacDesignSetRenderTest {

    private static final String OUT_DIR =
            "C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/e2e-render/wrap-check/";

    private final CardImageCompositor compositor = new CardImageCompositor();

    @Test
    void rendersAllThreeZodiacDesignSets() throws Exception {
        new File(OUT_DIR).mkdirs();
        for (int set = 1; set <= 3; set++) {
            CardMemberData data = new CardMemberData("김", "성노", "Jordan Smith", "星爐",
                    "별 성(星) 풀무 노(爐)", "밝고 지혜롭게 살다.", null, "ROK-90088-0001",
                    "대한민국 전라북도 전주시", LocalDate.now(), "인", null, null, null, null, null, null,
                    null, null, null, set);
            byte[] front = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);
            try (FileOutputStream out = new FileOutputStream(OUT_DIR + "zodiac-set" + set + "-front.png")) {
                out.write(front);
            }
        }
    }
}
