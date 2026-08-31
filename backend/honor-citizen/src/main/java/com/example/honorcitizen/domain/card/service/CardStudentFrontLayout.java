package com.example.honorcitizen.domain.card.service;

/**
 * 학생증 앞면 필드 배치(4-C). 다른 3종의 {@link CardLayout}과 달리 카드종류(STUDENT) 하나에
 * 방향(orientation)별로 2세트가 필요하고, 필드 구성 자체도 다르다(카드번호/주소 대신
 * 학번(대학교)/생년월일(고등학교)+학과). 좌표는 디자이너 제공 `학생증_위치값.jpg`를 실제
 * 렌더링(4개 조합)으로 육안 검증·조정한 값을 그대로 옮긴 것 — 이 계획에서 좌표를 새로 정하지 않는다.
 *
 * studentId/birthDate를 별도 필드로 두는 이유: 세로형에서 학번 칸 좌표로 생년월일("생년월일
 * YYYY.MM.DD")을 그리면 캔버스 밖으로 잘린다(실측 확인) — 가로형은 두 필드가 같은 좌표를 쓴다.
 *
 * 로고·직인은 원본 위치값 표에 좌표가 있지만 실제 렌더링 검증이 안 됐다(2026-08-26 탐색
 * 렌더링에서 미사용) — 이번 등록에서 뺀다. 사진 슬롯은 다른 3종처럼 디자인별 참고 파일(`사진.png`)로
 * 크기를 재지 않는다(학생증 디자인엔 그 참고 파일 자체가 없음) — photoWidth/photoHeight 고정값 사용.
 */
record CardStudentFrontLayout(
        double baseWidth,
        double baseHeight,
        CardFieldOffset title,
        CardFieldOffset name,
        CardFieldOffset englishName,
        CardFieldOffset photo,
        double photoWidth,
        double photoHeight,
        CardFieldOffset studentId,
        CardFieldOffset birthDate,
        CardFieldOffset department,
        CardFieldOffset issueDate,
        CardFieldOffset zodiac) {
}
