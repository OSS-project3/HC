package com.example.honorcitizen.domain.card.service;

// 뒷면 "한국이름풀이" 필드 배치 1세트. hanja/hanjaMeaning은 한자 유무에 따라 name/englishName/
// interpretation 위치 자체가 미세하게 달라져(디자이너 위치값.jpg 실측) 한자 유무별로 변형(variant)을
// 통째로 분리한다 — 한자 없을 때 한자 영역만 비우고 나머지 좌표를 그대로 쓰지 않는다.
record CardBackVariant(
        CardFieldOffset name,
        CardFieldOffset hanja,
        CardFieldOffset englishName,
        CardFieldOffset hanjaMeaning,
        CardFieldOffset interpretation) {
}
