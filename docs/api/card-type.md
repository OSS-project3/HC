### API 1 / 2 — 카드 종류 관리 (관리자) ⚠️ 확인필요 — 관리자 화면 자체가 프론트에 없음(신규)

> ⚠️ **정정(2026-08-25): 아래 API는 미구현 설계다.** `CardTypeController` 백엔드 컨트롤러가 **존재하지 않는다**(코드 대조 확인). 카드 종류는 코드/시드로만 관리되며 관리자 CRUD API·프론트 화면 모두 없음. "설계 완료"가 아니라 "설계만 있고 미구현"이다.

#### ④ Request/Response 설계

**등록**
```
POST /api/admin/card-types
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: application/json
```
```json
{ "code": "HONOR_KOREAN", "name": "명예한국인증", "description": "...", "price": 30000 }
```
✅ 2026-07-31 신규: `code`(`HONOR_KOREAN`/`HONOR_CITIZEN`/`VISITOR`/`STUDENT`) 추가 — 학생증 전용 로직(학번/학과/학교로고·직인 노출 등)이 관리자가 자유롭게 바꿀 수 있는 `name` 문자열이 아니라 이 고정 코드값으로 분기되게 하기 위함(`.md` 4.1절).

**Response `201 Created`**
```json
{ "success": true, "data": { "cardTypeId": 1, "code": "HONOR_KOREAN", "name": "명예한국인증", "price": 30000, "isActive": true } }
```

**목록 조회**
```
GET /api/admin/card-types
```
**Response `200 OK`**
```json
{ "success": true, "data": [ { "cardTypeId": 1, "code": "HONOR_KOREAN", "name": "명예한국인증", "price": 30000, "isActive": true } ] }
```

**수정**
```
PATCH /api/admin/card-types/{cardTypeId}
```
```json
{ "price": 35000, "isActive": true }
```
**Response `200 OK`** — 등록 응답과 동일 형태

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `name` 누락, 또는 이미 존재하는 `name`(UNIQUE) | `INVALID_INPUT` | 400 |
| `code` 누락(등록 시), 또는 이미 존재하는 `code`(UNIQUE) | `INVALID_INPUT` | 400 |
| `cardTypeId` 없음(수정 시) | `NOT_FOUND` | 404 |
| 비로그인 | `UNAUTHORIZED` | 401 |

⚠️ `price` 변경은 **이미 신청된 건에 영향 안 줌** — `Application.total_price`는 신청 시점 스냅샷이라 이 API로 가격을 바꿔도 기존 신청 금액은 그대로 (2.5절 가격 정책과 일치)
⚠️ 2026-07-31 신규: `code`는 등록 후 **수정 API에서 변경 불가**로 설계(카드종류별 비즈니스 로직이 이 값에 고정 연결되므로) — 바꾸고 싶으면 새 CardType을 등록해야 함

#### ⑥ DB 컬럼과 매핑 검증

| Request/Response | CardType 컬럼 |
|---|---|
| code | code (✅ 2026-07-31 신규) |
| name | name |
| description | description |
| price | price |
| isActive | is_active |

#### ⑦ 누락된 필드 확인

없음 — `.md` `CardType` 컬럼과 1:1.

**API 1 완료.**

---
