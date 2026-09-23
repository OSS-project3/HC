### API 2 / 2 — 카드 디자인 관리 (관리자) ⚠️ 확인필요 — 관리자 화면 자체가 프론트에 없음(신규)

> ⚠️ **정정(2026-08-25): 아래 API는 미구현 설계다.** `CardDesignController` 백엔드 컨트롤러가 **존재하지 않는다**(코드 대조 확인). 카드 디자인 배정 관리 API·프론트 화면 모두 없음.

#### ④ Request/Response 설계

**등록**
```
POST /api/admin/card-designs
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | `{ "cardTypeId": 1, "name": "명예한국인증 01", "orientation": "LANDSCAPE", "isDefault": false }` |
| `templateFront` | file | 앞면 빈 템플릿 → `CardDesign.template_front_id` (UploadFile 생성) |
| `templateBack` | file | 뒷면("이름풀이") 빈 템플릿 → `CardDesign.template_back_id` (UploadFile 생성) |

✅ 2026-07-31 확정(`시안.zip` 실물 확인): 템플릿 2장(앞/뒤) + `orientation` 필수로 정정. 명예시민증/명예한국인증=LANDSCAPE(83×55mm), 방문증=PORTRAIT(55×83mm) — 시안 자료로 실제 확인됨.

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "cardDesignId": 1,
    "cardTypeId": 1,
    "name": "명예한국인증 01",
    "orientation": "LANDSCAPE",
    "isDefault": false,
    "isActive": true
  }
}
```

**목록 조회**
```
GET /api/admin/card-designs?cardTypeId={optional}
```

**수정**
```
PATCH /api/admin/card-designs/{cardDesignId}
```
(`templateFront`/`templateBack` 파일 교체는 각각 선택 — 안 보내면 기존 템플릿 유지)

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `cardTypeId` 없음 | `NOT_FOUND` | 404 |
| `orientation` 누락/잘못된 값 | `INVALID_INPUT` | 400 |
| `templateFront`/`templateBack` 파일 형식 오류 | `UNSUPPORTED_FILE_TYPE` | 415 |
| 비로그인 | `UNAUTHORIZED` | 401 |

#### ⑥ DB 컬럼과 매핑 검증

| Request | CardDesign 컬럼 |
|---|---|
| cardTypeId | card_type_id |
| name | name |
| orientation | orientation |
| isDefault | is_default |
| templateFront(file) | UploadFile 생성 → template_front_id |
| templateBack(file) | UploadFile 생성 → template_back_id |

#### ⑦ 누락된 필드 확인

없음 — `시안.zip` 확인으로 전부 해결됨.

**API 2 완료.**

---

### API 3 — 카드 디자인 미리보기 (관리자, 2026-09-22 확정, 구현됨)

> 위 API 2(등록/수정)와 달리 이 API는 실제 `CardDesignController`/`CardAssetPreviewService`에
> 구현되어 있습니다. 등록/수정 API는 여전히 미구현이며 목록 조회(`GET /api/admin/card-designs`)만
> 기존에 구현되어 있었습니다.

```
GET /api/admin/card-designs/{cardDesignId}/preview
Cookie 또는 Authorization: accessToken={JWT}  (role=ADMIN 필요)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "cardDesignId": 1,
    "frontImageBase64": "...",
    "backImageBase64": "..."
  }
}
```

- 일반 카드(HONOR_KOREAN/HONOR_CITIZEN/VISITOR): `classpath:card-templates/{CardType.code}/{designNumber}/`
  아래 앞면(`앞면.png`→`대지 1.png` 순 탐색)·뒷면(`뒷면.png`→`대지 1 사본.png` 순 탐색) 원본을 그대로 base64 인코딩.
- 학생증(STUDENT): `CardDesign.templateFrontId`/`templateBackId` → `UploadFile.filePath` → `StorageService.download()`.
- 렌더링(합성)이 아니라 원본 파일 바이트만 반환하므로 `CardImageCompositor`는 호출하지 않음.

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| 비로그인 | `UNAUTHORIZED` | 401 |
| `cardDesignId` 없음 / `active=false` | `CARD_DESIGN_NOT_FOUND` | 404 |
| 검증되지 않은 방문증 디자인 1 | `CARD_DESIGN_NOT_FOUND` | 404 |
| 학생증 템플릿 참조 없음 또는 참조된 `UploadFile` 행 유실 | `CARD_DESIGN_NOT_FOUND` | 404 |

읽기 전용 — 신청·디자인·파일 데이터나 `AdminActivityLog`를 변경하지 않음.

**십이간지 디자인 세트 미리보기**는 카드 디자인이 아니라 `ApplicationMember.zodiacDesignSet` 값을
미리 보기 위한 별도 API이며, `docs/specs/application/admin-saju.md`
"십이간지·카드 디자인 선택 이미지 미리보기" 절에 계약이 정리되어 있습니다.

```
GET /api/admin/zodiac-designs/{designSet}/preview
```

**API 3 완료.**

---
