# API 명세서

> `.md`(DB 엔티티 정리)를 기준으로 도메인별로 하나씩 작성합니다.
> 확정된 DB 구조·프론트 구현 기준으로 진행하며, 확정 안 된 사항은 임의로 만들지 않고 TODO로 남깁니다. 이 문서가 가장 최근에 만들어진 API명세입니다.
> **⚠️ 확인필요** 표시 = 이 API가 실제 프론트 화면/호출로 검증된 게 아니라는 뜻(화면이 아예 없거나, mock이라 서버 호출을 안 하는 상태). DB 구조·정책 기준으로 설계는 됐지만, 프론트 구현 후 실제 요청 형태와 다를 수 있으니 그때 다시 대조 필요.

---

## 공통 규칙 (2026-07-29 확정)

**기존 백엔드(`backend/honor-citizen`)의 `ApiResponse<T>`/`GlobalExceptionHandler`/`ErrorCode`를 그대로 재사용합니다.** 인증(JWT)과 마찬가지로 도메인 독립적인 인프라라 새로 안 만들고 가져다 씁니다.

### 성공 응답
```json
{
  "success": true,
  "data": { ... }
}
```
(데이터 없는 성공은 `data: null`)

### 실패 응답

⚠️ **주의 — `error: {code, message}`처럼 중첩된 게 아니라, `errorCode`/`errorMessage`가 최상위에 나란히 있습니다.**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_INPUT",
  "errorMessage": "입력값 검증에 실패했습니다."
}
```

### 공통 에러 코드 (기존 `ErrorCode.java`에서 그대로 재사용 — 도메인 무관)

| 코드 | HTTP | 메시지 |
|---|---|---|
| `INVALID_INPUT` | 400 | 입력값 검증에 실패했습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 권한이 없습니다. |
| `NOT_FOUND` | 404 | 데이터를 찾을 수 없습니다. |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |

도메인별 에러 코드(예: `DUPLICATE_APPLICATION`, `APPLICATION_NOT_FOUND` 등)는 각 도메인 API 설계(⑤ Validation) 때 그때그때 추가합니다. 기존 `ErrorCode.java`에 이미 있는 이름은 재사용하고, 새로 필요한 것만 신규로 만듭니다.

---
