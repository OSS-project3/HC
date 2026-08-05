# DOMAIN_CITIZENCARD.md — 시민증 카드 도메인

## 1. 책임

관리자가 발급한 디지털 명예 시민증 정보를 관리한다.
카드 이미지(PNG) 생성 → S3 업로드 → ZIP 묶음 → Presigned URL 발급까지 담당한다.
Application 1:1 관계이며, REVIEWING 상태에서만 발급 가능하다.

---

## 2. 패키지 위치

```
domain/citizencard/
├── entity/
│   └── CitizenCard.java
├── repository/
│   └── CitizenCardRepository.java
├── service/
│   └── CitizenCardService.java
└── dto/
    ├── CitizenCardIssueResponse.java
    └── CitizenCardDownloadResponse.java

infra/card/
├── CardImageGenerator.java        ← 인터페이스
└── DefaultCardImageGenerator.java ← 구현체 (AWT 기반 placeholder)
```

---

## 3. 엔티티

```java
@Entity
@Table(name = "citizen_cards")
public class CitizenCard extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, unique = true, length = 30)
    private String cardNumber;          // honor_id_YYYYXXXX

    @Column(nullable = false, length = 500)
    private String imagePath;           // S3 키: cards/{cardNumber}.png

    @Column(length = 500)
    private String nameMeaningPath;     // S3 키: cards/meaning_{cardNumber}.png

    @Column(length = 500)
    private String zipPath;             // S3 키: zips/{cardNumber}.zip

    @Column(length = 1000)
    private String downloadUrl;         // Presigned URL (유효 7일)

    private LocalDateTime urlExpiresAt;

    @Column(nullable = false)
    private LocalDateTime issuedAt;
}
```

**DB 컬럼 기준: DB_RULES.md `CITIZEN_CARDS` 테이블**

---

## 4. 카드 번호 생성 규칙

```
형식: honor_id_{YYYY}{XXXX}
예시: honor_id_20240001
YYYY: 발급 연도 4자리
XXXX: 연도별 순번 (0001부터 시작, 매년 초기화)
```

```sql
-- 현재 연도의 최대 순번 조회
SELECT MAX(CAST(SUBSTRING(card_number, 13) AS UNSIGNED))
FROM citizen_cards
WHERE card_number LIKE 'honor_id_YYYY%'
```

- Repository에서 현재 연도 prefix로 max 조회
- 없으면 1, 있으면 +1
- 4자리 zero-padding: `String.format("%04d", seq)`

---

## 5. 흐름

### 시민증 발급 (6-6)

```
POST /admin/applications/{applicationId}/issue-card
Authorization: Bearer {adminToken}

1. applicationId로 Application 조회 → 없으면 APPLICATION_NOT_FOUND
2. Application 상태 검증 → REVIEWING 이 아니면 INVALID_APPLICATION_STATUS
3. KoreanName 존재 확인 → 없으면 KOREAN_NAME_NOT_FOUND
4. CitizenCard 중복 발급 확인 → 있으면 CARD_ALREADY_ISSUED
5. 카드 번호 생성 (honor_id_YYYYXXXX)
6. 카드 이미지 생성 (CardImageGenerator)
   - 시민증 PNG 생성
   - 이름 의미 카드 PNG 생성
7. S3 업로드 (imagePath, nameMeaningPath)
8. ZIP 생성 → S3 업로드 (zipPath)
9. Presigned URL 발급 (7일, downloadUrl, urlExpiresAt)
10. CitizenCard INSERT (issuedAt = now)
11. Application 상태 CARD_READY 전이 (application.markCardReady())
12. AdminActivityLog INSERT (action_type: CARD_ISSUE)
13. ApplicationStatusLog INSERT (REVIEWING → CARD_READY, changed_by: ADMIN)
14. CitizenCardIssueResponse 반환
```

### 카드 ZIP 다운로드 (5-4)

```
GET /api/my/applications/{applicationId}/card/download
Authorization: Bearer {token}

1. applicationId로 Application 조회 → 없으면 APPLICATION_NOT_FOUND
2. 본인 소유 확인 → 아니면 UNAUTHORIZED_ACCESS
3. Application 상태 검증 → CARD_READY 이상이 아니면 CARD_NOT_READY
4. CitizenCard 조회 → 없으면 CARD_NOT_FOUND
5. Presigned URL 유효성 확인
   - urlExpiresAt이 24시간 이상 남아 있으면 기존 URL 반환
   - 만료 임박(24시간 미만)이면 새 Presigned URL 재발급 → DB 갱신
6. CitizenCardDownloadResponse 반환
```

---

## 6. 상태 전이와의 관계

```
REVIEWING
  ↓ KoreanName 등록 (KoreanName 도메인)   ← 상태 변경 없음
REVIEWING
  ↓ 시민증 발급 (이 도메인)
CARD_READY
  ↓ 운송장 등록 (PhysicalOrder 도메인)
SHIPPING
```

---

## 7. 전제 조건

- Application 상태가 **REVIEWING** 이어야 발급 가능.
- **KoreanName이 반드시 존재**해야 발급 가능 (이름이 없으면 카드를 만들 수 없음).
- 동일 applicationId에 CitizenCard가 이미 존재하면 재발급 불가 (`CARD_ALREADY_ISSUED`).
- 다운로드는 Application 상태가 `CARD_READY`, `SHIPPING`, `DELIVERED` 중 하나이어야 가능.

---

## 8. 이미지 생성 구조

```java
public interface CardImageGenerator {
    byte[] generateCitizenCard(CardImageData data);
    byte[] generateNameMeaningCard(CardImageData data);
}

public class CardImageData {
    private String nameEn;
    private String fullNameKo;
    private String fullNameEn;
    private String meaning;
    private String nameOrigin;
    private String cardNumber;
    private String nationality;
    private LocalDate issuedDate;
}
```

- `DefaultCardImageGenerator`: AWT Graphics2D 기반 placeholder 구현체
  - 실제 디자인 확정 전까지 텍스트만 렌더링한 PNG 생성
- 추후 실제 카드 디자인 확정 시 구현체만 교체

---

## 9. Service 메서드

| 메서드 | 설명 | 트랜잭션 |
|--------|------|----------|
| `issueCard(adminId, applicationId)` | 카드 발급 전체 흐름 | `@Transactional` |
| `getDownloadUrl(userId, applicationId)` | 다운로드 URL 조회 (만료 시 재발급) | `@Transactional` |
| `findByApplicationId(applicationId)` | applicationId로 조회 (Optional) | `@Transactional(readOnly = true)` |

---

## 10. Repository

```java
public interface CitizenCardRepository
    extends JpaRepository<CitizenCard, Long> {

    Optional<CitizenCard> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    // 연도별 최대 순번 조회 (카드 번호 채번용)
    @Query("""
        SELECT MAX(CAST(SUBSTRING(c.cardNumber, 13) AS int))
        FROM CitizenCard c
        WHERE c.cardNumber LIKE :prefix
        """)
    Optional<Integer> findMaxSequenceByYearPrefix(@Param("prefix") String prefix);
}
```

---

## 11. DTO

### CitizenCardIssueResponse

```java
// API_SPEC 6-6 Response 기준
public class CitizenCardIssueResponse {
    private String cardNumber;
    private String imagePath;
    private String zipPath;
    private String downloadUrl;
    private LocalDateTime issuedAt;
    private boolean emailSent;          // 이메일 발송 여부 (EmailLog 도메인 구현 후 true)
}
```

### CitizenCardDownloadResponse

```java
// API_SPEC 5-4 Response 기준
public class CitizenCardDownloadResponse {
    private String downloadUrl;
    private LocalDateTime expiresAt;
    private String fileName;            // honor_id_20240001.zip
    private List<String> includes;      // [honor_id_20240001.png, name_meaning_20240001.png]
}
```

---

## 12. 에러 케이스

| 에러 코드 | HTTP | 상황 | 발생 API |
|-----------|------|------|----------|
| `APPLICATION_NOT_FOUND` | 404 | 존재하지 않는 applicationId | POST, GET |
| `INVALID_APPLICATION_STATUS` | 400 | REVIEWING 이 아님 (발급 시) | POST |
| `KOREAN_NAME_NOT_FOUND` | 404 | 한국 이름 미등록 상태 | POST |
| `CARD_ALREADY_ISSUED` | 409 | 이미 발급된 카드 존재 | POST |
| `CARD_NOT_READY` | 400 | 아직 카드 발급 전 상태 (다운로드 시) | GET |
| `CARD_NOT_FOUND` | 404 | CitizenCard 레코드 없음 | GET |
| `UNAUTHORIZED_ACCESS` | 403 | 본인 신청이 아님 | GET |
| `FORBIDDEN` | 403 | ADMIN Role 없음 | POST |
