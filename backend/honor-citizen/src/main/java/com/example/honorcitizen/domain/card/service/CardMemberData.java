package com.example.honorcitizen.domain.card.service;

import java.time.LocalDate;

// 카드 앞면 합성에 필요한 값. photo는 원본 바이트(신청 시 업로드된 사진) — 카드 사진 자리 크기에 맞춰
// 합성 시점에 스케일한다.
record CardMemberData(
        String name,
        String englishName,
        byte[] photo,
        String cardNumber,
        String address,
        LocalDate issueDate) {
}
