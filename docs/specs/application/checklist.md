# Application Validation Checklist

> 이 문서는 Application 명세의 검증 영역을 정의하는 파생 문서입니다.
> 업무 규칙은 [requirements.md](requirements.md), 데이터 구조는 [data-model.md](data-model.md), 외부 계약은 [api.md](api.md)를 기준으로 합니다.
> 새로운 정책을 이 문서에서 정의하지 않습니다.

## 1. Requirements

- [ ] 개인 신청은 `Application` 1건과 `ApplicationMember` 1건으로 처리되는가?
- [ ] 단체 신청은 `Application` 1건과 유효한 인원 수만큼의 `ApplicationMember`로 처리되는가?
- [ ] 사용자는 `CardType`만 선택하고 `CardDesign`은 관리자가 배정하는가?
- [ ] 출생시간·출생지역·입국날짜의 선택 입력 규칙이 반영됐는가?
- [ ] 학생증 학번·학과·학교 로고 필수 및 학교 직인 선택 규칙이 반영됐는가?
- [ ] `IssueType`이 Receiver와 배송 정책만 결정하고 파일 조합에는 영향을 주지 않는가?
- [ ] 파일 조합이 `ApplicationType × CardType` 기준으로 결정되는가?
- [ ] 결제 금액을 하드코딩하거나 `CardType.price × 수량`으로 자동 계산하지 않는가?
- [ ] 상담 후 신청하고, Application 생성 시 결제 금액을 계산하거나 저장하지 않는가?
- [ ] 신청 이후 계좌이체 처리는 Application 생성과 분리되어 있는가?
- [ ] 단체 신청의 엑셀 ID는 ZIP 사진 매칭에만 사용하고 별도 사진 파일 ID로 저장하지 않는가?
- [ ] 신청일로부터 3일 이내 미입금 신청을 취소하는가?
- [ ] `RECEIVED`, `REVIEWING`, `PHOTO_REJECTED` 상태의 전액 환불 정책이 반영됐는가?

## 2. Data Model

- [ ] `Application`과 `Applicant`의 1:1 관계가 보장되는가?
- [ ] `Application`과 `Receiver`의 1:1 관계가 보장되는가?
- [ ] `Application`과 `ApplicationMember`의 1:N 관계가 보장되는가?
- [ ] 개인 신청의 `total_quantity`가 1로 고정되는가?
- [ ] `application_number`에 UNIQUE 제약이 있는가?
- [ ] `card_design_id`가 신청 생성 시 `NULL`인가?
- [ ] 개인 신청과 단체 신청의 email/phone 저장 위치가 명세와 일치하는가?
- [ ] 학생증이 아닌 신청에서 학생증 전용 필드가 사용되지 않는가?

## 3. State Transitions

- [ ] `PAYMENT_PENDING → RECEIVED` 전이가 검증되는가?
- [ ] `RECEIVED → REVIEWING` 전이가 검증되는가?
- [ ] `REVIEWING → PHOTO_REJECTED` 전이가 검증되는가?
- [ ] `PHOTO_REJECTED → REVIEWING` 전이가 검증되는가?
- [ ] `REVIEWING → NAME_EDITING` 전이가 검증되는가?
- [ ] `NAME_EDITING → PRODUCING` 전이가 검증되는가?
- [ ] `PRODUCING → COMPLETED` 전이가 검증되는가?
- [ ] 허용되지 않은 상태 전이가 거절되는가?

## 4. API Contract

- [ ] URL과 HTTP method가 [api.md](api.md)와 일치하는가?
- [ ] Request 필드와 validation이 명세와 일치하는가?
- [ ] Response 필드와 상태 enum이 명세와 일치하는가?
- [ ] 인증 및 소유권 검증이 적용되는가?
- [ ] 공통 응답 및 에러 형식을 준수하는가?
- [ ] Entity가 API 응답에 직접 노출되지 않는가?

## 5. Tests

- [ ] `{Class}Test`가 불변조건과 상태 전이를 검증하는가?
- [ ] `{Class}ServiceTest`가 트랜잭션, 소유권, 업무 규칙을 검증하는가?
- [ ] `{Class}ControllerTest`가 API 계약, 인증, 오류 응답을 검증하는가?
- [ ] 개인·단체 정상 시나리오가 모두 존재하는가?
- [ ] 필수값 누락과 잘못된 상태 전이 시나리오가 존재하는가?

## 6. Documentation Consistency

- [ ] requirements/data-model/api의 필드명과 enum이 일치하는가?
- [ ] 새 미결정 사항이 중앙 open-questions 문서에 등록됐는가?
- [ ] Markdown 내부 링크가 유효한가?
- [ ] 레거시 문서를 현재 구현 기준으로 참조하지 않는가?
- [ ] Service 처리 순서와 책임이 [service-flow.md](service-flow.md)와 일치하는가?
