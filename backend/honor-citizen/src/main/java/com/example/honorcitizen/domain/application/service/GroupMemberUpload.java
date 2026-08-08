package com.example.honorcitizen.domain.application.service;

/**
 * 단체 신청에서 사진 업로드 완료 후 DB 저장 단계로 전달하는 중간 전달 객체(DTO).
 *
 * [존재 이유 — 두 단계 처리의 연결]
 * ApplicationService.createGroup()은 두 단계로 나뉜다:
 *   단계 1: ZIP 파싱 완료 후 각 멤버 사진을 S3에 업로드 → S3 키 확정
 *   단계 2: S3 업로드가 완료된 경로를 포함해 DB에 저장
 *
 * BulkMemberRow는 파싱 결과(엑셀 데이터 + 사진 byte[])를 담고,
 * GroupMemberUpload는 BulkMemberRow에 단계 1에서 확정된 S3 경로(photoPath)를 추가한다.
 * 이렇게 두 레코드를 분리해 "파싱 결과"와 "업로드 결과"의 책임을 명확히 구분한다.
 *
 * [photoBytes의 역할 종료]
 * row.photoBytes()는 S3 업로드에 이미 사용됐으므로 이 레코드 이후에는 참조되지 않는다.
 * 그러나 구조 단순화를 위해 BulkMemberRow 전체를 래핑해 가져온다.
 * 멤버 수가 많으면 photoBytes가 힙에 남아있게 되므로,
 * DB 저장이 완료된 후 이 리스트 참조를 해제해 GC가 photoBytes를 회수할 수 있게 해야 한다.
 *
 * [패키지 가시성]
 * package-private으로 선언해 service 패키지 내에서만 사용한다.
 * 외부 레이어(Controller, 다른 Service)가 이 객체를 다룰 이유가 없다.
 *
 * @param row       BulkExcelParser가 파싱한 멤버 데이터 (엑셀 1행 분량)
 * @param photoPath S3에 업로드 완료된 사진 경로 (예: applications/APP-2026-000001/member-photos/uuid-1.jpg)
 */
record GroupMemberUpload(BulkMemberRow row, String photoPath) {
}
