# AGENT.md — 전체 행동 규칙

## 1. 프로젝트 개요

한국을 방문한 외국인에게 한국식 이름을 부여하고,
명예 시민증(Honor Citizen Card)을 발급해주는 웹 서비스.

- 사용자는 이름·국적·생년월일·사진을 입력
- 관리자가 직접 한국 이름을 지어서 등록
- 시민증 이미지가 생성되면 사용자가 다운로드/공유

---

## 2. 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| ORM | Spring Data JPA + Hibernate |
| DB | H2 (로컬/테스트) / PostgreSQL (운영) |
| Storage | AWS S3 (SDK v2) |
| Cache | Redis (access token blacklist, refresh session jti) |
| Build | Gradle |
| Auth | Spring Security OAuth2 (Google/Naver) + JWT (HS256) |
| 설정 파일 | `application.properties` (`.yml` 아님) |

> **Flyway 미사용**: 현재 JPA `ddl-auto`로 스키마 관리. 운영 전환 시 추가 예정.
> **Swagger 미구현**: API 명세는 `docs/api/API_SPEC.md` 참조.

---

## 3. 코드 작성 원칙

### 3-1. 레이어 역할 엄수

- **Controller** — HTTP 요청/응답만 처리. 비즈니스 로직 금지.
- **Service** — 비즈니스 로직 전담. 트랜잭션 경계 여기서 관리.
- **Repository** — DB 접근만 담당. 쿼리 외 로직 금지.
- **Entity** — 상태 변경 메서드는 엔티티 내부에 위치 (도메인 모델 원칙).

### 3-2. DTO 규칙

- Entity를 Controller까지 절대 노출하지 않는다.
- Request DTO는 `@Valid` 어노테이션으로 입력 검증.
- Response DTO는 필요한 필드만 포함 (최소 노출 원칙).
- 네이밍: `{도메인}{동작}Request.java` / `{도메인}{동작}Response.java`

### 3-3. 예외 처리

- 모든 커스텀 예외는 `CustomException(ErrorCode)` 단일 패턴으로 처리.
- 개별 예외 클래스(`ApplicationNotFoundException` 등) 생성 금지.
- `GlobalExceptionHandler`에서 `CustomException` 일괄 처리.
- 에러 코드 추가 시 반드시 `common/exception/ErrorCode.java`에 등록.

### 3-4. 트랜잭션

- 읽기 전용 메서드는 반드시 `@Transactional(readOnly = true)` 사용.
- 쓰기 메서드는 `@Transactional` 명시.
- Service 외 레이어에서 `@Transactional` 사용 금지.

### 3-5. 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `ApplicationService` |
| 메서드/변수 | camelCase | `findByCardNumber` |
| 상수 | UPPER_SNAKE | `MAX_PHOTO_SIZE` |
| DB 컬럼 | snake_case | `birth_date` |
| API 경로 | kebab-case | `/citizen-cards` |

### 3-6. 금지 사항

- `System.out.println()` 사용 금지 → SLF4J Logger 사용
- `@Autowired` 필드 주입 금지 → 생성자 주입 (`@RequiredArgsConstructor`)
- 도메인 간 Entity 직접 참조 금지 → ID 참조 또는 Service 경유
- 하드코딩된 설정값 금지 → `application.properties` + `@Value`

### 3-7. StorageService 인터페이스 (실제 구현 기준)

```java
public interface StorageService {
    String upload(String key, MultipartFile file);
    String uploadBytes(String key, byte[] bytes, String contentType);
    byte[] download(String key);
    String generatePresignedUrl(String key, long expirySeconds);
    void delete(String key);
}
```

---

## 4. 패키지 구조 원칙

```
com.example.honorcitizen/          ← 실제 base package
├── domain/                        ← 도메인 기준 분리 (핵심 원칙)
│   ├── {domain}/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
├── api/                           ← 사용자 API 컨트롤러
│   └── admin/                     ← 어드민 API 컨트롤러
├── common/                        ← 공통 유틸, 예외, 응답 형식
├── infra/                         ← 외부 시스템 연동 (S3, Security, Card)
│   ├── security/
│   ├── storage/
│   └── card/                      ← 카드 이미지 생성 로직
└── (config/)                      ← 설정 클래스 (현재 S3Config만 존재)
```

- 도메인 간 의존 방향: `api → service → repository → entity`
- 역방향 의존 절대 금지

---

## 5. 인증/보안 구조

- 사용자: OAuth2 로그인 (Google/Naver) → JWT accessToken/refreshToken HttpOnly 쿠키 발급
- 어드민: 별도 username/password 로그인 + ADMIN role (`SecurityConfig` 분리 필요)
- `/admin/**` → `hasRole("ADMIN")` 필수
- `/api/**` → `hasAnyRole("USER", "ADMIN")` 필수 (인증 없이 접근 불가)
- `@AuthenticationPrincipal Long userId` — `JwtAuthFilter`가 userId를 principal로 주입

---

## 6. Git 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 운영 배포 |
| `develop` | 통합 개발 |
| `feature/{기능명}` | 기능 개발 |
| `fix/{버그명}` | 버그 수정 |

커밋 메시지 형식: `[타입] 내용`
타입: `feat` / `fix` / `refactor` / `docs` / `test`

---

## 7. 관련 문서

| 문서 | 설명 |
|------|------|
| `docs/architecture/Architecture.md` | 모놀리식 내부 모듈 구조 (실제 파일 목록 포함) |
| `docs/api/API_SPEC.md` | 전체 API 명세 (Request/Response 필드명 기준) |
| `docs/db/DB_RULES.md` | 엔티티 컬럼명, 상태값, 제약조건 기준 |
| `docs/domain/APPLICATION.md` | 신청 도메인 규칙 |
| `docs/domain/DOMAIN_KOREANNAME.md` | 한국 이름 도메인 규칙 |
| `docs/domain/DOMAIN_CITIZENCARD.md` | 시민증 카드 도메인 규칙 |
| `CLAUDE.md` | 구현 현황, 작업 전 체크리스트 |
