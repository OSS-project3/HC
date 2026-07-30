package com.example.honorcitizen.infra.card;

public record CardImageContext(
        byte[] templateBytes,
        byte[] photoBytes,
        byte[] zodiacBytes,     // nullable — 띠 이미지 없으면 렌더링 생략
        CardImageData data
) {}
