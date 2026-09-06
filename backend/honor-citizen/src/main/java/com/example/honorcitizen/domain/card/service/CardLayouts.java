package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;

import java.util.Map;

// 카드종류별 필드 배치(디자이너 제공 위치값.jpg/카드사이즈 및 위치값.jpg 원문 그대로). 발행처(텍스트)
// 좌표는 표에 있지만 쓰지 않는다 — 1-A 확정 정책(관리자 작명 확정·카드 제작 구현 계획)상 발행처는
// 별도 텍스트 없이 신청자가 업로드한 로고 이미지만으로 표기한다.
final class CardLayouts {

    static final Map<CardTypeCode, CardLayout> FRONT = Map.of(
            CardTypeCode.HONOR_KOREAN, new CardLayout(
                    235, 156,
                    new CardFieldOffset(-0.5437, -55.9868),
                    new CardFieldOffset(-9.164, -22.6696),
                    new CardFieldOffset(-1.823, -8.4263),
                    new CardFieldOffset(-76.3825, 6.9823),
                    new CardFieldOffset(4.4421, 12.9746),
                    new CardFieldOffset(23.3687, 35.6633),
                    new CardFieldOffset(79.9606, 63.2729),
                    // 십이간지 아이콘 위치·크기는 2026-09-06 사용자 확인 후 조정 중 — 원래보다
                    // 아래로 내리고(y +14) 크기도 키웠다(ZODIAC_BASE_WIDTH 참고).
                    new CardFieldOffset(81.1677, 12.1524),
                    new CardFieldOffset(-98.7078, 63.3311),
                    new CardFieldOffset(-13.0232, 59.0572)),
            // ⚠️ 캐릭터/직인 두 값은 "카드사이즈 및 위치값.jpg" 원문 그대로가 아니다. 원문은
            // 캐릭터=2.1059/4.2076(육안 확인 결과 시안과 달리 카드 정중앙에 찍힘), 직인=138.237/168.61
            // (baseWidth/baseHeight 절반(117.5/78)을 넘어 캔버스 밖으로 나가 렌더링해도 안 보임) —
            // 둘 다 다른 8개 필드와 달리 실측이 명백히 안 맞는다(2-B 실제 렌더링으로 발견).
            // 시안_최종.jpg 목업을 참고해 HONOR_KOREAN과 같은 상대적 위치(이름 줄 높이의 배지,
            // 발행처 텍스트 오른쪽의 직인)로 잠정 대체했다 — 디자이너 재확인 전까지 추정값이다.
            CardTypeCode.HONOR_CITIZEN, new CardLayout(
                    235, 156,
                    new CardFieldOffset(-47.2422, -56.5943),
                    new CardFieldOffset(-85.5052, -17.9807),
                    new CardFieldOffset(-85.3381, -3.0552),
                    new CardFieldOffset(78.3521, -27.2369),
                    new CardFieldOffset(-73.2793, 29.6069),
                    new CardFieldOffset(-36.176, 41.7827),
                    new CardFieldOffset(-76.7358, 64.1948),
                    // 위 HONOR_KOREAN과 동일한 이유로 아래로 내림(y +14).
                    new CardFieldOffset(0, -16),
                    new CardFieldOffset(-4.0777, 63.8937),
                    new CardFieldOffset(90, 60)),
            CardTypeCode.VISITOR, new CardLayout(
                    156, 235,
                    new CardFieldOffset(0, -92.491),
                    new CardFieldOffset(0, 17.3712),
                    new CardFieldOffset(0, 29.4497),
                    new CardFieldOffset(0, -32.9611),
                    new CardFieldOffset(-35.9253, 42.0928),
                    new CardFieldOffset(-34.0664, 58.4863),
                    new CardFieldOffset(-42.7554, 79.2412),
                    new CardFieldOffset(50.0527, 60.1066),
                    new CardFieldOffset(-27.4965, 102.7491),
                    new CardFieldOffset(53.9352, 27.7603)));

    // "한국이름풀이" 뒷면. 한자 유무에 따라 이름/영문명/풀이 위치 자체가 미세하게 달라진다(디자이너
    // 실측값 그대로 — 한자 있을 때/없을 때 표가 서로 다른 좌표를 준다).
    static final Map<CardTypeCode, CardBackLayout> BACK = Map.of(
            CardTypeCode.HONOR_KOREAN, new CardBackLayout(
                    235, 156,
                    new CardFieldOffset(0, -56.7161),
                    new CardBackVariant(
                            new CardFieldOffset(0, -33.1887),
                            new CardFieldOffset(0, -19.8344),
                            new CardFieldOffset(0, -1.5711),
                            new CardFieldOffset(0, 14.9756),
                            new CardFieldOffset(0, 46.7319)),
                    new CardBackVariant(
                            new CardFieldOffset(0, -21.6696),
                            null,
                            new CardFieldOffset(0, -2.867),
                            null,
                            new CardFieldOffset(0, 46.5493))),
            CardTypeCode.HONOR_CITIZEN, new CardBackLayout(
                    235, 156,
                    new CardFieldOffset(0, -58.4236),
                    new CardBackVariant(
                            new CardFieldOffset(0, -34.9751),
                            new CardFieldOffset(0, -21.5424),
                            new CardFieldOffset(0, -3.2786),
                            new CardFieldOffset(0, 15.2681),
                            new CardFieldOffset(0, 47.0195)),
                    new CardBackVariant(
                            new CardFieldOffset(0, -20.925),
                            null,
                            new CardFieldOffset(0, 2.0146),
                            null,
                            new CardFieldOffset(0, 48.5879))),
            CardTypeCode.VISITOR, new CardBackLayout(
                    156, 235,
                    new CardFieldOffset(0, -91.5369),
                    new CardBackVariant(
                            new CardFieldOffset(0, -56.1228),
                            new CardFieldOffset(0, -42.7685),
                            new CardFieldOffset(0, -19.7818),
                            new CardFieldOffset(0, 13.7778),
                            new CardFieldOffset(0, 62.9526)),
                    new CardBackVariant(
                            new CardFieldOffset(0, -56.1005),
                            null,
                            new CardFieldOffset(0, -36.2307),
                            null,
                            new CardFieldOffset(0, 62.9722))));

    // 학생증 좌표(4-C, `학생증_위치값.jpg`)는 mm 단위로 주어졌다 — 다른 3종처럼 이미 pt/px 단위인
    // 표와 달리 여기만 변환이 필요하다(디자이너 표 형식이 카드종류마다 다름, 2026-08-26 탐색
    // 렌더링에서 확인). 72dpi 기준 1mm=2.8346pt이고, 카드 물리 크기(83×55mm)를 이 배율로 환산하면
    // 235×156(다른 3종의 LANDSCAPE baseWidth/baseHeight와 정확히 일치)이 나와 이 환산이 맞음을
    // 교차 확인했다.
    private static final double MM_TO_PT = 2.8346;

    private static CardFieldOffset mm(double x, double y) {
        return new CardFieldOffset(x * MM_TO_PT, y * MM_TO_PT);
    }

    // 앞면. 좌표는 탐색 렌더링(4개 조합 실제 렌더링 후 육안 비교)으로 조정·검증 완료된 값 — 원문
    // 위치값.jpg 그대로가 아니라 세로형 학번/생년월일 칸처럼 실측으로 보정된 부분이 있다(아래 참고).
    static final Map<CardDesignOrientation, CardStudentFrontLayout> STUDENT_FRONT = Map.of(
            CardDesignOrientation.PORTRAIT, new CardStudentFrontLayout(
                    156, 235,
                    mm(0, -31.592),
                    mm(0, 7.471),
                    mm(0, 12.418),
                    mm(0, -10.353), 50, 66,
                    mm(-16.078, 17.046),
                    // 세로형 생년월일 전용 보정 — 원래 학번 칸 x(-16.078)를 그대로 쓰면 "생년월일
                    // YYYY.MM.DD" 문자열이 캔버스 밖으로 잘려(실측 확인) 발급일자 x(-12.055) 근처로
                    // 옮겼다. 대학교(학번) 쪽 좌표는 원문 그대로 안 건드림.
                    mm(-12.055, 17.046),
                    mm(-13.197, 21.068),
                    mm(-12.055, 29.988),
                    // 2026-09-06 사용자 확인 — 영문명 줄과 가로 방향으로 가까워 아래로 더 내림(y +4mm).
                    mm(17.253, 19.935)),
            CardDesignOrientation.LANDSCAPE, new CardStudentFrontLayout(
                    235, 156,
                    mm(0, -20.629),
                    mm(-3.373, -4.766),
                    mm(-1.198, -0.279),
                    mm(-27.077, 2.95), 50, 66,
                    mm(-3.278, 7.022),
                    mm(-3.278, 7.022), // 가로형은 학번/생년월일 칸 좌표가 같다(캔버스 밖으로 안 잘림).
                    mm(-1.867, 11.028),
                    mm(-27.031, 22.499),
                    // 위 PORTRAIT와 동일한 이유로 아래로 내림(y +4mm).
                    mm(30.48, 5.856)));

    // 뒷면. 원문 위치값.jpg 표 그대로(4-C 구현 시 처음 렌더링해서 확인). 이름/한자/영문/풀이 4개뿐이라
    // 다른 3종의 한자뜻음(hanjaVariant의 hanjaMeaning) 줄이 없다 — 그 offset을 null로 둔다. 한자
    // 유무와 무관하게 좌표가 안 바뀌므로(표에 "한자 있을 때/없을 때" 구분이 없음) hanjaVariant/
    // noHanjaVariant에 동일한 좌표값을 쓴다. 뒷면 타이틀은 다른 3종과 동일하게 이미지가 아니라
    // 텍스트("학 생 증", titleFallbackText() 재사용)로 그린다 — 이미지 에셋을 새로 안 만든다.
    //
    // ⚠️ PORTRAIT 영문명 y좌표는 원문(-12.866) 그대로가 아니다 — 한자·영문명이 같은 폰트크기(9pt)를
    // 쓰는데 원문 간격(한자 -15.268 → 영문명 -12.866, 2.402mm)이 그 폰트 한 줄 높이보다 좁아 한자
    // 있는 학생 렌더링 시 두 줄이 겹친다(실측 확인, 2026-08-31). 아래로 여유 공간이 넉넉해(영문명
    // 다음 풀이 줄까지 30mm 이상 비어있음) 영문명만 아래로 옮겼다 — 이동폭(약 5.8mm)은 같은 폰트
    // 크기 조합이 실제로 안 겹치는 LANDSCAPE의 한자→영문명 간격(5.844mm)을 그대로 참고했다. 이름/
    // 한자/풀이 좌표는 원문 그대로 안 건드림(앞면 세로형 생년월일 보정과 같은 성격의 실측 보정).
    static final Map<CardDesignOrientation, CardBackLayout> STUDENT_BACK = Map.of(
            CardDesignOrientation.PORTRAIT, new CardBackLayout(
                    156, 235,
                    mm(0, -32.292),
                    new CardBackVariant(mm(0, -19.439), mm(0, -15.268), mm(0, -9.5), null, mm(0, 17.436)),
                    new CardBackVariant(mm(0, -19.439), mm(0, -15.268), mm(0, -9.5), null, mm(0, 17.436))),
            CardDesignOrientation.LANDSCAPE, new CardBackLayout(
                    235, 156,
                    mm(0, -20.615),
                    new CardBackVariant(mm(0, -13.247), mm(0, -8.145), mm(0, -2.301), null, mm(0, 12.91)),
                    new CardBackVariant(mm(0, -13.247), mm(0, -8.145), mm(0, -2.301), null, mm(0, 12.91))));

    private CardLayouts() {
    }
}
