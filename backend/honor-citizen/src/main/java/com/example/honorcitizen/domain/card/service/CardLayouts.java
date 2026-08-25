package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;

import java.util.Map;

// 카드종류별 앞면 필드 배치(디자이너 제공 위치값.jpg 원문 그대로). STUDENT는 시안 미제공이라 없음.
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
                    new CardFieldOffset(79.9606, 63.2729)),
            CardTypeCode.HONOR_CITIZEN, new CardLayout(
                    235, 156,
                    new CardFieldOffset(-47.2422, -56.5943),
                    new CardFieldOffset(-85.5052, -17.9807),
                    new CardFieldOffset(-85.3381, -3.0552),
                    new CardFieldOffset(78.3521, -27.2369),
                    new CardFieldOffset(-73.2793, 29.6069),
                    new CardFieldOffset(-36.176, 41.7827),
                    new CardFieldOffset(-76.7358, 64.1948)),
            CardTypeCode.VISITOR, new CardLayout(
                    156, 235,
                    new CardFieldOffset(0, -92.491),
                    new CardFieldOffset(0, 17.3712),
                    new CardFieldOffset(0, 29.4497),
                    new CardFieldOffset(0, -32.9611),
                    new CardFieldOffset(-35.9253, 42.0928),
                    new CardFieldOffset(-34.0664, 58.4863),
                    new CardFieldOffset(-42.7554, 79.2412)));

    private CardLayouts() {
    }
}
