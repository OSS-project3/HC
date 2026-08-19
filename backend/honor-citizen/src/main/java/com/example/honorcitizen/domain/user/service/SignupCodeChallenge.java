package com.example.honorcitizen.domain.user.service;

// Redis에 저장되는 인증 코드 challenge 값. codeHmac은 평문 코드가 아니라 서버 Secret으로 HMAC한 값이다.
// challengeId는 "메일 발송 실패 시 이 요청이 방금 저장한 challenge만 지운다"는 compare-and-delete 안전장치용.
record SignupCodeChallenge(String challengeId, String codeHmac, int attempts) {
}
