package com.example.honorcitizen.domain.card.service;

// 뒷면(=한국이름풀이) 필드 배치. baseWidth/baseHeight는 앞면과 같은 카드 종류 기준 캔버스를 공유한다.
record CardBackLayout(
        double baseWidth,
        double baseHeight,
        CardFieldOffset title,
        CardBackVariant hanjaVariant,
        CardBackVariant noHanjaVariant) {
}
