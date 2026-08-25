## Payment 도메인

> ⚠️ **정정(2026-08-25) — 이 문서의 옛 설계는 실제 구현과 다르다.** 별도 Payment 컨트롤러/도메인은 **없다**. 입금자명 저장은 Application 도메인의 실제 엔드포인트로 구현됐다:
> - 실제 경로: **`PATCH /api/applications/{applicationId}/depositor`** (문서의 `.../payment` 아님)
> - 요청: `DepositorNameUpdateRequest { depositorName }` (`@NotBlank`, `@Size(max=60)`) / 응답: **`ApiResponse<Void>`**(body 없음)
> - 권한: 신청자 **본인만**, **결제 확인 전(SUBMITTED·WAITING)** 에만 허용
> - 결제 "확인"은 관리자 수동 전이 `POST /api/admin/applications/{id}/confirm-payment`로 처리(결제 게이트웨이 없음).
> 상세는 `docs/FRONTEND_API_GAPS.md` §1.11 / `docs/LOCALSTORAGE_TO_BACKEND.md` §2.1. 아래는 낡은 설계 기록.

### ① 도메인의 책임

입금 확인을 관리한다. PG/가상계좌 자동화가 아니라 **고정 회사 계좌 무통장입금 + 관리자 수동 확인** 방식 — 사용자가 입금자명을 등록하면, 관리자가 그 이름을 기준으로 통장 내역과 대조해서 확인 처리한다. (`.md` 2.5절 기준)

> 스코프 참고: 관리자가 입금을 "확인 처리"하는 쪽은 Admin 도메인에서 다룹니다. 이번 패스는 **사용자가 입금자명을 등록하는 흐름**만 다룹니다.

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `StepComplete.tsx` | 고정 계좌 정보 표시(은행/계좌번호/예금주, `bankInfo` 하드코딩) + 입금자명 입력란. ⚠️ 입력란이 `value`/`onChange` 없는 비활성 상태 — 프론트 미구현(기존 확정 TODO) |

### ③ 필요한 API 목록

1. **입금자명 등록** — `StepComplete.tsx` 진입 직후(신청 생성 API 완료 후 별도 호출)

### API 1 / 1 — 입금자명 등록/수정 ⚠️ 확인필요 — `StepComplete.tsx`에 입력란은 있으나 `value`/`onChange` 없는 비활성 상태

#### ④ Request/Response 설계

```
PATCH /api/applications/{applicationId}/payment
Cookie: accessToken={JWT}
Content-Type: application/json
```
```json
{ "depositorName": "홍길동" }
```

✅ 2026-07-29 확정: **멱등(Upsert) — `Payment` row가 없으면 생성, 있으면 `depositor_name` 덮어쓰기.** 오타 등으로 확인 전까지는 자유롭게 재호출 가능.

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "applicationId": 1,
    "depositorName": "홍길동",
    "updatedAt": "2026-07-29T10:10:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `depositorName` 누락/공백 | `INVALID_INPUT` | 400 |
| `applicationId`가 존재하지 않음 | `NOT_FOUND` | 404 |
| `applicationId`가 로그인한 유저의 신청이 아님 | `FORBIDDEN` | 403 |
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.payment_status = CONFIRMED`(이미 입금 확인됨) 이후 호출 | `PAYMENT_ALREADY_CONFIRMED`(신규 코드) | 409 |

✅ 2026-07-29 확정: **관리자가 입금 확인(`payment_status=CONFIRMED`) 이후엔 잠금** — 기존 `ShippingAddress.is_locked`(배송 시작 후 배송지 잠금)와 같은 철학. 확인 전까지는 계속 수정 가능.

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| (path) applicationId | Payment.application_id |
| depositorName | Payment.depositor_name |
| — | Payment.confirmed_at = NULL (관리자 확인 전) |
| — | Application.payment_status는 그대로 `WAITING` 유지(입금자명 등록만으로는 변경하지 않음). 관리자 입금 확인 시 `payment_status`만 `WAITING→CONFIRMED`로 바뀌고 Application.status는 `SUBMITTED`를 유지한다. 자동 취소 후 늦은 입금 확인이면 `CANCELLED`를 유지한다. |

#### ⑦ 누락된 필드 확인

없음.

**API 1 완료.**

---

## Payment 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `PATCH /api/applications/{applicationId}/payment` (입금자명 등록/수정) | 설계 완료 |

**프론트 반영 필요 항목:**
- `StepComplete.tsx`의 입금자명 입력란을 `value`/`onChange` 연결해서 실제로 이 API를 호출하도록 구현 필요

**남은 TODO:**
- `PAYMENT_ALREADY_CONFIRMED` 에러코드는 기존 `ErrorCode.java`에 없음 — 신규 추가 필요(구현 단계에서 처리)
- 관리자가 입금을 확인 처리하는 API는 Admin 도메인에서 다룸

---
Payment 도메인 완료.

---
