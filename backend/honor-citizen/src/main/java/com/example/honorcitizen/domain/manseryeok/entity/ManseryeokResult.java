package com.example.honorcitizen.domain.manseryeok.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

// 관리자가 확정한 만세력 계산 입력·결과(admin-saju.md "계산 재현성"). ApplicationMember 1건에 여러 row가
// 쌓일 수 있다 — 재계산 이력을 보존하고 active=true인 행 하나만 "현재 활성 결과"로 취급한다(덮어쓰지 않음).
@Entity
@Table(name = "manseryeok_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManseryeokResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationMemberId;

    // 계산 입력(생년월일시+timezoneId+longitude+엔진 버전)의 해시 — 입력이 바뀌면 기존 결과를 stale로
    // 판정하는 데 쓴다. 알고리즘은 서비스 레벨에서 고정한다(엔티티는 문자열로만 보관).
    @Column(nullable = false, length = 64)
    private String inputHash;

    @Column(nullable = false, length = 100)
    private String timezoneId;

    private Double longitude;

    @Column(length = 10)
    private String selectedOffset;

    private Instant utcInstant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeAccuracy timeAccuracy;

    // {"year":{"stem":"...","branch":"..."}, "month":{...}, "day":{...}, "hour":{...}} 중 확정된 주만.
    @Column(columnDefinition = "TEXT")
    private String confirmedPillarsJson;

    // 확정 못한 주 이름 목록(예: ["hour"]) — PARTIAL/UNKNOWN일 때만 값이 있다.
    @Column(columnDefinition = "TEXT")
    private String uncertainPillarsJson;

    // {"목":n,"화":n,"토":n,"금":n,"수":n} — 프론트가 계산해 보낸 오행 카운트.
    @Column(columnDefinition = "TEXT")
    private String elementCountsJson;

    @Column(length = 50)
    private String tzdbVersion;

    @Column(length = 50)
    private String calculationEngineVersion;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @Column(nullable = false)
    private Long confirmedByAdminId;

    @Column(nullable = false)
    private boolean active;

    public static ManseryeokResult create(Long applicationMemberId, String inputHash, String timezoneId,
            Double longitude, String selectedOffset, Instant utcInstant, TimeAccuracy timeAccuracy,
            String confirmedPillarsJson, String uncertainPillarsJson, String elementCountsJson,
            String tzdbVersion, String calculationEngineVersion, LocalDateTime calculatedAt,
            Long confirmedByAdminId) {
        ManseryeokResult result = new ManseryeokResult();
        result.applicationMemberId = applicationMemberId;
        result.inputHash = inputHash;
        result.timezoneId = timezoneId;
        result.longitude = longitude;
        result.selectedOffset = selectedOffset;
        result.utcInstant = utcInstant;
        result.timeAccuracy = timeAccuracy;
        result.confirmedPillarsJson = confirmedPillarsJson;
        result.uncertainPillarsJson = uncertainPillarsJson;
        result.elementCountsJson = elementCountsJson;
        result.tzdbVersion = tzdbVersion;
        result.calculationEngineVersion = calculationEngineVersion;
        result.calculatedAt = calculatedAt;
        result.confirmedByAdminId = confirmedByAdminId;
        result.active = true;
        return result;
    }

    public void deactivate() {
        this.active = false;
    }
}
