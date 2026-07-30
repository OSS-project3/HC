# DOMAIN_KOREANNAME.md — 한국 이름 도메인

## 1. 책임

관리자가 외국인 신청자에게 직접 지어준 한국 이름 정보를 관리한다.
Application 1:1 관계이며, 관리자 액션으로만 생성된다.
한국 이름 등록 이후 시민증 발급(CitizenCard) 단계로 진입 가능하다.

---

## 2. 패키지 위치

```
domain/koreanname/
├── entity/
│   └── KoreanName.java
├── repository/
│   └── KoreanNameRepository.java
├── service/
│   └── KoreanNameService.java
└── dto/
    ├── KoreanNameRegisterRequest.java
    └── KoreanNameRegisterResponse.java
```

---

## 3. 엔티티

```java
@Entity
@Table(name = "korean_names")
public class KoreanName extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, length = 10)
    private String familyName;

    @Column(nullable = false, length = 20)
    private String givenName;

    @Column(nullable = false, length = 30)
    private String fullNameKo;       // familyName + givenName

    @Column(nullable = false, length = 100)
    private String fullNameEn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @Column(length = 100)
    private String nameOrigin;       // 한자 등, nullable
}
```

**DB 컬럼 기준: DB_RULES.md `KOREAN_NAMES` 테이블**

---

## 4. 흐름

### 한국 이름 등록 (6-5)

```
POST /admin/applications/{applicationId}/korean-name
Authorization: Bearer {adminToken}

1. applicationId로 Application 조회 → 없으면 APPLICATION_NOT_FOUND
2. Application 상태 검증 → REVIEWING 이 아니면 INVALID_APPLICATION_STATUS
3. 이미 등록된 KoreanName 존재 여부 확인 → 있으면 KOREAN_NAME_ALREADY_EXISTS
4. fullNameKo = familyName + givenName 조합
5. KoreanName INSERT
6. ApplicationStatusLog INSERT
   - from_status: REVIEWING
   - to_status: REVIEWING  (상태 변경 없음, 이름 등록 이벤트 기록용)
   - changed_by: ADMIN
7. AdminActivityLog INSERT
   - action_type: KOREAN_NAME_ASSIGN
   - target_table: korean_names
8. KoreanNameRegisterResponse 반환
```

### 한국 이름 수정 (6-5-2)

```
PATCH /admin/applications/{applicationId}/korean-name
Authorization: Bearer {adminToken}

1. applicationId로 Application 조회 → 없으면 APPLICATION_NOT_FOUND
2. Application 상태 검증 → REVIEWING 이 아니면 INVALID_APPLICATION_STATUS
3. 등록된 KoreanName 조회 → 없으면 KOREAN_NAME_NOT_FOUND
4. fullNameKo = familyName + givenName 재조합
5. KoreanName UPDATE
6. AdminActivityLog INSERT
   - action_type: KOREAN_NAME_UPDATE
   - target_table: korean_names
7. KoreanNameUpdateResponse 반환
```

### 읽기 흐름 (다른 도메인에서 참조)

| 호출 위치 | 반환 형태 | 비고 |
|-----------|-----------|------|
| GET /api/my/applications (5-1) | `fullNameKo` String or null | ApplicationSummaryResponse.koreanName |
| GET /api/my/applications/{id} (5-2) | KoreanNameDetail or null | ApplicationDetailResponse.koreanName |
| GET /admin/applications/{id} (6-3) | KoreanNameDetail or null | 어드민 상세 응답 |

---

## 5. 상태 전이와의 관계

```
PENDING
  ↓ (관리자 검토 시작 — startReview)
REVIEWING
  ↓ 한국 이름 등록 (이 도메인)   ← 상태 변경 없음, REVIEWING 유지
REVIEWING
  ↓ (시민증 발급 — CitizenCard 도메인)
CARD_READY
```

- 이름 등록 자체는 Application 상태를 변경하지 않는다.
- 이름 등록 완료 후 관리자가 `POST /admin/applications/{id}/issue-card` 호출 시 CARD_READY로 전이.
- `REVIEWING` 상태 진입은 별도 서비스 메서드(`ApplicationService.startReview`)가 담당한다.

---

## 6. 전제 조건

- Application 상태가 **REVIEWING** 이어야 등록 가능.
- 동일 applicationId에 KoreanName이 이미 존재하면 등록(`POST`) 불가 → 수정(`PATCH`) 사용.
- 수정은 Application이 REVIEWING 상태일 때만 가능 (CARD_READY 이후 변경 불가).
- 관리자 Role(`ADMIN`) 인증 필수.

---

## 7. Service 메서드

| 메서드 | 설명 | 트랜잭션 |
|--------|------|----------|
| `registerKoreanName(adminId, applicationId, request)` | 이름 등록 + 로그 기록 | `@Transactional` |
| `updateKoreanName(adminId, applicationId, request)` | 이름 수정 + 로그 기록 | `@Transactional` |
| `findByApplicationId(applicationId)` | applicationId로 조회 (Optional) | `@Transactional(readOnly = true)` |

---

## 8. Repository

```java
public interface KoreanNameRepository
    extends JpaRepository<KoreanName, Long> {

    Optional<KoreanName> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);
}
```

---

## 9. DTO

### KoreanNameRegisterRequest

```java
// API_SPEC 6-5 Request 기준
public class KoreanNameRegisterRequest {

    @NotBlank
    private String familyName;     // 성 (한글, 1자)

    @NotBlank
    private String givenName;      // 이름 (한글, 1~2자)

    @NotBlank
    private String fullNameEn;     // 로마자 전체 이름

    @NotBlank
    private String meaning;        // 이름 의미

    private String nameOrigin;     // 한자 등 (nullable)
}
```

### KoreanNameRegisterResponse

```java
// API_SPEC 6-5 Response 기준
public class KoreanNameRegisterResponse {
    private Long koreanNameId;
    private String fullNameKo;
    private String fullNameEn;
    private String meaning;
    private String nameOrigin;
    private String applicationStatus;    // "REVIEWING" 고정
}
```

### KoreanNameUpdateRequest

```java
// PATCH /admin/applications/{id}/korean-name Request
public class KoreanNameUpdateRequest {

    @NotBlank
    private String familyName;

    @NotBlank
    private String givenName;

    @NotBlank
    private String fullNameEn;

    @NotBlank
    private String meaning;

    private String nameOrigin;
}
```

### KoreanNameUpdateResponse

```java
// PATCH /admin/applications/{id}/korean-name Response
public class KoreanNameUpdateResponse {
    private Long koreanNameId;
    private String fullNameKo;
    private String fullNameEn;
    private String meaning;
    private String nameOrigin;
    private LocalDateTime updatedAt;
}
```

### KoreanNameDetail (내부 중첩 DTO — ApplicationDetailResponse 등에서 재사용)

```java
// API_SPEC 5-2, 6-3 Response koreanName 블록 기준
public class KoreanNameDetail {
    private String fullNameKo;
    private String fullNameEn;
    private String meaning;
    private String nameOrigin;
}
```

---

## 10. 에러 케이스

| 에러 코드 | HTTP | 상황 |
|-----------|------|------|
| 에러 코드 | HTTP | 상황 | 발생 API |
|-----------|------|------|----------|
| `APPLICATION_NOT_FOUND` | 404 | 존재하지 않는 applicationId | POST, PATCH |
| `INVALID_APPLICATION_STATUS` | 400 | Application 상태가 REVIEWING 이 아님 | POST, PATCH |
| `KOREAN_NAME_ALREADY_EXISTS` | 409 | 동일 applicationId에 이미 이름 등록됨 | POST |
| `KOREAN_NAME_NOT_FOUND` | 404 | 수정 대상 이름이 없음 | PATCH |
| `FORBIDDEN` | 403 | ADMIN Role 없음 | POST, PATCH |
| `INVALID_INPUT` | 400 | 필수 항목 누락 | POST, PATCH |
