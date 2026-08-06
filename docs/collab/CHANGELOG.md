# CHANGELOG

작업을 종료할 때마다 **맨 위에** 새 항목을 추가한다 (최신이 위). 과거 항목은 수정·삭제하지 않는다.

## 템플릿

```md
## {YYYY-MM-DD} — {Claude|Codex} — `{브랜치명}`

- 변경: {무엇을 바꿨는지 한두 줄}
- 파일: {변경한 파일 목록}
- 사유: {왜 바꿨는지 — 요구사항 변경 / 버그 수정 / 설계 정리 등}
- 관련: TODO #{번호} (있다면)
```

---

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 5 완료)

- 변경: IDENTITY와 Root 선저장 순서를 유지하는 package-private ApplicationFactory를 추가하고 개인 신청의 Application, Applicant, Receiver, ApplicationMember 생성 책임을 Service에서 이동. Service는 검증, 파일 준비, 생성 호출, 저장 순서를 조정.
- 파일: ApplicationFactory.java, ApplicationService.java, ApplicationFactoryTest.java, docs/specs/application/requirements.md, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: CreatedApplication, CreatedChildren, Context 없이 최소 구조로 Entity 생성 책임을 독립시키기 위함.
- 테스트: Factory 테스트를 구현 전 클래스 부재로 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 101개 중 기존 UserControllerTest 2건만 실패.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 5

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 4 완료)

- 변경: Application 생성 시 서버 값의 책임을 확정. 수령인 동일 여부 계산을 ApplicationCreateRequest의 파생 메서드로 이동하고 Service는 이를 사용하도록 정리. prepareServerValues, Context, Factory는 추가하지 않음.
- 파일: ApplicationCreateRequest.java, ApplicationService.java, ApplicationCreateRequestTest.java, docs/specs/application/requirements.md, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: 신청번호는 Service, 초기 상태는 Entity, 수령인 동일 여부는 Request의 독립된 책임이므로 별도 준비 객체로 묶을 필요가 없음.
- 테스트: 신규 DTO 테스트 2건을 구현 전 메서드 부재로 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 99개 중 기존 UserControllerTest 2건만 실패.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 4

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 4 정책 확정)

- 변경: 상담 후 신청하고 신청 이후 계좌이체하는 흐름을 확정하여 Application 생성 시 total_price를 계산·저장하지 않도록 문서를 정정. 단체 신청은 엑셀 ID와 ZIP 사진 파일명을 매칭하되 ID를 저장하거나 구성원별 사진 파일 ID를 생성하지 않는 것으로 확정.
- 파일: docs/specs/application 문서, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: Task 4 구현 전에 가격과 파일 식별 정책을 확정하여 불필요한 서버 생성값과 중간 구조 도입을 방지.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 4

## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 3)

- 변경: `ApplicationPhotoValidator`를 추가해 얼굴사진과 학생증 학교 로고·직인의 5MiB, 확장자, MIME, signature, 디코딩을 검증. 얼굴사진은 EXIF Orientation 적용 후 300×400 최소 해상도를 검증하고 학교 파일은 해상도에서 제외. 학생증 필수값은 기존 Service private 메서드에서 공백까지 거절하도록 보강.
- 파일: `ApplicationPhotoValidator.java`, `ApplicationService.java`, `ApplicationPhotoValidatorTest.java`, `ApplicationServiceTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/{requirements,api}.md`, `docs/collab/{TODO,CHANGELOG}.md`
- 사유: Application Task 3의 확정 사진·학생증 정책을 업로드와 DB 저장 전에 적용. StudentCardValidator 별도 클래스는 구조 변경 원칙상 추가하지 않음.
- 테스트: Validator 정책 테스트 8건과 Service 통합 테스트 2건 추가. Application 관련 전체 테스트 통과. 전체 97개 중 기존 `UserControllerTest` 2건만 실패.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 3"
## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 2)

- 변경: 기존 `UserService.findEligibleApplicationUser()`에서 회원 존재·ACTIVE·USER 권한·필수 약관을 검증하고 User를 반환하도록 구현. ApplicationService의 UserRepository 직접 의존을 제거하고 UserService를 사용. 하루 3회 제한은 정책 보류에 따라 미구현.
- 파일: `domain/user/service/UserService.java`, `domain/application/service/ApplicationService.java`, `ApplicationServiceTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/api.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`
- 사유: Application Task 2의 확정 정책을 기존 ErrorCode로 구현하고 도메인 간 Repository 직접 참조를 제거.
- 테스트: 신규 상태·권한·약관 테스트 3건을 구현 전 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 87개 중 기존 `UserControllerTest` 2건만 실패.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 2"
## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 1)

- 변경: `createIndividual()`을 private 메서드 중심으로 분리하고 User 조회를 첫 단계로 이동. User 미존재 시 CardType 조회·신청번호 생성·파일 업로드·DB 저장 전에 `USER_NOT_FOUND`로 중단하도록 테스트 우선으로 보장. Factory/Validator/Context는 추가하지 않음.
- 파일: `domain/application/service/ApplicationService.java`, `domain/application/service/ApplicationServiceTest.java`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`
- 사유: Application 개인 신청 리팩터링 Task 1. 이후 로직의 전제조건인 User 존재를 부수효과보다 먼저 검증하고, 최소 구조 변경으로 Service 가독성을 개선.
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 84개 중 기존 `UserControllerTest` 2건만 실패하며 Task 전후 동일.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 1"

## 2026-08-01 — Claude — `feature/application-domain-impl` (API 4/5 + merge)

- 변경: API 4(`PATCH /api/applications/{id}/photo`, 사진 재업로드)·API 5(`GET /api/applications/{id}/cards/download`, 카드 다운로드) 구현으로 Application 도메인 5개 API 전부 완료. `backend-api`(Codex의 `docs/specs/application/*` 재구성, `ecd72b3`)를 이 브랜치로 머지(`docs/collab/*` 4개 파일만 충돌, RULES.md 7절 방식대로 수동 재작성). `ErrorCode`에서 삭제된 레거시 도메인 전용 코드 정리, `CARD_NOT_READY` 재사용. 컴파일이 깨진 orphan `infra/toss/*`(Payment 도메인 삭제 후 미사용) 삭제. `checklist.md` 6개 섹션 자체 검증(결과는 HANDOFF.md).
- 파일: `domain/application/service/ApplicationService.java`, `domain/application/dto/{ApplicationPhotoReuploadResponse,ApplicationCardDownloadResponse}.java`, `api/ApplicationController.java`, `common/exception/ErrorCode.java`, `infra/toss/*`(삭제), 테스트 4개 클래스(신규 21테스트)
- 사유: 사용자 승인("응") 후 API 4/5 이어서 구현, 그 직전 "변경사항을 받아오고" 요청으로 backend-api 병합 선행
- 관련: TODO "Application 도메인 엔티티/API 구현"(완료로 갱신)

## 2026-08-01 — Claude — `feature/application-domain-impl`

- 변경: `docs/specs/application/*` 기준으로 Application 도메인 재구현. 옛 BulkOrder/CitizenCard/KoreanName/Payment/Shipping 도메인 및 옛 Application/CardType/ApplicationStatus 삭제. 신규 CardType/CardDesign/UploadFile/Applicant/Receiver/ApplicationMember 엔티티 + 재작성된 Application(8단계 상태머신). API 1(개인 신청)/API 2(단체 ZIP 신청, 신규 BulkExcelParser)/API 3(신청 조회 lookup) 구현. GlobalExceptionHandler에 MissingServletRequestPartException 핸들러 추가.
- 파일: `domain/application/*`, `domain/card/*`, `domain/uploadfile/*`, `api/ApplicationController.java`, `common/enums/*`, `common/exception/GlobalExceptionHandler.java`, `infra/security/SecurityConfig.java`, 테스트 다수
- 사유: 사용자 요청("Application 구현 전 docs/specs/application 읽고 시작") + 5개 확정 문서(requirements/data-model/api/checklist + arch.md) 기준 구현
- 관련: TODO "Application 도메인 엔티티/API 구현 착수", HANDOFF.md의 "확인 필요" 항목(englishName/total_price/엑셀실패정책 — 사람 확인 완료, docs/specs/application 반영은 Codex 몫)

## 2026-08-01 — Codex — `feature/application-domain-docs`

- 변경: Application 문서를 `docs/specs/application/` 아래의 `requirements.md`/`data-model.md`/`api.md`/`checklist.md`로 패키지화하고 기존 경로 참조를 갱신.
- 파일: `docs/specs/application/*`, `DB.md`, `docs/api/README.md`, Application 경로를 참조하는 협업·테스트 문서
- 사유: Application 업무 규칙, 데이터 모델, API 계약과 검증 영역을 한 도메인 폴더에서 찾을 수 있도록 Source of Truth 구조를 정리.
- 관련: TODO "Application 문서 도메인 패키지 이전"

## 2026-08-01 — Claude — `backend-api`

- 변경: 협업 규칙 체계(`docs/collab/`) 신설 — `RULES.md`/`TODO.md`/`CHANGELOG.md`/`HANDOFF.md` 추가. 기존 `guide.md`는 `RULES.md`로 가리키는 안내문으로 축소.
- 파일: `docs/collab/RULES.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`, `guide.md`
- 사유: Claude(구현)와 Codex(문서/아키텍처)가 각자 다른 워크트리에서 작업하면서 서로의 변경사항을 사람이 매번 전달해야 하는 문제를 해결하기 위해, 작업 시작/종료 시 반드시 확인·갱신하는 공용 문서 체계를 만듦.
- 관련: TODO #1

## 2026-08-01 — Claude — `feature/application-domain-docs` (codex-docs 워크트리)

- 변경: `arch.md`의 계층 구조를 4계층(API/Application/Domain/Infrastructure) + Port/Adapter에서, 실제 코드 규모에 맞는 3단 구조(Controller/Service/Repository·Entity, Infra)로 단순화. 패키지 구조 예시를 실제 코드(`api/`, `domain/{도메인}/{entity,repository,service,dto}`, `infra/`)와 일치시킴. Command/Query 강제 분리 규칙 완화, 테스트 섹션 네이밍을 실제 컨벤션(`{Class}Test`/`{Class}ServiceTest`/`{Class}ControllerTest`)에 맞춤.
- 파일: `arch.md`
- 사유: 사용자가 "현재 구조에서는 과한 설계이니 규모에 맞게 축소해달라"고 요청. 비즈니스 규칙(4~18절 대부분)은 그대로 유지.
- 관련: -
