# ARCHITECTURE.md — 모놀리식 내부 모듈 규칙

## 1. 아키텍처 개요

단일 Spring Boot 애플리케이션으로 운영되는 **모놀리식 구조**.
내부는 **도메인 기준**으로 패키지를 분리하여 응집도를 높이고 결합도를 낮춘다.

```
[Client / Admin Browser]
         │
         ▼
   [Controller]          ← api/ , api/admin/
         │
         ▼
   [Service]             ← domain/{domain}/service/
         │
         ▼
   [Repository]          ← domain/{domain}/repository/
         │
         ▼
   [DB: H2(로컬) / PostgreSQL(운영)]

   [AWS S3]  ←─────────  infra/storage/StorageService
   [Redis]   ←─────────  infra/security/TokenSessionStore
   [Card이미지생성]  ←──  infra/card/
```

---

## 2. 전체 패키지 구조 (2026-06-11 실제 파일 기준)

```
com.example.honorcitizen/

├── HonorCitizenApplication.java

├── api/
│   ├── AuthController.java              ← 1-1 약관동의, 1-3 토큰갱신, 1-4 로그아웃
│   ├── ApplicationController.java       ← 2-2 단건신청, 5-1 목록, 5-2 상세, 5-3 사진재업로드, 5-4 카드다운로드
│   ├── UploadController.java            ← 2-1 사진업로드
│   ├── ShippingController.java          ← 3-1 배송지등록, 3-2 배송지수정
│   └── admin/
│       └── AdminApplicationController.java  ← 6-5-1 한국이름등록, 6-5-2 한국이름수정, 6-6 시민증발급

├── common/
│   ├── entity/
│   │   └── BaseTimeEntity.java          ← createdAt/updatedAt JPA Auditing
│   ├── enums/
│   │   ├── ApplicationStatus.java       ← PENDING/REVIEWING/PHOTO_REJECTED/CARD_READY/SHIPPING/DELIVERED/CANCELLED
│   │   ├── Gender.java                  ← MALE/FEMALE/OTHER
│   │   └── UserRole.java                ← USER/ADMIN
│   ├── exception/
│   │   ├── CustomException.java         ← 모든 도메인 예외의 단일 진입점
│   │   ├── ErrorCode.java               ← HTTP 상태 + 메시지 통합 관리
│   │   └── GlobalExceptionHandler.java
│   └── response/
│       └── ApiResponse.java             ← { success, data, errorCode, errorMessage }

├── domain/
│   ├── user/
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── RefreshTokenSession.java
│   │   │   └── RefreshTokenStatus.java  ← ACTIVE/ROTATED/REVOKED
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   └── RefreshTokenSessionRepository.java
│   │   ├── service/UserService.java
│   │   └── dto/
│   │       ├── TermsAgreeRequest.java / TermsAgreeResponse.java
│   │       └── TokenRefreshRequest.java / TokenRefreshResponse.java
│   │
│   ├── application/
│   │   ├── entity/Application.java
│   │   ├── repository/ApplicationRepository.java
│   │   ├── service/ApplicationService.java
│   │   └── dto/
│   │       ├── ApplicationCreateRequest.java
│   │       ├── ApplicationResponse.java
│   │       ├── ApplicationSummaryResponse.java   ← 목록(5-1)용
│   │       ├── ApplicationDetailResponse.java    ← 상세(5-2)용
│   │       ├── ApplicationListResponse.java
│   │       └── ApplicationPhotoReuploadResponse.java
│   │
│   ├── photo/
│   │   ├── entity/PhotoUpload.java               ← 업로드 시점 +1h 만료
│   │   ├── repository/PhotoUploadRepository.java
│   │   ├── service/PhotoUploadService.java
│   │   └── dto/PhotoUploadResponse.java
│   │
│   ├── shipping/
│   │   ├── entity/ShippingAddress.java            ← applicationId/userId ID 참조
│   │   ├── repository/ShippingAddressRepository.java
│   │   ├── service/ShippingService.java           ← 배송비 3000원, 예상 배송일 "1~3일"
│   │   └── dto/
│   │       ├── ShippingAddressRequest.java
│   │       ├── ShippingAddressRegisterResponse.java
│   │       └── ShippingAddressUpdateResponse.java
│   │
│   ├── koreanname/
│   │   ├── entity/KoreanName.java
│   │   ├── repository/KoreanNameRepository.java
│   │   ├── service/KoreanNameService.java
│   │   └── dto/
│   │       ├── KoreanNameRegisterRequest.java / KoreanNameRegisterResponse.java
│   │       └── KoreanNameUpdateRequest.java / KoreanNameUpdateResponse.java
│   │
│   └── citizencard/
│       ├── entity/CitizenCard.java
│       ├── repository/CitizenCardRepository.java
│       ├── service/CitizenCardService.java
│       └── dto/
│           ├── CitizenCardIssueResponse.java
│           └── CitizenCardDownloadResponse.java

├── infra/
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtTokenProvider.java       ← HS256 고정, alg 헤더 신뢰 금지
│   │   ├── JwtAuthFilter.java          ← 쿠키 + Bearer 모두 지원
│   │   ├── OAuth2SuccessHandler.java   ← Google/Naver 판별, JWT 쿠키 발급
│   │   ├── OAuth2FailureHandler.java
│   │   ├── AuthCookieManager.java      ← HttpOnly/Secure/SameSite 쿠키 발급
│   │   ├── TokenSessionStore.java      ← Redis: refresh rotation, access blacklist
│   │   └── AuthTokens.java
│   │
│   ├── storage/
│   │   ├── StorageService.java         ← 인터페이스 (upload/uploadBytes/download/presignedUrl/delete)
│   │   ├── S3StorageService.java       ← AWS SDK v2 구현체
│   │   └── S3Config.java               ← S3Client + S3Presigner 빈 등록
│   │
│   └── card/
│       ├── CardImageData.java          ← 카드 필드 VO (이름/카드번호/birthRegion/발급일 등)
│       ├── CardImageContext.java       ← record (templateBytes, photoBytes, zodiacBytes, data)
│       ├── CardImageGenerator.java     ← 인터페이스
│       ├── DefaultCardImageGenerator.java  ← AWT Graphics2D 렌더링
│       └── ZodiacCalculator.java       ← 생년도 → 띠 키 변환 (2020=쥐 기준)
```

---

## 3. 레이어별 책임

### Controller (api/)

- HTTP 요청 수신 및 응답 반환만 담당
- `@RestController` + `@RequestMapping` 사용
- Service 호출 후 `ApiResponse.success()`로 래핑하여 반환
- 입력값 검증은 `@Valid`로 위임
- `@AuthenticationPrincipal Long userId` — JwtAuthFilter가 주입

```java
// 올바른 예
@GetMapping("/my/applications/{applicationId}")
public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getDetail(
    @AuthenticationPrincipal Long userId,
    @PathVariable Long applicationId) {
    return ResponseEntity.ok(ApiResponse.success(service.getMyApplicationDetail(userId, applicationId)));
}
```

### Service (domain/{domain}/service/)

- 비즈니스 로직 전담
- 트랜잭션 경계 관리
- 다른 도메인 Service 또는 Repository 호출 가능 (다른 도메인 Entity 직접 주입 금지)
- Entity → DTO 변환 책임
- 검증 패턴: `Optional.of(...).filter(...).orElseThrow(() -> new CustomException(ErrorCode.XXX))`

### Repository (domain/{domain}/repository/)

- `JpaRepository` 상속
- 복잡한 쿼리는 `@Query` 사용
- 메서드 네이밍: Spring Data JPA 컨벤션 준수

### Entity (domain/{domain}/entity/)

- `@Entity` + `@Table` 명시
- `BaseTimeEntity` 상속 (createdAt, updatedAt 자동 관리)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + static `create()` 팩토리 메서드
- 상태 변경은 엔티티 내부 메서드로 캡슐화 (Setter 공개 금지)

```java
// 올바른 예
public void markCardReady() {
    validateTransition(ApplicationStatus.CARD_READY);
    this.status = ApplicationStatus.CARD_READY;
}

// 금지
public void setStatus(ApplicationStatus status) { ... }  // ❌
```

---

## 4. 도메인 간 의존 규칙

```
user  ←──  application  ←──  koreanname
               │
               └──────────→  citizencard
               │
               └──────────→  shipping
               │
               └──────────→  photo
```

- 단방향 참조만 허용
- 순환 참조 금지
- 도메인 간 Entity 직접 주입 금지 → `Long {domainId}` ID 참조 사용

---

## 5. 예외 처리 패턴

```java
// 개별 예외 클래스 생성 금지 (ApplicationNotFoundException 등) ❌

// 올바른 패턴 ✅
throw new CustomException(ErrorCode.APPLICATION_NOT_FOUND);

// ErrorCode 형식
APPLICATION_NOT_FOUND(404, "신청 정보를 찾을 수 없습니다.")
```

---

## 6. 인프라 모듈 (infra/)

### StorageService 인터페이스 (실제 기준)

```java
public interface StorageService {
    String upload(String key, MultipartFile file);
    String uploadBytes(String key, byte[] bytes, String contentType);
    byte[] download(String key);
    String generatePresignedUrl(String key, long expirySeconds);
    void delete(String key);
}
```

### Security 구조

- 사용자: OAuth2(Google/Naver) → `OAuth2SuccessHandler` → JWT HttpOnly 쿠키 발급
- 어드민: 별도 username/password 로그인 (미구현, `SecurityConfig` 수정 필요)
- `/admin/**` → `hasRole("ADMIN")` 필수
- `/api/**` → `hasAnyRole("USER", "ADMIN")` 필수

### Card 이미지 생성 (infra/card/)

- `CardImageData`: 카드에 들어갈 필드 VO
- `CardImageContext`: record — 템플릿/사진/띠 바이트 배열 + CardImageData 묶음
- `DefaultCardImageGenerator`: AWT Graphics2D로 PNG 오버레이 렌더링
- `ZodiacCalculator`: 생년도 → 띠 동물 키 (`rat`, `ox`, ...) 변환

---

## 7. 설정 파일 구조

```
resources/
├── application.properties           ← 공통 설정 (환경변수 참조 ${VAR:default})
├── application-local.properties     ← 로컬 개발 (H2, app.cookie.secure=false)
└── application-prod.properties      ← 운영 (미생성, PostgreSQL + 환경변수 필요)
```

> `.yml` 아님 — `.properties` 사용
> `spring-dotenv`는 Spring Boot 4.x에서 동작 안 함 → `application-local.properties`에 직접 기입
