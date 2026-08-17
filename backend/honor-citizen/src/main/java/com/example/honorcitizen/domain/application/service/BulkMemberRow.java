package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * BulkExcelParser가 ZIP의 엑셀 1행을 파싱한 결과를 담는 불변 데이터 컨테이너.
 *
 * [생명주기]
 * 1. BulkExcelParser.parseRow()에서 생성된다.
 * 2. ApplicationService.createGroup()에서 각 멤버 사진을 S3에 업로드한 뒤
 *    photoPath와 함께 GroupMemberUpload에 래핑된다.
 * 3. ApplicationPersistenceService.saveGroup()에서 ApplicationMember 엔티티 생성에 사용된다.
 * 4. DB 저장이 완료되면 GC 대상이 된다.
 *
 * [메모리 주의]
 * photoBytes는 ZIP에서 읽은 사진 파일 전체를 메모리에 보관한다.
 * 단체 신청 멤버가 수십 명이면 이 레코드 배열이 상당한 힙을 차지할 수 있다.
 * S3 업로드가 완료되면 더 이상 필요하지 않으므로 GroupMemberUpload로 래핑된 후
 * BulkMemberRow 리스트는 GC될 수 있도록 보관 범위를 최소화해야 한다.
 *
 * [null 허용 필드]
 * - birthTime: 출생시간은 선택 항목 (일부 국가에서 출생 시각 미기재)
 * - birthRegion: 출생지역도 선택 항목
 * - address: 국내 연락처 주소, 선택 항목
 * - entryDate: 개별 입국날짜. 없으면 공통 입국날짜로 대체되고, 공통 날짜도 없으면 null
 * - studentId, department: 학생증 카드(isStudent=true)에서만 값이 있고 나머지는 null
 */
record BulkMemberRow(
        // 엑셀 A열의 고정 사진 번호 — 사진 파일명(확장자 제외)과 매칭
        // 예: photoNumber="001" ↔ 파일명="001.jpg"
        String photoNumber,

        String englishName,   // 카드에 인쇄될 영문 성명, 필수
        LocalDate birthDate,  // 생년월일, 필수
        String nationality,   // 국적(국가명), 필수
        LocalTime birthTime,  // 출생시간, 선택 (null 가능)
        String birthRegion,   // 출생지역, 선택 (null 가능)
        Gender gender,        // 성별(MALE/FEMALE), 필수

        // 개별 입국날짜. 엑셀 7열에 값이 있으면 그 값, 없으면 공통 입국날짜로 대체.
        // 공통 날짜도 없으면 null. 카드에 방문일로 표시된다.
        LocalDate entryDate,

        String email,         // 신청 확인 및 알림 이메일, 필수
        String phone,         // 연락처 전화번호, 필수
        String address,       // 국내 연락 주소, 선택 (null 가능)

        String studentId,     // 학번 — 학생증 카드에서만 필수, 나머지는 null
        String department,    // 학과 — 학생증 카드에서만 필수, 나머지는 null

        // ZIP에서 읽은 사진 원본 바이트 — S3 업로드 전까지만 메모리에 보관
        byte[] photoBytes,

        // ZIP 내 사진 파일의 원본 파일명 (예: "1.jpg")
        // S3 업로드 시 sanitize 후 키 구성에 사용 (한글·특수문자 → _ 치환)
        String photoFilename) {
}
