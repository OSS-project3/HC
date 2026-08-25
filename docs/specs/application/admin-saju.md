# 관리자 만세력 계산 정책

## 문서 목적

관리자가 신청자의 출생정보를 바탕으로 만세력 결과를 확인할 때 적용할 시간대, DST 및 진태양시 계산 책임과 HC 연동 경계를 정의합니다.

> **시스템 경계 확정(2026-08-26)**: 만세력 계산은 별도 `saju` 프로젝트가 전담합니다. 아래의 백엔드·프론트엔드는 각각 **별도 saju 시스템의 Spring 백엔드와 saju web**을 뜻합니다. HC는 신청자 정보를 Excel로 내보내고 별도 saju 결과 Excel을 다시 가져오는 역할만 담당하며, HC 백엔드·일반 사용자 프론트에 만세력 계산 API, `SajuCalculationResult` 엔티티 또는 전체 계산 이력을 추가하지 않습니다.

기존 별도 프로젝트의 `saju/web/src/lib/saju.ts` 계산 결과를 기준으로 결과값을 비교하되, 별도 saju 시스템에서는 역사적 timezone 및 DST 판정을 saju Spring 백엔드가 단일하게 담당합니다.

## 시스템 책임

### 별도 saju Spring 백엔드

Spring은 다음 순서로 출생시각의 절대 시점을 확정합니다.

```text
입력된 출생 현지시각
+ IANA timezoneId
→ ZoneRules로 해당 현지시각의 유효 offset 조회
→ DST 경계 상태 판정
→ offset 확정
→ utcInstant 확정
```

백엔드는 단순 UTC offset만 프론트엔드에 전달하지 않습니다. 역사적 DST를 포함한 timezone 판정과 `utcInstant` 확정까지 백엔드에서 완료합니다.

`LocalDateTime.atZone(zoneId)`를 검증 없이 바로 사용하지 않습니다. 먼저 `ZoneRules#getValidOffsets(LocalDateTime)`의 결과를 확인하여 존재하지 않거나 중복되는 현지시각을 명시적으로 처리합니다.

### 별도 saju 프론트엔드

프론트엔드는 백엔드가 확정한 다음 정보를 사용합니다.

- `utcInstant`
- `timezoneId`
- 선택된 UTC offset
- 출생지 경도
- 시간 정확도

프론트엔드는 역사적 DST 또는 UTC 변환을 다시 수행하지 않습니다. 확정된 절대 시점을 기준으로 진태양시 보정과 만세력 계산만 수행합니다.

따라서 프론트엔드에서 `Intl` 등을 이용해 timezone offset을 다시 계산하거나, 백엔드에서 받은 offset으로 `utcInstant`를 재계산하지 않습니다.

## DST 경계 정책

### 정상 현지시각

유효한 offset이 하나이면 해당 offset으로 `utcInstant`를 확정합니다.

```text
validOffsets.size() == 1
→ timeAccuracy = EXACT
→ utcInstant 확정
→ 시주를 포함한 전체 만세력 계산
```

### NONEXISTENT_LOCAL_TIME

DST 시작으로 시계가 앞으로 이동하여 입력한 현지시각이 실제로 존재하지 않는 경우입니다.

예:

```text
DST 시작일 02:30
→ 해당 지역에서는 02:00 다음이 03:00
→ 02:30은 존재하지 않음
```

정책은 다음과 같습니다.

- `03:30` 등 다른 시각으로 자동 보정하지 않습니다.
- 관리자에게 입력값과 timezone 전환 정보를 표시합니다.
- 출생기록을 확인하여 현지시각을 수정하도록 합니다.
- 현지시각이 수정되기 전에는 `utcInstant`를 확정하지 않습니다.
- 현지시각이 수정되기 전에는 만세력을 계산하지 않습니다.

### AMBIGUOUS_LOCAL_TIME

DST 종료로 동일한 현지시각이 두 번 존재하는 경우입니다.

예:

```text
DST 종료일 01:30
→ DST offset이 적용된 01:30
→ 표준시 offset이 적용된 01:30
→ 두 개의 utcInstant가 존재
```

백엔드는 가능한 두 offset과 각각의 `utcInstant`를 관리자에게 제공합니다.

```json
{
  "status": "AMBIGUOUS_LOCAL_TIME",
  "timezoneId": "America/New_York",
  "options": [
    {
      "offset": "-04:00",
      "utcInstant": "2000-10-29T05:30:00Z"
    },
    {
      "offset": "-05:00",
      "utcInstant": "2000-10-29T06:30:00Z"
    }
  ]
}
```

객관적인 출생기록 등으로 offset을 확인할 수 있으면 관리자가 하나를 선택합니다.

```text
offset 확인 가능
→ 선택한 offset과 utcInstant 저장
→ timeAccuracy = EXACT
→ 시주를 포함한 전체 만세력 계산
```

offset을 판별할 수 없으면 관리자가 임의로 선택하지 않습니다.

```text
offset 판별 불가
→ timeAccuracy = PARTIAL
→ 특정 utcInstant를 정답으로 임의 확정하지 않음
→ 두 offset에 대응하는 utcInstant를 각각 후보로 계산
→ 두 계산 결과에서 동일한 주(柱)만 확정
→ 서로 다른 주는 uncertainPillars로 표시
→ 확정 가능한 정보만으로 작명 진행
```

`PARTIAL`이라고 해서 항상 시주만 제외하고 연주·월주·일주를 모두 확정하지 않습니다. 두 후보가 한 시간 차이 나더라도 진태양시 보정 결과가 날짜 또는 절기 경계를 넘으면 일주나 월주까지 달라질 수 있기 때문입니다.

```text
두 후보의 연주가 같음 → 연주 확정
두 후보의 월주가 같음 → 월주 확정
두 후보의 일주가 같음 → 일주 확정
두 후보의 시주가 같음 → 시주 확정 가능
두 후보에서 값이 다름 → 해당 주를 uncertainPillars에 기록
```

일부 주가 불확실하면 전체 명식에 의존하는 오행 합계나 부족 오행을 확정 결과로 표시하지 않습니다. 후보별 결과를 구분하여 제공하거나 `partial=true`를 명시합니다.

## 시간 정확도

```java
public enum TimeAccuracy {
    EXACT,
    PARTIAL,
    UNKNOWN
}
```

| 값 | 의미 | 계산 범위 |
|---|---|---|
| `EXACT` | 출생시각과 offset을 확정하여 절대 시점을 알 수 있음 | 시주를 포함한 전체 계산 |
| `PARTIAL` | 출생시각은 입력됐지만 DST 중복 등의 사유로 하나의 절대 시점을 확정할 수 없음 | 후보 결과에서 공통인 주만 확정 |
| `UNKNOWN` | 출생시간이 입력되지 않았거나 신뢰할 수 없음 | 해당 날짜의 가능한 결과에서 공통인 주만 확정하고 시주는 제외 |

### 불변조건

- `EXACT`이면 `timezoneId`, 선택된 offset 및 `utcInstant`가 모두 존재해야 합니다.
- `PARTIAL`이면 후보 중 하나를 정답인 `utcInstant`로 임의 선택하지 않습니다.
- `PARTIAL`은 모든 후보에서 동일한 주만 `confirmedPillars`에 포함합니다.
- `PARTIAL`에서 후보별 값이 다른 주는 `uncertainPillars`에 포함합니다.
- 불확실한 주가 있으면 전체 명식에 의존하는 오행 합계나 부족 오행을 확정값으로 제공하지 않습니다.
- `UNKNOWN`을 임의의 정오 등으로 변환하여 시주를 생성하지 않습니다.
- `UNKNOWN`은 출생일의 가능한 시간 범위를 기준으로 결과를 비교하며 모든 후보에서 동일한 주만 확정합니다.
- `UNKNOWN`에서는 시주를 항상 제외하며, 절기 또는 날짜 경계로 다른 주도 달라질 수 있으면 함께 불확실로 표시합니다.
- `PARTIAL` 또는 `UNKNOWN`인 경우 관리자 화면에 확정할 수 없는 주와 그 이유를 표시합니다.
- 이후 객관적인 정보로 offset이 확인되면 `PARTIAL`에서 `EXACT`로 변경하고 만세력 결과를 다시 계산합니다.
- 시간 정확도 변경과 재계산 이력은 감사 가능한 형태로 남깁니다.

## 출생지역 해석 정책

출생지역은 IANA `timezoneId`와 경도를 얻기 위한 입력입니다.

- 검색 결과가 하나이면 해당 `timezoneId`와 경도를 계산 후보로 사용합니다.
- 같은 지명이 여러 지역에 존재하면 후보를 제시하고 관리자가 실제 출생지역을 선택합니다.
- `timezoneId` 또는 경도를 찾지 못하면 서울이나 현재 서버 timezone으로 대체하지 않습니다.
- 외부 지역 조회 서비스의 장애와 실제 검색 결과 없음은 서로 다른 오류로 구분합니다.
- 관리자가 선택한 `timezoneId`와 경도는 계산 기록에 고정합니다.
- 출생지역을 수정하면 기존 시간 판정과 만세력 결과를 오래된 결과로 표시하고 다시 계산합니다.

지역 조회 공급자는 설정으로 교체할 수 있도록 추상화하며, 특정 공급자의 응답 객체를 Application 도메인 모델에 직접 저장하지 않습니다.

## 계산 재현성

timezone 데이터나 만세력 엔진이 업데이트되면 같은 입력의 결과가 달라질 수 있으므로 계산에 사용한 원본과 버전을 기록합니다.

```text
birthLocalDateTime
timezoneId
longitude
selectedOffset          // EXACT일 때
utcInstant              // EXACT일 때
timeAccuracy
timeResolutionReason
tzdbVersion
calculationEngineVersion
calculatedAt
```

`PARTIAL`은 하나의 `selectedOffset` 또는 `utcInstant`를 정답으로 저장하지 않습니다. 대신 계산에 사용한 offset/instant 후보와 `confirmedPillars`, `uncertainPillars`를 구분하여 재현할 수 있어야 합니다.

timezone DB나 만세력 엔진 버전이 변경되어도 기존 계산 결과를 자동으로 덮어쓰지 않습니다. 관리자가 재계산을 실행한 경우 새 결과와 버전을 저장하고 이전 결과와 변경 이력을 보존합니다.

## 별도 saju 내부의 기존 만세력 로직 이식 원칙

별도 `saju/web/src/lib/saju.ts`를 saju Spring 백엔드로 그대로 복사하지 않습니다. 기존 함수에는 브라우저에서 timezone/DST를 판정하는 책임이 포함되어 있어, 그대로 이식하면 saju Spring 백엔드와 saju 프론트엔드에서 DST를 중복 계산할 수 있습니다.

이식 시 다음 경계를 적용합니다.

```text
기존 saju.ts
├─ 현지시각 → UTC 변환             saju Spring으로 이동
├─ 역사적 timezone/DST 판정       saju Spring으로 이동
├─ 확정 시점의 계산 입력 변환       saju web 유지
├─ 진태양시 보정                   saju web 유지
└─ manseryeok 계산                saju web 유지
```

saju web에는 `utcInstant`와 경도를 입력받아 계산하는 별도 함수를 두고, 해당 함수에서는 timezone offset을 다시 조회하지 않습니다.

## 검증 정책

이식 전에 기존 `saju.ts` 결과를 테스트 기준값으로 확보하고, Spring에서 확정한 `utcInstant`를 사용했을 때 동일한 결과가 나오는지 비교합니다.

최소 검증 대상은 다음과 같습니다.

- 서울 등 DST가 없는 지역
- 뉴욕의 표준시 및 서머타임 적용 시점
- DST 시작 시 존재하지 않는 현지시각
- DST 종료 시 중복되는 현지시각과 두 offset 선택 결과
- 시드니 등 남반구 DST 지역
- UTC 변환으로 날짜가 변경되는 지역
- 같은 절대 시점에서 경도 차이에 따른 진태양시 결과
- 출생시간 미입력
- 1970년 이전 역사적 timezone 데이터
- 연주·월주·일주·시주 및 오행 계산 결과 전체

기존 코드의 결과를 고정하는 테스트는 동작 보존을 위한 회귀 테스트로 사용합니다. 기존 구현 결과만으로 정확성이 증명되는 것은 아니므로 대표 계산값은 검증된 수작업 기준값 또는 업무 전문가가 확인한 값과 별도로 대조합니다.

## HC와 별도 saju 결과 연동 계약

```text
HC 신청 데이터 Excel 내보내기
→ 별도 saju 프로그램에서 만세력 계산과 이름 선택
→ 결과 Excel을 HC로 가져오기
→ HC가 최종 이름·한자·띠 코드를 Member에 반영
→ 관리자가 HC에서 성씨와 필수 의미 확정
```

- HC는 `confirmedPillars`, 오행 점수 또는 만세력 계산 이력을 저장하지 않습니다.
- 결과 Excel에는 `사주이름`과 선택 한자, 카드 이미지용 `띠 코드`를 포함하고 성씨는 포함하지 않습니다.
- 단체 결과는 행 순서나 이메일·전화번호가 아니라 신청 내에서 유일한 `사진 번호`로 Member와 매칭합니다.
- 의미는 HC의 이름 사전 또는 관리자 입력으로 확정하며 `completeNaming()` 전에 반드시 존재해야 합니다.
- 누락·중복 사진 번호, 존재하지 않는 Member, 잘못된 띠 코드 또는 필수 결과 누락이 하나라도 있으면 전체 import를 실패시킵니다.
- 별도 saju의 mock fallback 결과는 HC의 확정 결과로 import할 수 없습니다.

## 한국 이름 추천 및 확정 정책

만세력은 한국 이름 후보를 추천하기 위한 참고 자료이며 이름을 자동으로 확정하지 않습니다. 최종 이름의 결정권은 관리자에게 있습니다.

### 추천 대상과 시간 정확도

- timeAccuracy가 EXACT인 신청자에게만 확정된 오행 합계를 사용하여 이름 추천 점수를 계산합니다.
- PARTIAL 또는 UNKNOWN인 신청자에게는 오행 보완 점수와 추천 순위를 제공하지 않습니다. 관리자는 불확실한 만세력 결과를 확정값처럼 사용하지 않고 수동으로 이름을 입력할 수 있습니다.
- 이름 분위기, 선호 발음 및 성별은 추천 입력값으로 받지 않습니다.
- 만세력 계산 결과는 후보 추천에만 사용하며 최고 점수의 이름을 자동 저장하거나 자동 확정하지 않습니다.

### 이름 사전 기반 추천

- 자동 추천 후보는 등록된 이름 사전의 데이터만 사용합니다.
- 추천 점수는 현재 프론트엔드의 오행 보완 계산 규칙을 기준으로 합니다.
  - 오행 개수가 0이면 결핍 가중치 3
  - 오행 개수가 1이면 결핍 가중치 1
  - 그 외에는 결핍 가중치 0
  - 자원오행 평균 점수에 가중치 2
  - 발음오행 평균 점수에 가중치 1
  - 자원오행 사이가 상생이면 0.3 가점
  - 자원오행 사이가 상극이면 0.3 감점
  - 부족하거나 약한 오행을 둘 이상 보완하면 초과 개수마다 0.5 가점
- 무작위 후보 선택은 사용하지 않습니다. 같은 계산 입력과 같은 이름 사전 버전이면 결과 순서가 같아야 합니다.
- 추천 결과는 점수 내림차순으로 정렬하고, 점수가 같으면 이름 사전 ID 오름차순으로 정렬합니다.
- 이름 사전에 있더라도 아래 이름 형식 검증을 통과하지 못하는 데이터는 추천 대상에서 제외합니다.

### 관리자 수동 입력

- 관리자는 이름 사전에 없는 이름도 직접 입력하여 최종 확정할 수 있습니다.
- 수동 입력은 추천 알고리즘의 결과가 아니며 오행 추천 점수를 임의로 부여하지 않습니다.
- 추천 후보를 선택한 경우와 관리자가 수동 입력한 경우 모두 동일한 이름 형식 검증을 적용합니다.
- 추천 후보의 뜻은 이름 사전 값을 사용하고, 수동 입력 이름은 관리자가 의미를 반드시 입력합니다.

### 성씨 분리 정책

만세력과 이름 사전은 성씨를 포함하지 않은 이름만 추천합니다. 성씨는 오행 추천 점수와 후보 정렬에 사용하지 않으며 관리자가 이름을 선택한 뒤 별도로 입력합니다.

```text
만세력 계산
→ 성씨를 제외한 이름 후보 추천
→ 관리자가 추천 이름 선택 또는 이름 수동 입력
→ 관리자가 성씨 입력
→ 성씨·이름·한자·의미 검증
→ 작명 완료
```

- `ApplicationMember.surname`과 `ApplicationMember.name`을 분리 저장합니다.
- `surname`은 `NAME_EDITING` 중에는 `null`일 수 있지만 `completeNaming()` 실행 시 필수입니다.
- `name`은 성씨를 제외한 이름만 저장합니다.
- 카드의 한글 이름은 렌더링 시 `surname + name`으로 조합합니다.
- 현재 범위에서 `chineseName`은 이름에 대응하는 한자만 저장하며 성씨 한자 필드는 추가하지 않습니다.
- 이름 추천 결과와 이름 사전에는 성씨를 저장하지 않습니다.
- 카드 제작 단계에 들어간 후에는 성씨와 이름을 수정할 수 없습니다.
- 관리자 작명 저장 요청은 `surname`, `name`, 선택 `hanja`, `reading`, 필수 `meaning`을 함께 받습니다.

### 이름 입력 형식

- 성씨는 이름과 분리된 별도 필드로 관리하며 관리자가 입력하고 최종 확정합니다.
- 이름은 성씨를 제외한 2~3글자만 허용합니다.
- 한글 이름에는 한글만 허용하며 숫자, 영문 및 특수문자를 허용하지 않습니다.
- 한자는 선택 입력이며 필수값이 아닙니다.
- 한자를 입력한 경우 한글 이름과 한자 이름의 Unicode 글자 수가 같아야 합니다.
  - 민준 / 珉俊: 허용
  - 민준 / 珉: 거절
- 의미는 필수로 저장합니다. 이름 사전 후보는 사전의 의미를 사용하고, 수동 입력은 관리자가 의미를 입력합니다.
- 동일한 이름을 여러 신청자에게 확정하는 것은 허용합니다. 이름은 신청자 간 고유값이 아닙니다.

### 최종 확정 흐름

    만세력 계산
    → timeAccuracy 확인
    → EXACT이면 이름 사전 후보의 오행 점수 계산
    → 점수 내림차순, 동점이면 이름 사전 ID 오름차순 정렬
    → 관리자가 추천 후보 선택 또는 이름 직접 입력
    → 성씨·이름·선택 한자·필수 의미 검증
    → 관리자가 최종 확정
    → ApplicationMember에 저장

이름 추천 또는 수동 입력만으로 신청 상태를 자동 전이하지 않습니다. 모든 신청자의 이름이 확정된 뒤 관리자가 별도의 작명 완료 처리를 실행해야 합니다.

## 운영 표시 정책

관리자 화면은 최소한 다음 정보를 표시합니다.

- 입력된 출생 현지시각
- 출생지역과 `timezoneId`
- DST 판정 결과
- 선택된 offset과 `utcInstant` 또는 확정 불가 사유
- `timeAccuracy`
- `confirmedPillars`와 `uncertainPillars`
- 시주 및 기타 불확실한 주의 제외 여부
- `PARTIAL`인 경우 `DST 중복으로 일부 만세력 정보를 확정할 수 없음` 안내
- `UNKNOWN`인 경우 `출생시간 미입력으로 시간에 따라 달라지는 주를 확정할 수 없음` 안내

`NONEXISTENT_LOCAL_TIME`은 계산 가능한 부분 결과로 처리하지 않고 입력 확인이 필요한 오류 상태로 표시합니다.

## 작명 완료 이후 카드 제작 연결 정책

### 문서 범위

이 절은 관리자 만세력·작명 결과가 카드 이미지 생성 단계로 넘어가는 경계를 정의합니다. 최신 코드 기준으로 결제 확인, 검토 시작, 작명 승인, 사진 반려, 작명 완료, 제작 시작, 카드 준비 및 발송 상태 전이 API는 이미 구현되어 있습니다. 여기서는 기존 상태 전이 API를 다시 설계하지 않고 `PRODUCTION_READY` 이후 카드 생성에 필요한 책임과 불변조건만 정리합니다.

학생증 렌더링은 이번 범위에서 제외하고 명예한국인증, 명예시민증, 방문증 3종을 먼저 지원합니다.

### 확정 연결 흐름

```text
NAME_EDITING
→ 모든 Member의 성씨·이름·필수 의미 확정
→ completeNaming
→ PRODUCTION_READY
→ Application 단위 디자인 선택
→ Application 단위 발급일자 입력
→ Member 한 명·한 면 미리보기
→ 관리자 최종 확정
→ 전체 Member 카드 비동기 생성
→ 전원 렌더링·S3 업로드·DB 반영 성공
→ 카드 생성 완료 기록
→ startProducing
→ PRODUCING
→ cardReady
   ├─ MOBILE: COMPLETED
   └─ MOBILE_AND_PHYSICAL: PRODUCING 유지, 모바일 카드 조회 허용
→ dispatch
→ COMPLETED
```

- 디자인은 Application 전체에서 하나를 공유합니다.
- 발급일자는 Application 단위로 한 번 입력하고 모든 `ApplicationMember.issueDate`에 동일하게 저장합니다.
- 미리보기는 Member 한 명과 `FRONT` 또는 `BACK` 한 면 단위로 동기 생성하며 DB와 S3에 저장하지 않습니다.
- 최종 확정은 Application의 전체 Member를 한 작업으로 생성합니다.
- 한 명이라도 실패하면 어떤 Member의 새 카드 경로도 DB에 반영하지 않습니다.
- 카드번호는 관리자가 Member별로 입력·확정하며 최초 최종 생성 전에 저장합니다. 재생성할 때 기존 번호를 유지합니다.
- 재생성은 `PRODUCTION_READY`에서만 허용합니다.
- 카드 제작 단계에서는 성씨·이름·한자·의미를 수정하지 않습니다. 수정이 필요하면 작명 단계에서 처리합니다.
- 전 Member의 카드 파일이 저장된 후에만 `startProducing`을 허용합니다.
- 카드 미리보기와 최종 생성은 최초 Excel 원문이 아니라 모든 수정 요청이 반영되고 관리자가 `APPROVED`로 확정한 현재 `ApplicationMember` DB 값을 사용합니다.
- 단체 Application의 모든 Member가 `APPROVED`가 아니면 작명 완료와 카드 생성 단계에 진입할 수 없습니다.

### 현재 코드에서 재사용 가능한 부분

| 구성 | 재사용 가능 범위 | 현재 한계 |
|---|---|---|
| `CardImageCompositor` | 템플릿 좌표에 사진·이름·영문명·카드번호·주소·발급일자를 합성 | 앞면만 지원하며 한자·의미·로고·직인·띠·학생증은 미지원 |
| 카드 템플릿 리소스 | 명예한국인증·명예시민증·방문증 디자인 리소스 | 모든 디자인의 시각 검수와 `CardDesign` DB 매핑 필요 |
| `Application.cardDesignId` | 신청 단위 디자인 선택 ID 저장 | 선택 메서드와 활성 디자인·카드 종류 일치 검증 없음 |
| `ApplicationMember` | `issueDate`, `cardNumber`, `cardFrontPath`, `cardBackPath` 저장 가능 | 생성 결과 일괄 반영 메서드와 `surname` 필드 없음 |
| `StorageService.uploadBytes` | 합성 이미지 S3 업로드 | 전체 생성 실패 보상과 재생성 정리 연결 필요 |
| 기존 transaction synchronization 패턴 | rollback 시 신규 S3 삭제, commit 후 기존 S3 삭제 | 카드 전체 생성 경로에 동일 패턴 적용 필요 |

### 현재 모델과 충돌하는 부분

1. 작명 정책은 성씨와 이름을 분리하지만 `ApplicationMember`에는 `surname`이 없습니다.
2. 학생증을 제외한 모든 카드는 주소를 표시해야 하지만 개인 신청의 `Applicant.address1/address2` 컬럼은 현재 신청 생성 흐름에서 채워지지 않습니다. 개인 신청에도 카드 표기용 주소 입력·저장 경로를 연결해야 하며 배송용 `Receiver` 주소를 자동 사용하면 안 됩니다.
3. `CardDesign`의 DB ID와 클래스패스 디자인 번호 1~6을 연결하는 명시적 값이 없습니다. DB PK를 디자인 번호로 간주하지 않습니다.
4. 현재 `CardImageCompositor`는 앞면만 생성하므로 `cardBackPath`까지 채우는 최종 흐름을 바로 구현할 수 없습니다.
5. `startProducing`과 `cardReady`는 디자인 선택 및 전 Member 카드 파일 존재 여부를 검사하지 않습니다.
6. 현재 사용자 카드 다운로드는 `COMPLETED` 상태만 허용합니다. `MOBILE_AND_PHYSICAL`은 카드 준비 후 발송 전 다운로드를 허용한다는 정책과 충돌합니다.
7. `cardReadyAt` 하나로 카드 파일 생성 완료와 사용자 공개 준비 완료를 동시에 표현하면 상태 의미가 섞입니다.
8. 로고·직인 입력 정책은 기존 Application 정책을 그대로 사용한다. 개인 일반카드는 로고·직인이 없고, 단체 일반카드는 로고·직인이 모두 필수다. 학생증은 개인·단체 모두 학교 로고가 필수이고 학교 직인은 선택이다. 카드 합성기는 이 매트릭스에 맞는 빈 영역 처리를 아직 지원하지 않는다.
9. 발행처 문구는 신청 시 입력받는 값이지만 현재 카드 제작 모델과 요청 DTO에 전용 필드가 연결되어 있는지 확인하고, 없으면 Application 단위 필드를 추가해야 한다.

### 권장 Entity 보완

- `ApplicationMember.surname`: 관리자가 확정한 한글 성씨. 1~2글자를 허용한다.
- 카드에 표시하는 전체 한글 이름은 성씨 1~2글자와 이름 2~3글자를 합쳐 최대 5글자다.
- `ApplicationMember.photoNumber`: 단체 신청 Excel의 사진 번호. 단체 신청에서 필수이며 `(applicationId, photoNumber)` 조합을 유일하게 유지한다. 개인 신청은 `null`을 허용한다.
- `Application.issuerName`: 신청 시 입력받는 발행처 문구를 Application 단위로 저장한다. 기존 DTO에 동일한 의미의 필드가 있으면 새 필드를 만들지 않고 그 필드를 사용한다.
- `Application.cardGeneratedAt`: 전 Member의 카드 파일 DB 반영 완료 시각.
- `Application.cardGenerationStatus`: `NOT_STARTED`, `PROCESSING`, `COMPLETED`, `FAILED`.
- `CardDesign.designNumber` 또는 `resourceDirectory`: DB 디자인과 템플릿 리소스의 명시적 매핑. `(cardTypeId, designNumber)` 조합은 유일해야 합니다.
- 비동기 생성의 진행률·실패 원인·재시도를 관리하기 위해 별도 `CardGenerationJob`을 둡니다.
- 생성 결과 이미지는 기존 `ApplicationMember.cardFrontPath/cardBackPath`에 S3 key를 저장하며 `UploadFile` row는 만들지 않습니다.
- 발급일자는 Application 단위 요청값을 각 Member의 기존 `issueDate`에 동일하게 저장하므로 Application에 중복 컬럼을 우선 추가하지 않습니다.

### 확정 불변조건과 권장 기본값

- `completeNaming`은 Application 소속 모든 Member의 성씨·이름·필수 의미와 형식을 Service에서 집계 검증한 뒤 실행합니다.
- `startProducing`은 결제·상태뿐 아니라 활성 디자인, 발급일자, 전 Member의 카드번호와 앞·뒷면 파일 존재를 검사합니다.
- `cardReady`도 전 Member 카드 파일 존재를 검사합니다.
- 같은 Application에서 실행 중인 카드 생성 작업은 하나만 허용합니다.
- 카드번호 형식은 `ROK-XXXXX-XXXX`를 유지합니다.
- 카드번호는 관리자가 `ROK-XXXXX-XXXX` 형식으로 Member별 입력하고 DB `UNIQUE` 제약으로 유일성을 보장합니다. 다른 Member가 이미 사용하는 번호는 거절합니다.
- 미리보기는 DB에 저장된 실제 카드번호가 있어야 실행할 수 있으며 예시 번호를 사용하지 않습니다.
- 카드 텍스트가 사진이나 다른 영역과 겹치면 디자인 기본 글꼴 크기에서 최대 2px까지만 줄입니다. 그래도 겹치면 생성에 실패하며 발급 카드에는 말줄임표를 사용하지 않습니다.
- 발급일자는 KST 기준 신청일의 날짜부터 신청일에 3개월을 더한 날짜까지 양 끝을 포함하여 허용합니다.
- 앞면·뒷면 필드와 좌표는 승인된 카드 시안을 기준으로 합니다. 한자가 없으면 해당 영역만 빈칸으로 남기고 다른 필드를 재배치하지 않습니다.

### 관리자 API 제안

| API | 역할 |
|---|---|
| `GET /api/admin/card-designs?cardTypeId={id}&active=true` | 선택 가능한 디자인 조회 |
| `PUT /api/admin/applications/{applicationId}/members/{memberId}/card-number` | 관리자가 실제 카드번호 입력·변경 |
| `PUT /api/admin/applications/{applicationId}/card-numbers` | 사진 번호 기준 단체 카드번호 일괄 입력 |
| `POST /api/admin/applications/{applicationId}/members/{memberId}/card-preview` | 단일 Member·단일 면 미리보기 |
| `POST /api/admin/applications/{id}/card-generation` | 전체 카드 생성 작업 시작, `202 Accepted` |
| `GET /api/admin/applications/{id}/card-generation` | 진행률·성공·실패 조회 |
| `GET /api/admin/applications/{id}/cards/download` | 관리자 제작용 결과 다운로드 |

미리보기 요청은 `cardDesignId`, `issueDate`, `side`를 받습니다. 미리보기 전에 대상 Member의 실제 카드번호가 관리자에 의해 저장되어 있어야 합니다. 최종 생성 요청은 `cardDesignId`, `issueDate`, 재생성 여부와 동시성 확인용 Application `version`을 받습니다. 학생증은 지원 전까지 명시적인 미지원 오류를 반환합니다.

### 관리자 카드번호 입력 정책

카드번호는 서버가 자동 생성하지 않고 관리자가 정합니다. 개인 신청은 Member별 입력 API를 사용하고, 단체 신청은 관리자 화면의 일괄 붙여넣기를 지원합니다.

관리자 화면의 붙여넣기 형식은 Excel에서 두 열을 복사할 수 있도록 헤더 없는 탭 구분 형식을 사용합니다.

```text
001<TAB>ROK-12345-0001
002<TAB>ROK-12345-0002
003<TAB>ROK-12345-0003
```

- 첫 번째 값은 신청 Excel에서 사용한 `photoNumber`, 두 번째 값은 관리자가 결정한 실제 카드번호입니다.
- 화면 정렬 순서나 Member ID로 암묵적으로 매칭하지 않습니다.
- `photoNumber`는 해당 Application에 속한 Member와 정확히 일치해야 합니다.
- 카드번호 형식은 `ROK-XXXXX-XXXX`이며 X는 숫자입니다.
- 카드번호는 전체 서비스에서 유일해야 합니다.
- 붙여넣기 내부의 사진 번호 중복, 카드번호 중복, 존재하지 않는 사진 번호, 잘못된 형식, 다른 Member가 이미 사용하는 카드번호가 하나라도 있으면 전체 요청을 실패시킵니다.
- 일부 행만 저장하는 부분 성공은 허용하지 않습니다.
- 붙여넣기 요청은 일부 Member만 포함할 수 있지만 최종 카드 생성 전에는 모든 Member에게 카드번호가 있어야 합니다.
- 입력 행 앞뒤 공백은 제거하되 사진 번호의 leading zero와 카드번호 값은 자동 보정하지 않습니다.
- 같은 카드번호를 같은 Member에게 다시 저장하는 요청은 멱등 성공으로 처리합니다.
- 다른 카드번호로 변경하는 것은 최초 카드 생성 성공 전까지만 허용합니다.
- 최초 카드 생성 성공 후에는 카드번호를 변경할 수 없으며 재생성에서도 기존 번호를 유지합니다.
- 일괄 저장은 Application을 잠그고 한 DB 트랜잭션에서 처리합니다.
- 동시 수정 감지를 위해 요청에 Application `version`을 포함합니다.

일괄 API 요청 모델은 다음과 같습니다.

```json
{
  applicationVersion: 12,
  items: [
    { photoNumber: 001, cardNumber: ROK-12345-0001 },
    { photoNumber: 002, cardNumber: ROK-12345-0002 }
  ]
}
```

관리자 UI는 붙여넣은 텍스트를 위 구조로 변환한 뒤 API를 호출합니다. 백엔드 API는 클립보드 원문을 직접 파싱하지 않습니다.

### Service 및 S3 처리 순서

#### 미리보기

1. 관리자 권한, Application 상태, Member 소속을 검증합니다.
2. 디자인 활성 여부와 카드 종류 일치를 검증합니다.
3. 작명 필수값과 발급일자를 검증합니다.
4. 사진·로고·직인 등 필요한 원본 파일을 읽습니다.
5. 저장된 실제 카드번호로 지정한 한 면을 합성합니다.
6. DB·S3 저장 없이 이미지 바이트를 반환합니다.

#### 최종 생성

1. 권한, 상태, 결제, Application `version`, 디자인, 전체 Member 검토 승인·작명 완료 여부, 전 Member 카드번호 존재·형식·유일성을 검증합니다.
2. 실행 중인 생성 작업이 없는지 확인하고 생성 상태를 `PROCESSING`으로 기록합니다.
3. DB 트랜잭션 밖에서 원본을 읽고 Member별 앞·뒷면을 렌더링한 뒤 신규 S3 key로 업로드합니다.
4. 업로드된 신규 key 전체를 추적합니다.
5. 한 DB 트랜잭션에서 상태와 `version`을 다시 검증한 후 디자인, 발급일자, 기존 카드번호, 카드 경로와 생성 완료 정보를 일괄 반영합니다.
6. 재생성이면 DB commit 이후 기존 카드 파일을 삭제합니다.
7. 렌더링·업로드·DB 저장·commit 실패 시 신규 S3 파일을 보상 삭제하고 기존 파일과 DB 값은 유지합니다.
8. 성공·실패·재생성과 수행 관리자를 `AdminActivityLog`에 기록합니다.

### 비동기 생성 실행 정책

단체 신청은 최대 100명이며 앞·뒷면을 모두 만들면 최대 200개 이미지를 렌더링하고 S3에 업로드해야 하므로 최종 카드 생성은 동기 HTTP 요청으로 처리하지 않습니다.

```text
POST card-generation
→ CardGenerationJob 생성
→ 202 Accepted + jobId 반환
→ Spring TaskExecutor에서 비동기 실행
→ 관리자는 jobId로 진행 상태 조회
→ COMPLETED 또는 FAILED
```

- 초기 운영에서는 RabbitMQ·Kafka 같은 외부 메시지 브로커를 추가하지 않고 Spring `TaskExecutor`를 사용합니다.
- 작업 상태는 DB의 `CardGenerationJob`으로 영속화하여 HTTP 연결 종료와 무관하게 조회할 수 있어야 합니다.
- 작업 상태는 `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`를 사용합니다.
- `totalCount`, `completedCount`, `failureReason`, `requestedBy`, `requestedAt`, `startedAt`, `completedAt`을 기록합니다.
- 같은 Application에는 `PENDING` 또는 `PROCESSING` 작업을 하나만 허용합니다.
- 한 작업 안에서는 Member를 순차 처리합니다. 동시에 여러 이미지를 무제한 렌더링하지 않습니다.
- 애플리케이션 재시작 등으로 장시간 `PROCESSING`에 머문 작업은 설정 가능한 기준시간이 지나면 `FAILED`로 전환하고 관리자가 재시도할 수 있게 합니다.
- 재시도는 새로운 `CardGenerationJob`을 생성하여 수행하며 이전 실패 이력을 덮어쓰지 않습니다.
- API는 작업 생성 직후 `202 Accepted`와 `jobId`를 반환하고 프론트엔드는 상태 조회 API를 polling합니다.
- 생성 성공 전까지 `ApplicationStatus`는 `PRODUCTION_READY`를 유지합니다.
- 작업 실패 시 신규 S3 파일을 보상 삭제하고 기존 DB 카드 결과와 기존 카드 파일을 유지합니다.
- 외부 메시지 큐는 다중 서버 운영이나 작업 유실 문제가 실제로 확인될 때 후속 도입합니다.

### 띠 이미지 결정 정책

HC는 띠를 생년의 양력 1월 1일, 음력 설 또는 입춘 기준으로 다시 계산하지 않습니다. 별도 saju 프로그램이 확정한 연주를 기준으로 결과 Excel에 `띠 코드`를 내보내고, HC는 검증한 코드를 `ApplicationMember.zodiacCode`에 저장하여 카드 이미지에 사용합니다.

```text
별도 saju에서 연주 확정
→ 결과 Excel에 띠 코드 포함
→ HC가 허용 enum 검증 후 저장
→ CardImageCompositor가 zodiacCode로 에셋 선택

연주 확정 불가 또는 띠 코드 누락
→ HC import 또는 카드 생성 거절
```

- `timeAccuracy`가 `EXACT`가 아니더라도 별도 saju가 모든 후보의 동일한 연주를 확인해 띠 코드를 확정했다면 import할 수 있습니다.
- 연주가 불확실하면 별도 saju는 띠 코드를 비워야 하며 HC는 임의의 띠 이미지를 사용하지 않습니다.
- HC의 재현 책임은 import된 `zodiacCode`와 결과 파일이며 HC 내부에 만세력 결과 엔티티를 추가하지 않습니다.

### 디자인 사용 승인 정책

- 관리자 디자인 조회에는 `CardDesign.active=true`인 디자인만 노출합니다.
- 실제 카드 출력·크기·텍스트·사진 좌표의 시각 검수를 통과한 디자인만 활성화합니다.
- 현재 검증이 끝나지 않은 방문증 디자인 1은 `active=false`로 유지합니다.
- 비활성 디자인은 신규 미리보기와 최종 생성에서 거절합니다.
- 과거 Application이 비활성화된 디자인을 참조하더라도 기존 생성 결과 조회는 허용하되 재생성하려면 활성 디자인을 다시 선택해야 합니다.

### 필수 검증

- Member 한 명이라도 성씨·이름·필수 의미가 없으면 `completeNaming` 실패.
- 미리보기는 DB와 S3를 변경하지 않으며 잘못된 상태·Member·디자인을 거절.
- 개인·단체 성공 시 전 Member에게 동일 발급일자, 관리자가 확정한 유일 카드번호, 앞·뒷면 경로 저장.
- N번째 렌더링 또는 업로드 실패 시 DB 카드 결과 미반영 및 신규 S3 파일 삭제.
- DB 저장 또는 commit 실패 시 신규 파일 삭제, 기존 재생성 파일 보존.
- 재생성 성공 후 카드번호 유지 및 기존 파일의 commit 이후 삭제.
- 생성 실패 시 Application을 `PRODUCTION_READY`에 유지하고 생성 작업만 `FAILED`로 기록. 신규 S3 파일을 정리한 뒤 관리자가 같은 설정으로 수동 재시도 가능.
- 재시도 사유는 필수 입력으로 받지 않고 수행 관리자·시각·실패 원인·재시도 결과를 감사 로그에 기록.
- 이름 추천 결과에는 성씨가 포함되지 않고 관리자가 성씨를 저장한 뒤에만 작명 완료 가능.
- 카드번호 일괄 입력의 정상 저장, 전체 실패, 중복, 잘못된 사진 번호, 낙관적 락 충돌 검증.
- 동시 생성 요청 중 하나만 실행.
- 카드 파일이 없으면 `startProducing`과 `cardReady` 실패.
- `MOBILE_AND_PHYSICAL`은 카드 준비 후 발송 전에도 모바일 카드 조회 가능.
- 미지원 학생증과 승인되지 않은 디자인은 명시적 오류 반환.

### 구현 권장 순서

1. 작명 완료 집계 검증과 Entity 불변조건.
2. `CardDesign`과 템플릿 리소스의 명시적 매핑 및 디자인 조회.
3. 단일 Member 미리보기.
4. 전체 생성 실행 방식 확정과 관리자 카드번호 입력·유일성 검증.
5. S3 보상·재생성·동시 실행 방지.
6. `startProducing`·`cardReady` 선행조건과 모바일 다운로드 조건.
7. 관리자 프론트 연결과 제작용 다운로드.
8. 학생증 렌더링.

### 확정된 카드 표기 정책

- 학생증에는 주소를 표시하지 않습니다.
- 명예한국인증, 명예시민증, 방문증에는 주소를 표시합니다.
- 단체 신청은 `ApplicationMember.address`를 사용합니다.
- 개인 신청은 카드 표기용 주소를 신청 입력값으로 받아 저장해야 합니다. 배송용 `Receiver` 주소와는 별도 값입니다.
- 발행처 문구는 신청 시 Application 단위 입력값으로 받습니다.
- 로고·직인은 Application의 기존 신청 유형·카드 종류 매트릭스를 그대로 사용합니다.
  - 개인 일반카드: 로고·직인 없음.
  - 단체 일반카드: 기관 로고·직인 필수.
  - 개인·단체 학생증: 학교 로고 필수, 학교 직인 선택.
- 성씨는 한글 1~2글자, 이름은 성씨를 제외한 한글 2~3글자, 전체 한글 이름은 최대 5글자입니다.
- 발급일자는 KST 기준 신청일 이상, 신청일로부터 3개월 이하여야 합니다.
- 앞면·뒷면 필드와 좌표는 승인된 시안을 따릅니다.
- 한자가 없으면 한자 영역만 빈칸으로 남깁니다.
- 텍스트는 기본 글꼴에서 최대 2px까지만 축소하고 여전히 겹치면 생성 실패로 처리합니다.
- 실제 카드번호는 관리자가 Member별로 입력하며 미리보기에서도 해당 실제 번호를 사용합니다.
- 단체 카드번호는 관리자 화면에서 사진 번호와 카드번호 두 열을 일괄 붙여넣어 저장합니다.
- 생성 실패 시 기존 신청·카드 결과를 유지한 채 관리자가 수동으로 다시 실행할 수 있습니다.

### 구현 전 정책 결정 상태

이 절에서 다루는 카드 생성 실행 방식, 띠 이미지 기준 및 디자인 활성화 기준은 위 정책으로 확정했습니다. 현재 확인된 정책 결정 차단 사항은 없으며, 구현 중 기존 DTO·DB·시안과 새 정책이 충돌하면 구현 범위를 확대하기 전에 다시 확인합니다.
