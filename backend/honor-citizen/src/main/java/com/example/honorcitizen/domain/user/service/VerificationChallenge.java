package com.example.honorcitizen.domain.user.service;

// Redis에 저장되는 인증 코드 challenge 값(가입 인증·계정 복구 공용). codeHmac은 평문 코드가 아니라
// 서버 Secret으로 HMAC한 값이다. challengeId는 "메일 발송 실패 시 이 요청이 방금 저장한 challenge만
// 지운다"는 compare-and-delete 안전장치용 — 계정 복구 흐름에서는 공개 requestId와 같은 값을 쓴다.
// userId는 계정 복구(아이디 찾기·비밀번호 재설정)에서만 채워진다 — 가입 인증은 User row가 생성되기
// 전이라 null이다.
record VerificationChallenge(String challengeId, String codeHmac, int attempts, Long userId) {
}
