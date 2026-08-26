package com.example.honorcitizen.domain.card.service;

/**
 * 카드종류 1개의 앞면 필드 배치 — 디자이너 제공 `위치값.jpg`(카드종류당 1장, 디자인 6개가 좌표 공유)를
 * 그대로 옮긴 값이다. 직인/발행처는 실제 지자체 관인을 무단 사용하는 문제로 이번 범위에서 제외했다
 * (TODO.md "카드 이미지 합성" 섹션 참고).
 *
 * baseWidth/baseHeight는 위치값 표의 기준 캔버스 크기(72dpi 기준 pt=px, 예: 235×156) — 오프셋은
 * 이 캔버스의 중심(0,0)으로부터의 거리다. 실제 템플릿 PNG는 이보다 큰 해상도라 합성 시
 * (실제크기/기준크기) 배율을 곱해 변환한다.
 */
record CardLayout(
        double baseWidth,
        double baseHeight,
        CardFieldOffset title,
        CardFieldOffset name,
        CardFieldOffset englishName,
        CardFieldOffset photo,
        CardFieldOffset cardNumber,
        CardFieldOffset address,
        CardFieldOffset issueDate,
        CardFieldOffset zodiac,
        CardFieldOffset issuerLogo,
        CardFieldOffset seal) {
}
