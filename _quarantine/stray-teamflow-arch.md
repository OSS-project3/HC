# TeamFlow 백엔드 아키텍처 명세서

> 모놀리식 구조. 패키지 구조와 내부 모듈 규칙을 정의한다.

---

## 내부 모듈 규칙

### 도메인 간 참조

- 다른 도메인의 데이터가 필요할 때는 **해당 도메인의 Service를 통해서만** 접근한다.
- 다른 도메인의 Repository를 직접 주입하는 것은 금지한다.

```java
// 금지
@RequiredArgsConstructor
public class TaskService {
    private final MemberRepository memberRepository; // X
}

// 허용
@RequiredArgsConstructor
public class TaskService {
    private final MemberService memberService; // O
}
```

**이유**: Repository는 DB 조회만 수행하고 비즈니스 규칙을 모른다. 직접 접근하면 해당 도메인 Service에 정의된 규칙(예: 탈퇴 멤버 필터, 존재 여부 검증)을 우회하게 된다.

---

### Service 인터페이스(Impl) 사용 기준

- 구현체가 하나뿐인 Service는 인터페이스 없이 클래스로 작성한다.
- **구현체가 두 개 이상 생길 가능성이 있을 때만** 인터페이스를 도입한다.

```java
// 인터페이스 불필요 — 구현체가 하나
@Service
public class MemberService { ... }

// 인터페이스 필요 — 규칙 기반 → LLM으로 교체 가능성 있음
public interface AiSummarizeService { ... }
@Service public class RuleBasedAiSummarizeService implements AiSummarizeService { ... }
@Service public class LlmAiSummarizeService implements AiSummarizeService { ... }
```

---

### 복잡한 도메인 조합 — Facade

- 단일 요청에서 **3개 이상의 도메인 Service를 조합**하는 경우 Facade 클래스를 도입한다.
- Facade는 `common/facade` 또는 해당 흐름의 진입 도메인 패키지에 둔다.

```java

@Component
public class MeetingTodoApplyFacade {
    private final MeetingService meetingService;
    private final TaskService taskService;
    private final MeetingTodoService meetingTodoService;
}
```

---

## 레이어별 책임

### Controller

- HTTP 요청 수신 및 응답 반환만 담당한다.
- `@RestController` + `@RequestMapping` 사용.
- Service 호출 후 `ApiResponse`로 래핑하여 반환한다.
- 입력값 검증은 `@Valid`로 위임한다. Controller에 비즈니스 로직을 두지 않는다.

```java
// 허용
@PostMapping("/projects")
public ApiResponse<ProjectResponse> create(
    @Valid @RequestBody ProjectCreateRequest request) {
    return ApiResponse.success(projectService.create(request));
}

// 금지 — 비즈니스 로직이 Controller에 있음
@PostMapping("/projects")
public ApiResponse<?> create(@RequestBody ProjectCreateRequest request) {
    if (request.getDeadline().isBefore(LocalDate.now())) { // X
        throw new IllegalArgumentException("마감일이 과거입니다");
    }
}
```

---

### Service

- 비즈니스 로직을 전담한다.
- 트랜잭션 경계를 관리한다 (`@Transactional`).
- 다른 도메인 Service 호출 가능. 다른 도메인 Repository 직접 주입 금지.
- Entity → DTO 변환 책임을 가진다.

---

### Repository

- `JpaRepository` 상속.
- 복잡한 조회 쿼리는 `@Query` 또는 QueryDSL 사용.
- 동적 조건 검색은 QueryDSL 사용한다.
- 메서드명이 지나치게 길어지면 @Query 또는 QueryDSL로 분리.
- 메서드 네이밍: Spring Data JPA 컨벤션 준수.


---

### Entity

- `@Entity` + `@Table` 명시.
- `BaseTimeEntity` 상속 (`createdAt`, `updatedAt` 자동 관리).
- Setter 전체 공개 금지. 상태 변경은 의미 있는 메서드명으로 캡슐화한다.

```java
// 허용
public void complete() {
    this.status = TaskStatus.DONE;
}

// 금지
public void setStatus(TaskStatus status) { // X
    this.status = status;
}
```

---

## 도메인 간 의존 방향

단방향 참조만 허용한다. 순환 참조는 금지한다.

```
auth
 └──→ member

project ──→ member
task    ──→ project
task    ──→ member
meeting ──→ member
meeting ──→ project
meeting ──→ task      (todos/apply 시)

dashboard ──→ project
dashboard ──→ task
dashboard ──→ member

ai ──→ project
ai ──→ task
ai ──→ member
```

---

## 공통 모듈 (common/)

### BaseTimeEntity

모든 Entity가 상속. `createdAt`, `updatedAt` 자동 관리.

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### ApiResponse

```json
{ "data": { ... }, "message": "ok" }
{ "error": { "code": "PROJECT_NOT_FOUND", "message": "..." } }
```

### Enum

| Enum | 값 |
|------|----|
| `TaskStatus` | `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED` |
| `TaskDifficulty` | `EASY`, `MEDIUM`, `HARD` |
| `ProjectStatus` | `ACTIVE`, `ARCHIVED` |
| `ProjectHealth` | `OK`, `WARN`, `BAD`, `IDLE` |
| `MemberRole` | `PM`, `FRONTEND`, `BACKEND`, `DESIGNER`, `QA` |
| `AiMessageTag` | `INSIGHT`, `NUDGE`, `WARN`, `TIP`, `AUTO`, `SUMMARY`, `PLAN`, `BALANCE`, `READY`, `FOCUS`, `INFO` |

---

## 인증 (Security)

- Spring Security + JWT 사용.
- `/api/v1/auth/**` (로그인, 회원가입)는 인증 불필요.
- 그 외 모든 `/api/v1/**` 엔드포인트는 `Authorization: Bearer {token}` 필수.
- JWT payload: `{ "sub": memberId, "role": "PM" }`.
- `JwtAuthFilter`에서 토큰 파싱 후 `SecurityContext`에 인증 정보 저장.

---

## 설정 파일 구조

```
resources/
├── application.yml          ← 공통 설정
├── application-local.yml    ← 로컬 개발 (H2 또는 로컬 MySQL)
└── application-prod.yml     ← 운영 (환경변수 참조)
```
