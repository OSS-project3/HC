## UploadFile 도메인

> 진행 순서: 남은 도메인 중 의존관계가 제일 상위인 것부터 갑니다. `Application`(logo/seal/submit_file_id), `CardDesign`(preview/template), `Review`가 전부 `UploadFile`을 참조하고, `UploadFile`은 아무것도 참조하지 않으므로 이게 다음입니다.
>
> ⚠️ 2026-08-06 정정: `Review`가 참조하는 방식이 바뀌었습니다. 후기 요구사항이 "사진 0개 이상 다중 첨부"로 확정되면서 애초에 이 문서에 적혀있던 "`Review.thumbnail_file_id`"(단일 FK 컬럼) 가정은 폐기되었고, 대신 `ReviewImage`(review_id + upload_file_id + display_order) join Entity가 N장을 연결합니다. 자세한 내용은 [`docs/specs/review/data-model.md`](../specs/review/data-model.md) §5 참고. `UploadFile` 자체가 "아무것도 참조하지 않는 공용 메타데이터 테이블"이라는 원칙은 그대로 유지됩니다.

### ① 도메인의 책임

업로드된 파일(사진/엑셀/ZIP/카드이미지)의 메타데이터와 저장 경로를 관리한다. (`.md` 3절 기준)

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `FileUploadBox.tsx` | 파일 선택 시 `URL.createObjectURL()`로 미리보기만 생성. **서버 업로드 호출이 전혀 없음** |
| `types.ts`의 `UploadFileInfo` | `{ name, size, previewUrl }` — ⚠️ **실제 `File` 객체(바이트)를 어디에도 안 들고 있음.** 파일 선택 즉시 메타데이터만 남고 원본 파일 참조는 버려짐 |
| `useApplicationDraft.ts` | sessionStorage 저장 시 파일은 `{name, size}`만 남기고 의도적으로 제외(주석에 명시) |

⚠️ **발견한 것**: 지금 프론트는 파일을 "나중에 한 번에 진짜로 전송"할 수 있는 구조가 아예 아닙니다 — 선택 즉시 미리보기만 만들고 실제 `File` 객체를 버립니다. Application 도메인에서 제가 "로고/직인/제출ZIP을 신청 생성 API에 멀티파트로 같이 보낸다"고 설계했는데, 이게 되려면 **`FileUploadBox`/`ApplicationDraft`가 실제 `File` 객체를 최종 제출 시점까지 들고 있도록 먼저 고쳐야 합니다.** 이것도 `birth_date`/사진입력란과 같은 종류의 프론트 미구현 갭입니다.

### ③ 필요한 API 목록

지금 프론트 화면들을 다 훑어봤는데, **독립적인 "파일 업로드 API"가 필요한 지점이 없습니다.**
- 로고/직인/제출ZIP/개인사진 → 전부 Application 생성 API(개인/단체)에 멀티파트로 임베드되어 처리 (이미 설계 완료)
- `CardDesign`의 preview/template 이미지 → 관리자가 카드 디자인을 등록하는 화면 자체가 프론트에 없음(`DesignPage.tsx`는 읽기 전용, `cards.ts` 정적 데이터를 그대로 보여줄 뿐)
- `Review`의 첨부 사진(`ReviewImage`, 0~N장) → `POST /api/reviews`(후기 등록) 요청에 `photos` 멀티파트로 함께 실어서 그 자리에서 `UploadFile`+`ReviewImage` row가 만들어짐(`docs/specs/review/api.md` API 1 참고) — 사전 업로드 엔드포인트 불필요라는 결론은 동일

즉 "먼저 업로드해서 fileId를 받고, 그 fileId를 다른 API에 넣는" 방식의 **사전 업로드 전용 엔드포인트가 필요한 화면이 하나도 없습니다.** 파일은 전부 그 파일을 쓰는 도메인의 생성 API에 같이 실려서 그때 `UploadFile` row가 만들어지는 구조입니다(이미 Application API 1/2 설계에 반영됨).

### UploadFile 도메인 정리

| # | API | 상태 |
|---|---|---|
| — | (없음) | 독립 API 불필요 — 각 도메인 생성 API에 임베드 |

**프론트 반영 필요 항목(신규 발견):**
- `FileUploadBox.tsx`/`ApplicationDraft`가 실제 `File` 객체를 최종 제출 시점까지 보관하도록 구조 변경 필요 — 지금은 메타데이터만 남기고 파일 자체를 버림. 이게 안 고쳐지면 Application 생성 API(로고/직인/제출ZIP/사진 멀티파트 전송)가 애초에 동작할 수 없음.

✅ 2026-07-29 확인: 위 결론(독립 API 불필요, `FileUploadBox` 구조 변경 필요) 확정.

---
UploadFile 도메인 완료.

---
