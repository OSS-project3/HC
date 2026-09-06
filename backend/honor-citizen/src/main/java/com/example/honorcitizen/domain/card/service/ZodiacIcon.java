package com.example.honorcitizen.domain.card.service;

import java.util.Map;

// 연주 지지(12지) → card-templates/zodiac/{designSet}/{name}.png 파일명. ManseryeokResult.
// confirmedPillars.year의 branch 한 글자를 그대로 키로 쓴다(admin-saju.md "띠 이미지 결정 정책").
// designSet(1~3)은 관리자가 신청 단위로 고르는 십이간지 캐릭터 스타일(2026-09-06 신규,
// Application.zodiacDesignSet) — 카드종류와 무관하게 세 스타일이 공용 폴더 밑에 나뉘어 있다.
final class ZodiacIcon {

    private static final Map<String, String> BRANCH_TO_ANIMAL = Map.ofEntries(
            Map.entry("자", "쥐"),
            Map.entry("축", "소"),
            Map.entry("인", "호랑이"),
            Map.entry("묘", "토끼"),
            Map.entry("진", "용"),
            Map.entry("사", "뱀"),
            Map.entry("오", "말"),
            Map.entry("미", "양"),
            Map.entry("신", "원숭이"),
            Map.entry("유", "닭"),
            Map.entry("술", "개"),
            Map.entry("해", "돼지"));

    static String resourcePathFor(String branch, int designSet) {
        String animal = BRANCH_TO_ANIMAL.get(branch);
        return animal == null ? null : "card-templates/zodiac/" + designSet + "/" + animal + ".png";
    }

    private ZodiacIcon() {
    }
}
