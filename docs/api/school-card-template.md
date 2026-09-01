### 학교별 학생증 카드 템플릿 (관리자, 4-D, 2026-09-01 구현 완료)

> 학생증(STUDENT)은 학교마다 디자이너가 완성된 앞/뒤 이미지를 통째로 다르게 제공하는 구조라(다른
> 3종 카드의 "공유 빈 템플릿" 구조와 다름), `docs/api/card-design.md`가 설계한 범용
> `POST/PATCH /api/admin/card-designs`(아직 미구현)와는 별개의, 학교(`schoolId`)로 스코프된 전용
> API다. 기존 3종 카드는 이 API와 무관하게 계속 classpath 리소스로 배포한다(TODO.md "4-D" 참고).
> 이번 구현은 백엔드 API까지만 — 실제 관리자 업로드 화면(미리보기+파일변경 버튼)은 프론트 스코프.

#### 조회 — 현재 등록된 템플릿

```
GET /api/admin/schools/{schoolId}/card-template?orientation=LANDSCAPE|PORTRAIT
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```

**Response `200 OK`(등록된 템플릿이 있을 때)**
```json
{
  "success": true,
  "data": {
    "cardDesignId": 1,
    "frontPreviewUrl": "https://...(presigned, 1시간 만료)",
    "backPreviewUrl": "https://...(presigned, 1시간 만료)"
  }
}
```

**Response `200 OK`(등록된 템플릿이 없을 때)** — 에러가 아니다. 신규 학교는 항상 이 상태로 시작하므로
관리자 화면이 정상적으로 매번 마주치는 상태다.
```json
{ "success": true }
```
(`data` 키 자체가 없다 — `ApiResponse`가 `@JsonInclude(NON_NULL)`이라 null data는 키가 생략된다.
프론트에서 `response.data`를 읽으면 `undefined`이므로 존재 여부 확인 로직은 동일하게 동작한다.)

#### 등록·교체 — 앞/뒤 템플릿 업로드

```
POST /api/admin/schools/{schoolId}/card-template
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `orientation` | 폼 필드 | `LANDSCAPE` \| `PORTRAIT`, 필수 |
| `front` | file | 앞면 템플릿, 필수 |
| `back` | file | 뒷면 템플릿, 필수 |

앞/뒤를 한 번에 같이 받는다 — 한쪽만 교체하는 흐름은 없다. 이 `schoolId`+`orientation`에 이미 활성
`CardDesign`이 있으면 그 템플릿만 교체(`CardDesign.id`는 유지 — 이미 이 디자인으로 생성된 멤버
카드는 영향 없음), 없으면 신규 생성한다.

**Response `200 OK`** — GET과 동일한 DTO(등록 직후 프론트가 재조회 없이 미리보기 갱신 가능).
```json
{
  "success": true,
  "data": {
    "cardDesignId": 1,
    "frontPreviewUrl": "https://...",
    "backPreviewUrl": "https://..."
  }
}
```

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `schoolId` 존재하지 않음 | `SCHOOL_NOT_FOUND` | 404 |
| `orientation` 누락 | `INVALID_INPUT` | 400 |
| `front`/`back` 파트 누락 | (Spring MVC `MissingServletRequestPartException`) | 400 |
| PNG가 아니거나 시그니처 불일치 | `UNSUPPORTED_FILE_TYPE` | 415 |
| 10MB 초과 | `FILE_TOO_LARGE` | 413 |
| 카드 비율(235:156 ±5%)에 안 맞거나 장변 800px 미만 | `CARD_TEMPLATE_INVALID_RESOLUTION` | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

해상도 기준(비율 ±5%, 최소 장변 800px)은 잠정값 — 실제 디자이너 산출물 기준으로 조정 가능
(`SchoolCardTemplateValidator` 참고).

#### 구현 메모

- `SchoolCardTemplateService`(비-transactional 오케스트레이션) + `SchoolCardTemplatePersistenceService`
  (`@Transactional`) 2-서비스 분리 — `CardGenerationService`/`CardGenerationPersistenceService`와
  같은 패턴(신규 파일 S3 선업로드 → DB 반영 → 커밋된 뒤에만 기존 파일 정리, 실패 시 신규 파일만 역순
  보상삭제).
- `UploadFileType.CARD_IMAGE`를 처음 실사용(템플릿 원본 — 렌더링 결과물인
  `ApplicationMember.cardFrontPath`/`cardBackPath`는 여전히 `UploadFile` row를 안 만듦, §3 정책 그대로).
- S3 key 네임스페이스: `card-templates/STUDENT/{schoolId}/{orientation}/{front|back}-{uuid}.png`.
- `CardDesign.designNumber`는 `student_card_design_seq` DB 시퀀스로 채번(`schema.sql`).
- 같은 `schoolId`+`orientation` 활성 디자인 1개 불변조건은 서비스 레벨 조회-후-분기 + DB unique
  index(`card_designs_school_orientation_idx`, `schema.sql`) 이중 방어.

---
