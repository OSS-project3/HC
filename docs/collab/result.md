# 관리자 신청 후속 프로세스 정합성 분석 결과

> 분석 기준: `docs/collab/CHECK.md` 및 2026-08-31 현재 `D:\HC-worktrees\main-preview` 작업 트리
>
> 범위: 사용자 신청 완료 이후 관리자 검토 → 만세력 확정 → 작명 → 카드 미리보기·생성 → 제작·발급
>
> 주의: 이 보고서는 원격 `origin/main`이 아니라 로컬 작업 트리의 실제 코드와 문서를 기준으로 한다. 분석 과정에서 코드와 기존 문서는 수정하지 않았다.

## 결론

프론트엔드와 백엔드는 신청 목록·상세 조회, 결제 확인, 검토 시작, 사진 반려, 작명 승인, 작명 완료, 제작 시작, 카드 준비 완료라는 **상위 상태 이름과 기본 전이 순서**는 대체로 동일하게 이해하고 있다.

그러나 관리자 업무를 처음부터 끝까지 실제 수행할 수 있는 수준으로는 아직 일치하지 않는다. 핵심 차단 요소는 다음 다섯 가지다.

1. 프론트 작명 화면은 백엔드 만세력 확정 API를 호출하지 않고 브라우저 로컬 계산 및 mock fallback·mock 이름 추천을 사용한다.
2. 프론트 이름 확정 요청에는 `surname`이 없지만, 백엔드는 `completeNaming()`에서 모든 Member의 성씨·이름·의미를 필수로 요구한다. 현재 UI만으로는 정상적인 작명 완료가 불가능하다.
3. 백엔드 카드 디자인 조회·미리보기·생성 API는 구현되어 있지만 프론트 관리자 화면에 호출 코드와 UI가 없다.
4. 프론트는 카드가 생성되지 않아도 `PRODUCTION_READY → PRODUCING`, 이후 카드 파일이 없어도 `card-ready`를 호출할 수 있고, 백엔드도 전체 Member 카드 생성 완료를 선행조건으로 검증하지 않는다.
5. `admin-saju.md` 안에 “별도 saju 시스템·Excel 왕복” 정책과 “HC 백엔드 ManseryeokResult 저장” 정책, 전체 비동기 생성과 Member 단위 동기 생성 정책이 동시에 남아 있어 문서 자체가 단일 Source of Truth 역할을 하지 못한다.

따라서 현재 상태는 **신청 검토 기본 전이: 연결 가능**, **만세력·작명: 부분 연결 및 계약 불일치**, **카드 제작: 백엔드 일부 구현·프론트 미연결·상태 안전장치 미완성**으로 판정한다.

---

## A. 전체 프로세스 맵

| 단계 | 프론트 실제 동작 | 백엔드 실제 동작 | 판정 |
|---|---|---|---|
| 신청 접수 | 실제 개인/단체 신청 API 호출 | `SUBMITTED + WAITING` 생성 | 대체로 일치 |
| 관리자 목록·상세 | 실제 API로 목록·상세·Member 조회 | 관리자 목록·상세·Member API 제공 | 연결됨. 단, 상세 필드 부족 |
| 입금 확인 | `confirm-payment` 호출 버튼 존재 | `PaymentStatus`만 `CONFIRMED`로 변경, 상태는 `SUBMITTED` 유지 | 일치 |
| 검토 시작 | `SUBMITTED + CONFIRMED`에서 `start-review` 호출 | 같은 조건에서 `REVIEWING` 전이 | 일치 |
| 사진 반려·재검토 | 반려 API 버튼 존재 | `REVIEWING → PHOTO_REJECTED`, 재업로드 후 `REVIEWING` | 기본 흐름 일치 |
| 작명 승인 | `REVIEWING`에서 `approve-naming` 호출 | `REVIEWING → NAME_EDITING` | 일치 |
| 만세력 시간 확정 | 브라우저 `manseryeok` 직접 계산. 실패 시 mock 사용 | 출생지역 검색, timezone/DST resolve, 확정 결과 저장·조회 API와 이력 Entity 존재 | **불일치/미연결** |
| 이름 추천 | `mockRecommendations()`로 임의 후보 생성 | 확정 문서는 이름 사전·점수·결정적 정렬 요구 | **불일치** |
| 이름 확정 | 이름·한자·훈음·의미만 저장 요청 | 성씨·이름·한자·훈음·의미 저장 가능 | **성씨 누락** |
| 작명 완료 | UI에서 `complete-naming` 호출 | 모든 Member의 성씨·이름·의미를 집계 검증 후 `PRODUCTION_READY` | **현재 UI로 완료 불가** |
| 디자인 조회 | UI/호출 없음 | 일반 카드와 학생증 학교·방향 기반 조회 API 존재 | **프론트 미연결** |
| 카드번호 | 단건 및 일괄 저장 API 호출 함수 존재 | 단건/사진번호 기준 일괄 저장 지원 | API 함수 존재, 실제 화면 흐름은 추가 확인 필요 |
| 카드 미리보기 | UI/호출 없음 | Member 단위 앞·뒤 base64 PNG 미리보기 API 존재 | **프론트 미연결** |
| 카드 생성·저장 | UI/호출 없음 | Member 1명 단위 동기 생성, S3 저장, DB 연결, 재생성 보상 처리 존재 | **프론트 미연결** |
| 제작 시작 | `PRODUCTION_READY`이면 즉시 버튼 노출 | 카드 파일 완성 집계 없이 `PRODUCING` 전이 | **운영 차단/무결성 허점** |
| 카드 준비 완료 | `PRODUCING`이고 `cardReadyAt`이 없으면 버튼 노출 | 전체 Member 파일 확인 없이 모바일은 `COMPLETED`, 실물은 `PRODUCING` 유지 | **운영 차단/무결성 허점** |
| 실물 발송 | tracking number 입력 후 dispatch 호출 | `MOBILE_AND_PHYSICAL`만 `COMPLETED` 전이 | 기본 흐름 일치 |

실제 목표 흐름과 현재 연결 상태는 다음과 같다.

```text
SUBMITTED + WAITING
  → 입금 확인                                   [연결됨]
SUBMITTED + CONFIRMED
  → REVIEWING                                  [연결됨]
  → PHOTO_REJECTED → 재업로드 → REVIEWING       [기본 연결됨]
  → NAME_EDITING                               [연결됨]
  → timezone/DST 확정 및 ManseryeokResult 저장  [백엔드만 구현, 프론트 미연결]
  → 이름 추천                                  [프론트 mock, 확정 계약 미구현]
  → 성씨·이름·의미 확정                         [성씨 UI/요청 누락]
  → PRODUCTION_READY                           [현재 프론트 입력만으로 차단]
  → 디자인 선택·미리보기·카드 생성               [백엔드만 구현, 프론트 미연결]
  → PRODUCING                                  [선행조건 없이 호출 가능]
  → 카드 준비/실물 발송 → COMPLETED              [카드 파일 선행조건 없음]
```

---

## B. 완전히 또는 대체로 일치하는 부분

| 항목 | 근거 | 판정 |
|---|---|---|
| 상태 enum | 프론트 `frontend/src/services/api.ts:120` 부근과 백엔드 `ApplicationStatus`가 `SUBMITTED, REVIEWING, PHOTO_REJECTED, NAME_EDITING, PRODUCTION_READY, PRODUCING, COMPLETED, CANCELLED`로 동일 | 일치 |
| 결제 상태 분리 | 백엔드 `Application.confirmPayment()`는 신청 상태를 변경하지 않고 `CONFIRMED`만 기록 (`Application.java:281`), 프론트도 이후 별도 `start-review`를 호출 | 일치 |
| 검토 시작 조건 | 프론트는 `SUBMITTED + CONFIRMED`에서 검토 시작 버튼을 제공하고, 백엔드는 `Application.startReview()`에서 같은 조건을 강제 (`Application.java:300`) | 일치 |
| 기본 관리자 전이 API | 프론트 `api.ts:229-236`과 백엔드 `AdminApplicationController:191-253`의 입금확인·검토시작·작명승인·작명완료·제작시작·카드준비·반려·발송 경로가 대응 | 일치 |
| 작명 완료 집계 | 백엔드 `ApplicationService.completeNaming()`은 전체 Member의 성씨·이름·의미 누락을 `NAMING_INCOMPLETE`로 반환 (`ApplicationService.java:643-651`) | 백엔드 정책 구현 완료 |
| 카드 Preview 공통 검증 | `CardRenderPreparation`에서 관리자, 상태, Member 소속, 디자인, 작명, 카드번호, 발행처 자산, 만세력 연주를 공통 검증 | 백엔드 구현 완료 |
| Preview 응답 | `POST .../card-preview`가 앞·뒤 base64 PNG를 한 호출로 반환 (`AdminApplicationController.java:166`) | 현재 TODO의 최신 계약과 일치 |
| Member 단위 카드 생성 | `POST .../card-generate` 및 S3/DB 저장 경로 존재 (`AdminApplicationController.java:178`) | 백엔드 최소 버전 구현 완료 |
| 학생증 디자인 조회축 | `CardDesignService`가 학생증을 `application.schoolId + orientation`으로 조회하고, 미연결 학교는 빈 목록 처리 | 확정 정책과 일치 |
| 카드 재생성 파일 순서 | 신규 S3 업로드 후 DB 확정, 성공 후 기존 파일 삭제 구조 | 최신 TODO 최소 버전과 일치 |

---

## C. 불일치 목록

| 단계 | 프론트 | 백엔드 | 문서 | 문제 | 심각도 | 우선 수정 주체 |
|---|---|---|---|---|---|---|
| 만세력 계산 | `computeMemberSaju()`로 생년월일·시간만 브라우저 계산, 실패 시 `mockSaju()` (`ApplicationsSection.tsx:400-403`) | timezone/DST resolve 및 확정 결과 저장·조회 API 존재 (`AdminApplicationController.java:132-160`) | HC가 확정 절대시각을 만들고 결과를 저장한다는 최신 TODO 존재 | 프론트가 백엔드 확정 시간·이력·정확도·활성 결과를 전혀 사용하지 않음 | BLOCKER | 프론트 우선, 필요 시 DTO 보강 |
| 출생시간 미입력 | 프론트 `saju.ts`가 12:00을 대입 | 백엔드·정책은 UNKNOWN으로 두고 확정 불가능한 주를 제외 | `admin-saju.md:165-168`은 임의 정오 금지 | 잘못된 시주·오행·추천을 확정값처럼 표시 가능 | BLOCKER | 프론트 |
| PARTIAL/UNKNOWN 표시 | 프론트 타입과 UI가 hour pillar가 항상 있다고 가정 | 백엔드는 `timeAccuracy`, `confirmedPillars`, `uncertainPillars` 반환 | 불확실한 주와 이유 표시 요구 | null/누락된 시주를 표시할 계약과 UI가 없음 | HIGH | 프론트 + 응답 계약 확인 |
| 이름 추천 | `mockRecommendations()` 사용, 무작위 후보 섞기 | 백엔드 이름 사전/결정적 추천 API 연결 확인 안 됨 | 점수 내림차순, 동점 사전 ID 오름차순, 무작위 금지 (`admin-saju.md:282-283`) | 같은 입력에서도 결과 재현 불가, 사전 밖 mock 가능 | BLOCKER | 백엔드 추천 API 확인/구현 + 프론트 연결 |
| 성씨 입력 | 요청 body가 `name, hanja, reading, meaning`뿐 (`api.ts:210`, `ApplicationsSection.tsx:415`) | `NameAssignRequest`는 surname을 받을 수 있고 `completeNaming()`에서 필수 | 성씨는 이름과 분리, 관리자 확정 | 정상 UI 사용만으로 `NAME_EDITING`을 끝낼 수 없음 | BLOCKER | 프론트 |
| 작명 완료 표시 | `assignedName`만 있으면 작명된 것으로 취급 | 성씨·이름·의미 전원 필수 | 같은 기준을 요구 | UI의 완료 표시와 서버 완료 기준이 다름 | HIGH | 프론트/응답 DTO |
| 작명 수정 상태 | 프론트 UI 상태 의존, 직접 API 호출 제한 없음 | `assignMemberName()`·`applyNamingResult()`가 Application 상태를 검사하지 않음 (`ApplicationService.java:481` 등) | 카드 제작 이후 작명 수정 금지 (`admin-saju.md:312`) | 카드 생성 후 DB 이름 변경으로 PNG와 DB 불일치 가능 | HIGH | 백엔드 |
| 관리자 상세 DTO | 화면 타입에 기본 신청 정보만 있음 (`api.ts:134-147`) | 동일하게 축약된 DTO만 반환 (`MyApplicationDetailResponse:19-43`, `AdminApplicationMemberResponse:17-29`) | 카드·학생증·작명 전체 정보를 소비해야 함 | 성씨·뜻·학생정보·사진·카드 파일·발급일자·디자인·만세력 상태를 화면이 알 수 없음 | BLOCKER | 백엔드 DTO/API |
| 디자인 선택 | 없음 | `GET /api/admin/card-designs` 구현 | 디자인 선택 후 Preview 요구 | 관리자 화면에서 다음 단계로 갈 수 없음 | BLOCKER | 프론트 |
| 카드 Preview | 없음 | `POST .../card-preview` 구현 | 앞·뒤 동시 미리보기 최신 계약 | API가 있어도 UI에서 호출 불가 | BLOCKER | 프론트 |
| 카드 생성 | 없음 | `POST .../card-generate` Member 단위 구현 | 최신 TODO는 Member 단위 동기 최소 버전 | 카드 파일을 실제로 생성할 UI가 없음 | BLOCKER | 프론트 |
| 제작 시작 선행조건 | `PRODUCTION_READY`이면 즉시 제작 시작 버튼 (`ApplicationsSection.tsx:316`) | 전체 Member 카드 생성 완료를 검사하지 않음 (`ApplicationService.startProducing():613-616`) | TODO 3-F에 미구현으로 명시 (`TODO.md:1322`) | 카드가 0장이어도 제작 상태로 이동 | BLOCKER | 백엔드 + 프론트 |
| 카드 준비 완료 선행조건 | `PRODUCING && !cardReadyAt`이면 즉시 버튼 (`ApplicationsSection.tsx:317`) | 전체 Member 앞·뒤 파일·수량·발급일자 검증 없음 | TODO 3-F에 미구현 | 빈/부분 카드 상태에서 사용자 다운로드·완료 상태가 열릴 수 있음 | BLOCKER | 백엔드 + 프론트 |
| 카드 생성 동시성 | 프론트 중복 실행 방지 UI 없음 | `Application` version은 있으나 `ApplicationMember` version/동시 생성 충돌 방어는 후속 TODO | 낙관적 락 보강이 보류됨 (`TODO.md:1323` 부근) | 같은 Member 동시 재생성 시 마지막 저장이 승리하고 S3 고아 가능 | HIGH | 백엔드, 프론트 보조 |
| 학생증 렌더링 | 관리자 UI 없음 | 디자인 조회는 지원하지만 `CardLayouts`·`CardImageCompositor`는 STUDENT layout이 없어 거절; 학생증 에셋은 작업 트리에 미완성 상태 | TODO 4-C 미완료 | 학생증은 Preview/생성까지 도달 불가 | BLOCKER(학생증) | 백엔드 후 프론트 |
| 발행처 문구 | UI/응답에 전용 값 없음 | `Application`/`CardMemberData`에 `issuerName`이 없음 | 신청 시 입력값을 카드에 표시한다는 정책 (`admin-saju.md:420`) | 로고·직인은 있어도 발행처 텍스트 출처가 구현되지 않음 | HIGH | 정책 확인 후 백엔드/신청 UI |
| 일반 개인 신청 주소 | 프론트 개인 Member payload에 `address`가 없음 | 비학생증 개인 신청은 `member.address`를 필수 검증 | 일반 카드 주소 표시 정책 | 실제 비학생증 개인 신청이 `INVALID_INPUT`으로 실패할 가능성 | BLOCKER | 프론트 또는 백엔드 계약 재정리 |
| 활성 만세력 재현성 | 프론트가 조회하지 않음 | Entity에는 `inputHash`가 있으나 `ManseryeokActiveResultResponse`에는 없음 (`ManseryeokResult.java:38`, 응답 DTO `:16-26`) | 입력 변경 후 stale 결과 구분 필요 | 소비자가 현재 Member 입력과 활성 결과가 같은 입력인지 확인하기 어려움 | MEDIUM/HIGH | 백엔드 DTO |
| PARTIAL/UNKNOWN 저장 검증 | 프론트 미연결 | EXACT는 offset/instant 재검증하지만 PARTIAL/UNKNOWN의 필드 조합은 요청값을 넓게 신뢰 | 불확실 주만 저장하고 단일 정답 instant를 두지 않는 정책 | 모순된 확정 결과를 저장할 여지 | HIGH | 백엔드 |
| 문서 시스템 경계 | 해당 없음 | 실제로 HC ManseryeokResult 구현 | `admin-saju.md:7-9,242-252`는 별도 saju/Excel 왕복, `data-model.md:164`와 TODO는 HC 저장 | 구현자가 서로 반대 아키텍처를 선택할 수 있음 | BLOCKER(문서) | 문서 SoT 정리 |
| 카드 생성 단위 | UI 없음 | Member 1명 동기 생성 | `admin-saju.md:390,431`은 전체 비동기, TODO `:1255,1555`는 Member 동기 최소 버전 | 확정/보류 정책이 한 문서 묶음에 혼재 | HIGH(문서) | 문서 SoT 정리 |

---

## D. API 연동 갭

### D-1. 백엔드에 있고 프론트가 호출하지 않는 API

| API | 백엔드 상태 | 프론트 상태 | 필요한 작업 |
|---|---|---|---|
| `GET /api/admin/birth-regions/search` | 구현 | 호출 없음 | 출생지 후보 검색 UI와 연결 |
| `POST /api/admin/applications/{id}/members/{memberId}/manseryeok/resolve` | 구현 | 호출 없음 | 로컬 생년월일 계산 전에 timezone/DST 확정 흐름 연결 |
| `POST /api/admin/applications/{id}/members/{memberId}/manseryeok` | 구현 | 호출 없음 | 계산 결과 확인 후 명시적 확정 저장 |
| `GET /api/admin/applications/{id}/members/{memberId}/manseryeok` | 구현 | 호출 없음 | active 결과·정확도·확정/불확실 주 표시 |
| `GET /api/admin/card-designs` | 구현 | 호출 없음 | Application/card type에 맞는 활성 디자인 조회 |
| `POST /api/admin/applications/{id}/members/{memberId}/card-preview` | 구현 | 호출 없음 | 발급일자·디자인·Member 선택 UI와 연결 |
| `POST /api/admin/applications/{id}/members/{memberId}/card-generate` | 구현 | 호출 없음 | 생성/재생성 확인 및 결과 표시 |

### D-2. 프론트가 호출하지만 계약이 불완전한 API

| API | 갭 |
|---|---|
| `POST .../members/{memberId}/name` | 프론트 body에 `surname`이 없어 서버 작명 완료 조건을 충족하지 못함 |
| `GET /api/admin/applications/{id}` | 디자인·공유 발급일자·학교·방향·발행처 등 후속 단계 필드가 없음 |
| `GET /api/admin/applications/{id}/members` | 성씨·의미·해석·학생정보·주소·사진경로·카드경로·발급일자·만세력 확정 상태가 없음 |
| `POST .../start-producing` | 서버가 카드 생성 완료를 선행 검증하지 않아 호출 가능 여부를 신뢰할 수 없음 |
| `POST .../card-ready` | 서버가 전체 Member 카드 파일 존재를 선행 검증하지 않음 |

### D-3. 필요한 신규 또는 확장 계약

1. 관리자 상세 응답 확장: Application의 `cardDesignId`, `cardIssueDate`, `orientation`, `schoolType`, `schoolId`, `schoolName`과 Member의 작명·학생·파일 필드.
2. 활성 만세력 응답에 `inputHash` 또는 서버가 계산한 `stale` 여부를 제공.
3. 이름 추천을 실제 이름 사전과 확정 만세력 결과에 연결하는 조회 계약. 현재 코드상 프론트 mock을 대체할 서버 계약이 확인되지 않는다.
4. 일반 개인 신청 주소의 단일 계약 확정: Member address를 프론트가 보내거나, 서버가 Applicant/Receiver 주소를 카드용 스냅샷으로 사용하도록 통일.
5. 발행처 텍스트가 실제 신청 입력이면 Application 요청·Entity·응답·렌더 데이터에 명시적으로 연결.

---

## E. enum 및 상태 전이 비교

### E-1. enum

| 구분 | 프론트 | 백엔드 | 결과 |
|---|---|---|---|
| ApplicationStatus | 8개 상태 동일 | 8개 상태 동일 | 일치 |
| PaymentStatus | `WAITING`, `CONFIRMED` | `WAITING`, `CONFIRMED` | 일치 |
| IssueType | `MOBILE`, `MOBILE_AND_PHYSICAL` | 동일 | 일치 |
| TimeAccuracy | 관리자 프론트 실제 모델·흐름 없음 | `EXACT`, `PARTIAL`, `UNKNOWN` | 프론트 누락 |

### E-2. 상태 전이

| 전이 | 프론트 버튼 | 백엔드 불변조건 | 결과 |
|---|---|---|---|
| `SUBMITTED+WAITING → SUBMITTED+CONFIRMED` | 있음 | 멱등 입금확인 | 일치 |
| `SUBMITTED+CONFIRMED → REVIEWING` | 있음 | 상태+결제 확인 | 일치 |
| `REVIEWING → PHOTO_REJECTED` | 있음 | 상태 확인 | 일치 |
| `PHOTO_REJECTED → REVIEWING` | 사용자 재업로드 흐름 | Entity 재검토 복귀 | 기본 일치 |
| `REVIEWING → NAME_EDITING` | 있음 | 상태 확인 | 일치 |
| `NAME_EDITING → PRODUCTION_READY` | 있음 | 전체 Member 성씨·이름·의미 | 프론트 필드 부족으로 실사용 실패 |
| `PRODUCTION_READY → PRODUCING` | 즉시 가능 | 카드 생성 완료 조건 없음 | 잘못 열려 있음 |
| `PRODUCING → cardReady` | 즉시 가능 | 카드 생성 완료 조건 없음 | 잘못 열려 있음 |
| `cardReady → COMPLETED` | 모바일은 즉시, 실물은 dispatch 후 | Entity 정책과 동일 | 전이 자체는 일치 |

추가 무결성 문제:

- 이름 저장·Excel 작명 결과 import는 `NAME_EDITING` 상태를 강제하지 않는다.
- 만세력 resolve/confirm도 Application이 작명 단계인지 검사하지 않는다.
- 카드 생성은 Preview와 달리 `PRODUCING && cardReadyAt == null`에서도 허용하는 최신 최소 버전을 따른다.
- 카드 생성 API 자체는 상태를 자동 전환하지 않으며 이는 최신 TODO와 일치한다.

---

## F. DTO 비교

### F-1. 관리자 Application 상세

현재 프론트 타입과 백엔드 응답은 서로 거의 동일하지만, 둘 다 후속 업무에 필요한 필드를 제공하지 않는다.

| 필요한 값 | Entity 존재 | 관리자 상세 응답 | 프론트 타입 | 영향 |
|---|---:|---:|---:|---|
| `cardDesignId` | 있음 | 없음 | 없음 | 확정 디자인·충돌·재생성 상태 표시 불가 |
| `cardIssueDate` | 있음 | 없음 | 없음 | Application 공유 발급일자 표시 불가 |
| `orientation` | 있음 | 없음 | 없음 | 학생증 디자인 조회축 구성 불가 |
| `schoolType`, `schoolId`, `schoolName` | 있음 | 없음 | 없음 | 학생증 학교 연결 및 미등록 학교 차단 사유 표시 불가 |
| `issuerName` | 없음 | 없음 | 없음 | 발행처 문구 렌더 출처 없음 |

### F-2. 관리자 Member

현재 응답은 `memberId, englishName, nationality, gender, birthDate, birthTime, birthRegion, assignedName, assignedHanja, photoNumber, cardNumber`만 제공한다 (`AdminApplicationMemberResponse.java:17-29`).

| 필요한 값 | Entity 존재 | 응답 | 영향 |
|---|---:|---:|---|
| `surname` | 있음 | 없음 | 작명 완료 여부와 카드 전체 이름 표시 불가 |
| `nameMeaning`, `nameInterpretation` | 있음 | 없음 | 필수 의미 누락 확인 및 뒷면 Preview 설명 불가 |
| `address` | 있음 | 없음 | 카드 표시 주소 검토 불가 |
| `studentId`, `department` | 있음 | 없음 | 학생증 Preview 입력 검토 불가 |
| `photoPath` | 있음 | 없음 | 관리자 사진 검토/카드 입력 확인 불가 |
| `issueDate` | 있음 | 없음 | 카드 생성 결과 확인 불가 |
| `cardFrontPath`, `cardBackPath` | 있음 | 없음 | 생성 완료·재생성 여부 표시 불가 |
| 활성 `ManseryeokResult` 요약 | 별도 Entity | 없음 | 확정 여부·정확도·stale 여부 표시 불가 |

### F-3. 이름 확정 요청

백엔드는 성씨까지 받을 수 있지만 프론트 타입은 성씨를 제외한다. `meaning`은 프론트 타입상 optional이지만 백엔드 Bean Validation은 필수다. TypeScript 단계에서도 `surname`과 `meaning`을 필수로 맞추는 것이 안전하다.

### F-4. 카드 DTO

- Preview 요청은 `cardDesignId`, `issueDate`만 받고 앞·뒤를 함께 반환한다. 현재 코드와 TODO 최신 항목은 일치한다.
- `admin-saju.md:460`의 `side` 필드는 폐기된 계약이므로 stale 문구다.
- Generate 응답은 `cardFrontPath`, `cardBackPath`, `issueDate`를 반환하지만 관리자 상세/Member 재조회 응답에는 이 값이 없어 화면 복구 시 상태를 재구성할 수 없다.

---

## G. 구조적 프로세스 불일치

### G-1. 만세력 Source of Truth가 화면에서 우회됨

백엔드 구조는 `출생지 후보 → timezone/DST resolve → 프론트 순수 계산 → confirm 저장 → active 결과 조회`를 전제로 한다. 그러나 현재 관리자 화면은 백엔드 resolve/confirm/get을 모두 건너뛰고 `birthDate + birthTime`만으로 로컬 계산한다. 시간 미입력은 정오로 보정하며, 계산 실패는 mock으로 대체한다. 따라서 카드 렌더러가 사용하는 DB active 연주와 관리자 화면이 보고 이름을 고른 사주가 서로 다를 수 있다.

### G-2. 이름 추천 계약이 구현 경계에서 사라짐

확정 정책은 이름 사전 데이터만 사용하고, 확정 가능한 만세력일 때 오행 점수로 정렬하며, 같은 입력·사전 버전에서 결과가 재현되어야 한다. 현재 관리자 화면은 mock 후보를 무작위로 섞는다. 백엔드에는 이름 선택 저장과 통계는 있으나 이 확정 추천 계약을 프론트가 호출하는 흐름이 없다.

### G-3. 작명 완료와 카드 제작 사이의 안전장치가 없음

서버는 `completeNaming()` 시점에는 성씨·이름·의미를 확인하지만, 이후 이름 API가 다시 호출되는 것을 막지 않는다. 반대로 제작 시작 시에는 카드 디자인·발급일자·전 Member 카드 파일을 확인하지 않는다. 결과적으로 다음 두 불일치가 모두 가능하다.

```text
카드 생성 완료 → 이름 변경 → DB 이름과 PNG 불일치
카드 미생성/부분 생성 → startProducing/cardReady → 상태만 제작·완료
```

### G-4. Application 단위 정책과 Member 단위 최소 구현의 경계가 UI에 없음

디자인과 발급일자는 Application 전체 공유지만 생성 API는 Member 1명 단위다. 이 최소 버전은 가능하지만, UI가 다음을 명확히 해야 한다.

- 최초 Member 생성 시 Application 디자인·발급일자가 확정됨.
- 이후 Member는 같은 값만 사용할 수 있음.
- 전체 Member 생성률을 계산하고 전원 완료 전 제작 시작을 막음.
- 실패 Member만 재시도하되 기존 성공 Member를 불필요하게 재생성하지 않음.

현재 프론트에는 이 orchestration이 전혀 없다.

### G-5. 학생증은 디자인 조회만 공통화됐고 렌더러는 미완성

`CardDesign`과 조회 Service는 `schoolId + orientation` 정책을 반영했다. 하지만 `CardLayouts.FRONT/BACK`에 STUDENT 레이아웃이 없고 `CardImageCompositor`는 layout이 없으면 `INVALID_INPUT`을 발생시킨다. `CardMemberData`도 학생증용 학번·학과·학교명 필드가 없다. 따라서 학생증은 신청·검토·작명까지 기존 공통 흐름을 탈 수 있어도 Preview부터 차단된다.

### G-6. 문서 우선순위가 기능별로 명확하지 않음

`admin-saju.md` 초반은 HC에 만세력 Entity/API를 만들지 말라고 하지만 실제 코드와 `data-model.md`, 최신 TODO는 정반대다. 같은 문서 안에서 카드 전체 비동기 생성과 Member 단위 API, side 단건 Preview와 앞·뒤 동시 Preview가 혼재한다. 구현자는 날짜가 최신인 TODO를 사실상 따라야 하지만 공식 SoT 문서만 보고는 판별할 수 없다.

---

## H. 우선순위

### P0 — 다음 단계 진행을 막는 항목

1. **프론트 만세력 흐름 교체**: 로컬 직접 계산 시작 전에 백엔드 resolve를 호출하고, mock fallback을 확정·저장 경로에서 제거하며, confirm/get active를 연결한다.
2. **작명 요청 계약 수정**: 성씨 입력 UI와 `surname` 전송 추가, `meaning` 필수화, 서버 기준 완료 상태 표시.
3. **실제 이름 추천 계약 제공·연결**: 이름 사전+오행 점수+결정적 정렬을 만족하는 API/Service 존재 여부를 확정하고 mock 추천을 제거한다.
4. **관리자 상세 DTO 확장**: 작명 완료, 학생증, 카드 생성 상태를 판단할 최소 필드를 제공한다.
5. **카드 제작 UI 연결**: 디자인 조회, 발급일자, Member Preview, Generate/Regenerate, 생성 상태 표시.
6. **상태 전이 서버 가드**: 전 Member 카드 생성 완료 전 `startProducing`과 `markCardReady`를 거절한다.
7. **작명 API 상태 가드**: 이름 저장과 Excel import 모두 `NAME_EDITING`에서만 허용한다.
8. **일반 개인 신청 주소 계약 수정**: 현재 프론트 payload와 백엔드 필수 검증 충돌을 해소한다.
9. **문서 SoT 정리**: `admin-saju.md`의 superseded 아키텍처·Preview·비동기 문구를 현재 코드/정책과 분리한다.

### P1 — 기능은 진행할 수 있지만 데이터 정확도·운영 안정성에 영향

1. 활성 만세력 응답에 `inputHash` 또는 stale 판정 제공.
2. PARTIAL/UNKNOWN confirm 요청의 불변조건 서버 검증 강화.
3. Card Generation의 Member 동시성 방어와 중복 클릭 방지.
4. 발행처 문구의 입력·저장·응답·렌더 출처 확정.
5. 전체 Member 카드 생성 진행률·실패 Member 표시. 현재는 별도 비동기 Job 없이 프론트에서 Member 단위 호출을 orchestration할 수 있다.
6. 학생증 렌더러·앞뒷면 에셋·4개 조합 테스트 완성.

### P2 — 정리 및 후속 개선

1. 관리자 페이지의 stale “미구현/mock” 안내 문구 정리.
2. `admin-saju.md`의 `side` 필드, 전체 비동기 생성 등 보류/폐기 문구를 archive 또는 명시적 superseded 절로 이동.
3. Manseryeok active row 동시 확정 시 DB 수준 단일 active 보장 검토.
4. 카드 생성 결과를 관리자 상세 재조회만으로 복구할 수 있도록 응답 구조 정리.

---

## 권장 구현 순서

```text
1. 문서 SoT 충돌 제거 및 API 계약 확정
2. 관리자 상세/Member 응답 확장
3. 프론트 timezone resolve → 계산 → confirm → active 조회 연결
4. 실제 이름 추천 연결 + 성씨/의미 입력
5. 작명 API NAME_EDITING 가드
6. 카드 디자인 조회·발급일자·Preview UI
7. Member 단위 Generate/재생성 UI + 전체 완료 집계
8. startProducing/cardReady 서버 선행조건
9. 학생증 렌더러 완성 및 4개 조합 연결
10. 전체 E2E 검증
```

이 순서가 필요한 이유는 카드 UI부터 붙이면 현재 mock 만세력·성씨 누락 결과를 카드의 확정 데이터로 사용하게 되고, 상태 전이 가드부터 추가하면 현재 프론트가 필요한 데이터를 제공하지 못해 관리자 업무가 더 일찍 막히기 때문이다.

---

## 최종 판정표

| 영역 | Backend | Frontend | E2E | 최종 판정 |
|---|---|---|---|---|
| 신청 접수·관리자 기본 조회 | PASS | PASS | PARTIAL PASS | 상세 필드 보강 필요 |
| 입금 확인·검토 시작 | PASS | PASS | PASS 가능 | 기본 흐름 일치 |
| 사진 반려·재검토 | PASS | 연결 존재 | 확인 필요 | 기본 계약 일치 |
| 만세력 시간 확정 | PASS | NOT CONNECTED | BLOCKED | 프론트 로컬/mock 제거 필요 |
| 이름 추천 | 계약 불완전/확인 필요 | MOCK | BLOCKED | 실제 추천 SoT 필요 |
| 성씨·이름 확정 | PASS | PARTIAL | BLOCKED | surname 누락 |
| 작명 완료 | PASS | 호출 있음 | BLOCKED | 프론트 입력으로 서버 조건 충족 불가 |
| 일반 카드 Preview | PASS | NOT CONNECTED | BLOCKED | 관리자 UI 필요 |
| 일반 카드 Generate | PASS(최소 버전) | NOT CONNECTED | BLOCKED | UI 및 전체 완료 가드 필요 |
| 학생증 Preview/Generate | NOT IMPLEMENTED | NOT CONNECTED | BLOCKED | 렌더러·에셋 후속 작업 |
| 제작 시작·카드 준비 | API 존재, 선행조건 부족 | 버튼 존재 | UNSAFE | 전 Member 완료 검증 필요 |
| 실물 발송·완료 | PASS | 호출 존재 | 선행 단계 때문에 BLOCKED | 앞 단계 해소 후 검증 |

**종합 결론: 동일한 상태 이름은 공유하지만, 만세력 확정값·작명 완료 기준·카드 생성 결과를 잇는 데이터 계약과 UI orchestration은 아직 서로 동일하게 구현되어 있지 않다. 현재 상태로는 관리자가 신청 접수부터 최종 카드 발급까지 완주할 수 없다.**
