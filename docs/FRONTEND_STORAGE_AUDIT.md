# 프론트엔드 클라이언트 저장소(localStorage/sessionStorage) 데이터 유출 점검 (2026-08-25)

브라우저 저장소(localStorage·sessionStorage·cookie)에 개인정보(PII)가 남아 유출될 수 있는 지점을 전수 점검한 결과.
**이 문서는 점검·정리용이며 코드 수정은 포함하지 않는다**(수정은 별도 진행). 백엔드/API 변경 없음.

- 조사 방법: `frontend/src` 전체에서 `localStorage` / `sessionStorage` / `document.cookie` / `indexedDB` 사용처 grep 후 각 쓰기·읽기 경로 확인.
- 인증 토큰(access/refresh)은 **HttpOnly 쿠키(서버)** 로 관리되어 JS(localStorage)에 노출되지 않음 — 점검 대상 외(양호).

---

## 1. 요약 (위험도)

| 키 | 저장소 | PII | 지속성 | 위험 | 상태 |
|---|---|---|---|---|---|
| `admin-applications` | localStorage | ✅✅ 민감(신청 전체) | 영구 | 🔴 **높음** | 미조치 |
| `customer-inquiries` | localStorage | ✅ 문의자 | 영구 | 🔴 **높음** | 미조치 |
| `auth-user` | localStorage | ✅ 이름/이메일/전화/주소 | 영구(로그아웃 시 삭제) | 🟠 중 | 미조치 |
| `application-draft` | sessionStorage | ✅ 작성 중 신청서 | 세션(탭 닫으면 삭제) | 🟡 낮음 | 설계상 허용 |
| `last-application-lookup` | sessionStorage | 마스킹된 이름 | 세션 | 🟡 낮음 | 허용 |
| `managed-content:*` | localStorage | ❌ 공개 콘텐츠 | 영구 | ⚪ 낮음 | 허용 |
| `site-language` | localStorage | ❌ | 영구 | ⚪ 없음 | 허용 |
| (작명) `admin:*` | — | — | — | ✅ 제거됨 | **조치 완료** |

---

## 2. 위험 높음 — 즉시 정리 권장

### 2.1 `admin-applications` (localStorage) 🔴
- 정의/입출력: `data/adminMock.ts:133`(load), `:139`(save).
- **쓰기(유출원)**: `pages/ApplyPage/ApplyPage.tsx:86` — 신청 제출 시 `saveApplications([...])`.
- 저장 데이터: `applicantName`, `applicantEmail`, `phone`, 그리고 **`detail` 객체 전체** —
  영문명·국적·출생지·생년월일·출생시간·성별·한국입국일·학교명·학번·학과·**수령인 이름/전화/주소**까지.
- 읽기: `MyPage.tsx:39`, `LookupPage.tsx:123`.
- **문제**: 같은 화면에서 **실제 백엔드 제출도 이미 수행**한다(`ApplyPage.tsx:130 api.createApplication`).
  즉 localStorage 저장은 **불필요한 중복**인데, 신청자 전원의 민감 PII가 브라우저에 **영구** 남는다.
  공용 PC에서는 다음 사용자가 DevTools 또는 마이페이지류 조회로 이전 신청자들의 PII를 볼 수 있다.
- 권장(미구현): `saveApplications`/`loadApplications` 호출 제거, 관리자 목록은 실제 API(`/api/admin/applications`)로만.
  `data/adminMock.ts`는 데드코드화 후 제거.

### 2.2 `customer-inquiries` (localStorage) 🔴
- 정의/입출력: `data/inquiries.ts:24/28/34`.
- **쓰기(유출원)**: `pages/InquiryPage/InquiryPage.tsx:36` — 문의 제출 시 `saveInquiries([record, ...])`.
- 저장 데이터: `name`, `email`, `phone`, `title`, `content`(+답변).
- 읽기: `MyPage.tsx:38`, `InquiryDetailPage`.
- **문제**: 공개 문의 폼이 **백엔드로 전송하지 않고 localStorage에만** 저장한다
  (프론트 `api.ts`에 공개 `createInquiry` 없음; 존재하는 백엔드 `POST /api/inquiries` 미연동).
  → 문의자 PII가 브라우저에만 영구 잔존하고, 실제 관리자에게 전달되지도 않는다(기능적 결함 겸 유출).
- 권장(미구현): 제출을 실제 API로 전환하고 localStorage 저장 제거. `data/inquiries.ts` 제거.

---

## 3. 중간 — 검토 권장

### 3.1 `auth-user` (localStorage) 🟠
- 위치: `features/auth/AuthContext.tsx:35`(read), `:64`(write), `:65`(logout 시 removeItem).
- 저장 데이터: `{ name, email, role, source, phone?, address? }` — 로그인 사용자 PII.
- 지속성: 로그인 상태로 영구, **로그아웃 시 삭제됨**(`:65`)이라 부분 완화.
- 참고: 현재 mock 로그인 구조라 서버 세션과 별개로 클라이언트에 PII를 둔다.
- 권장(미구현): 표시에 꼭 필요치 않은 `phone/address`는 저장 제외하거나 세션 한정. 실제 로그인 전환 시 재검토.

---

## 4. 낮음/허용 — 조치 불필요(기록만)

- `application-draft` (**sessionStorage**, `features/apply/useApplicationDraft.ts:17/29/49`): 작성 중 신청서 PII를 담지만
  **세션 저장(탭 닫으면 삭제) + 제출 시 clear + 파일 내용 미저장**으로 의도된 설계. 유지 무방.
- `last-application-lookup` (**sessionStorage**, `pages/MobileCardPage/MobileCardPage.tsx:12`): 조회 결과. 이름은 **마스킹**. 세션 한정.
- `managed-content:*` (localStorage): `components/admin/ContentAdminPanel.tsx:14`, `pages/EventsPage/EventsPage.tsx:26`,
  `data/eventFeedPosts.ts:86/96`. 관리자 편집 **공개 콘텐츠**(행사 프로그램/피드)라 PII 아님. (다만 이 역시 실제 API로 옮기는 게 바람직.)
- `site-language` (localStorage, `features/i18n/LanguageContext.tsx:16/17`): 언어 설정. 비민감.

---

## 5. 이미 정리된 부분 (참고)

- 관리자 작명 화면의 확정 이름·선택 이력은 **localStorage에서 백엔드(DB)로 이전 완료**
  (`admin:member-chosen-names`, `admin:name-selection-counts` **제거**). 커밋 `b763cc9`.
- 자세한 내용: [`specs/admin-dashboard/STATUS.md`](./specs/admin-dashboard/STATUS.md) · [`specs/admin-dashboard/BACKEND_TODO.md`](./specs/admin-dashboard/BACKEND_TODO.md).

---

## 6. 결론 / 권장 우선순위 (문서상 제안 — 미구현)

1. 🔴 `ApplyPage`의 `saveApplications`(localStorage) **쓰기 제거** — 실제 API 제출은 이미 있으므로 중복만 제거하면 됨. (`admin-applications` 유출 차단)
2. 🔴 `InquiryPage` 제출을 실제 API로 전환하고 `saveInquiries`(localStorage) 제거. (`customer-inquiries` 유출 차단 + 기능 정상화)
3. 🟠 `auth-user`에서 `phone/address` 저장 축소 검토.
4. ⚪ `managed-content:*`도 장기적으로 실제 API로 이관(공개 콘텐츠라 우선순위 낮음).

> 위 1·2를 제거하면 **공개 사용자 흐름에서 PII가 브라우저에 영구 저장되는 경로가 사라진다**.
> sessionStorage 두 건은 세션 한정이라 유지 가능.
