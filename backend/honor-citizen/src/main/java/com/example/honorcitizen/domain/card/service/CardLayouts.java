package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;

import java.util.Map;

// 카드종류별 필드 배치(디자이너 제공 위치값.jpg/카드사이즈 및 위치값.jpg 원문 그대로). STUDENT는
// 시안 미제공이라 없음. 발행처(텍스트) 좌표는 표에 있지만 쓰지 않는다 — 1-A 확정 정책(관리자 작명
// 확정·카드 제작 구현 계획)상 발행처는 별도 텍스트 없이 신청자가 업로드한 로고 이미지만으로 표기한다.
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
                    new CardFieldOffset(81.1677, -1.8476),
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
                    new CardFieldOffset(0, -30),
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

    private CardLayouts() {
    }
}
