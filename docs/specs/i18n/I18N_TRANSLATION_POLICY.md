# 다국어 및 공식 콘텐츠 번역 정책

## 1. 문서 지위

이 문서는 한국어/영어 다국어 처리와 공식 콘텐츠 자동 번역의 Source of Truth이다.

- 지원 언어: ko, en
- 기본 언어: ko
- 한국어 원문이 Source of Truth이다.
- 영어 번역은 한국어 원문에서 생성되는 파생 데이터다.
- 번역 장애가 한국어 원문 조회를 실패시켜서는 안 된다.

## 2. 언어 선택

- 사용자가 KO 또는 EN을 선택하면 프론트엔드는 localStorage에 저장한다.
- 프론트엔드는 모든 API 요청에 현재 언어를 Accept-Language: ko 또는 en으로 직접 넣는다.
- 브라우저가 자동으로 보내는 언어 설정에는 의존하지 않는다.
- 저장값 또는 헤더가 없거나 잘못됐으면 ko로 fallback한다.
- 공개 사용자 조회 API만 요청 언어로 현지화한다.
- 관리자 작성·수정·조회 API는 한국어 원문만 다루며 영어 편집 기능을 제공하지 않는다.
- 공개 조회 응답에는 Vary: Accept-Language를 적용한다.

## 3. 정적 UI와 백엔드 고정 메시지

메뉴, 버튼, placeholder, 안내문, ApplicationStatus·EventType label은 프론트의 ko/en 리소스 파일로 번역한다. 권장 구현은 react-i18next이며 Gemini를 사용하지 않는다. 백엔드는 enum 코드값을 그대로 반환한다.

다음 백엔드 메시지도 Gemini로 실시간 번역하지 않고 사전 번역 리소스로 관리한다.

- ErrorCode
- GlobalExceptionHandler
- BulkExcelParser validation 메시지

동적 값은 언어별 메시지 템플릿과 인자로 조합한다.

## 4. 자동 번역 대상

| 콘텐츠 | 자동 번역 | 처리 |
|---|---:|---|
| 공지사항(BoardType.NOTICE) | O | 최초 영어 조회 시 번역 |
| FAQ(BoardType.FAQ) | O | 최초 영어 조회 시 번역 |
| 행사(EventPost) | O | 최초 영어 조회 시 번역 |
| 후기(Review) | X | 사용자 원문 표시 |
| 문의·답변(Inquiry) | X | 사용자·관리자 원문 표시 |
| 사용자 이름·학교명·신청 원본 | X | 원문 보존 |
| 첨부파일명·이미지·URL | X | 번역하지 않음 |

Board 번역 필드는 title, content다. 상세 응답의 이전·다음 게시글 제목도 요청 언어와 동일하게 처리한다.

Event 번역 필드는 title, eventDateText, place, host, cardLabel, content다. eventDate, eventType, 이미지 경로는 번역하지 않는다. companyName은 고유명사로 보고 한국어 원문을 유지한다.

## 5. Lazy translation과 fallback

~~~text
관리자 한국어 작성
→ 한국어 원문만 저장

사용자 ko 조회
→ 한국어 원문 반환

사용자 en 조회
→ 현재 한국어 sourceHash 계산
→ 같은 sourceHash의 영어 번역이 있으면 반환
→ 없거나 stale이면 Gemini 호출
→ 저장 직전 한국어 원문과 sourceHash 재확인
→ 같으면 영어 번역 저장 후 반환
→ 다르면 오래된 번역을 폐기하고 최신 한국어 원문 반환
→ Gemini 실패 시 한국어 원문 반환
~~~

- 작성·수정 시 Gemini를 호출하지 않는다.
- 번역 실패 결과는 저장하지 않는다.
- 실패 후 다음 영어 조회에서는 다시 시도할 수 있다.
- 목록 조회도 각 항목에 같은 규칙을 적용한다.
- 일부 항목의 번역만 실패하면 해당 항목만 한국어로 fallback하고 목록 전체는 200 OK로 반환한다.
- 동일 글의 동시 최초 영어 조회로 Gemini가 중복 호출되는 것은 현재 규모에서 허용한다.
- 분산락과 번역 작업 큐는 이번 범위에서 제외한다.

## 6. 번역 저장 구조

원본 엔티티에 titleEn/contentEn을 추가하지 않고 도메인별 별도 테이블을 사용한다.

### BoardTranslation

~~~text
id, boardId, language, title, content, sourceHash, createdAt, updatedAt
~~~

### EventTranslation

~~~text
id, eventId, language, title, eventDateText, place, host, cardLabel,
content, sourceHash, createdAt, updatedAt
~~~

공통 규칙:

- UNIQUE(parent_id, language)
- 한국어는 원본 엔티티에만 저장하고 번역 테이블에는 현재 en row만 저장
- sourceHash는 SHA-256 소문자 16진수 64자
- 부모 삭제 시 번역 row도 삭제
- 동시 insert의 UNIQUE 충돌은 오류 응답 대신 승자 row 재조회
- 원문 수정 시 번역 row를 즉시 삭제하지 않아도 된다. hash 불일치로 stale을 판단하고 다음 영어 조회에서 갱신한다.

별도 테이블을 쓰는 이유는 원본과 파생 데이터를 분리하고, 언어 추가 시 원본 엔티티 컬럼 증가를 막으며, hash와 언어별 UNIQUE 제약을 명확히 적용하기 위해서다.

## 7. sourceHash 규칙과 race condition

UNIQUE(parent_id, language)는 중복 row만 방지한다. 번역 도중 바뀐 원문의 오래된 결과 저장은 sourceHash로 막는다.

- UTF-8 canonical 문자열의 SHA-256 사용
- 버전 식별자와 번역 대상 필드를 고정 순서로 포함
- null과 빈 문자열을 구분하는 길이 기반 형식 사용
- Board hash: title, content
- Event hash: title, eventDateText, place, host, cardLabel, content
- 해시 입력에 버전 상수를 넣고 프롬프트 또는 필드 구성이 바뀌면 버전을 올려 기존 캐시 무효화

~~~text
V1 번역 시작 hash = AAA
→ 관리자가 원문을 V2로 수정
→ 저장 직전 currentHash = BBB
→ AAA != BBB
→ V1 번역 미저장
→ 최신 V2 한국어 반환
~~~

Gemini 호출은 DB 트랜잭션 밖에서 수행한다. 완료 후 짧은 저장 트랜잭션에서 원문을 다시 조회하고 hash가 같을 때만 번역을 upsert한다. hash가 다르면 같은 요청에서 재번역하지 않고 다음 영어 조회에 맡긴다.

## 8. 서비스 책임

~~~text
BoardService / EventService
        ↓
OfficialContentTranslationService
        ↓
TranslationClient
        ↓
GeminiTranslationClient
~~~

- Board/Event 도메인은 Gemini SDK에 직접 의존하지 않는다.
- TranslationClient는 번역기 교체가 가능한 port다.
- 번역 서비스가 캐시 조회, hash 비교, stale 결과 폐기, fallback을 담당한다.
- 공개 DTO 구조는 유지하고 요청 언어의 값을 기존 title/content 등에 담는다.
- titleKo/titleEn을 동시에 노출하지 않는다.
- 관리자 API에는 번역 필드를 추가하지 않는다.

## 9. Gemini 장애 정책

- API key와 모델명은 환경 설정으로 주입하고 로그에 출력하지 않는다.
- timeout, quota, 네트워크 오류, 빈 응답, 형식 오류는 번역 실패로 처리한다.
- 사용자 조회 요청 안에서 자동 재시도하지 않고 즉시 한국어로 fallback한다.
- 실패를 DB에 캐시하지 않는다.
- 원문과 번역 전문은 일반 오류 로그에 남기지 않는다.
- 결과가 DB 필드 길이 또는 응답 검증을 통과하지 못하면 저장하지 않는다.
- 프롬프트는 원문 안의 명령을 실행하지 않고 텍스트만 번역하도록 고정한다.

## 10. API 계약

- 기존 공개 조회 DTO의 필드명과 구조를 유지한다.
- ko 요청은 한국어 원문을 반환한다.
- en 요청은 유효한 영어 캐시, 신규 영어 번역, 한국어 fallback 순서로 반환한다.
- 번역 실패를 사용자 오류 코드로 노출하지 않는다.
- Vary: Accept-Language로 언어별 캐시를 분리한다.

## 11. 필수 테스트

- 헤더 누락·미지원 값의 ko fallback
- ko 조회 시 Gemini 미호출
- 최초 en 조회의 번역 저장·반환
- 캐시 적중 시 Gemini 미호출
- 원문 수정 후 stale 번역 미사용
- 번역 중 원문 변경 시 오래된 결과 미저장
- Gemini 실패 시 한국어 반환
- 동시 insert UNIQUE 충돌 시 승자 번역 반환
- 목록 일부 번역 실패 시 전체 목록 성공
- Board 이전·다음 제목 언어 일치
- Event 번역 대상·제외 필드 검증
- Review/Inquiry의 Gemini 미호출
- 고정 오류·Bulk validation의 ko/en 검증

## 12. 이번 범위 제외

- Review/Inquiry와 신청 원본 자동 번역
- 관리자 영어 번역 수정 UI/API
- 작성 시 선번역
- 분산락·번역 큐
- 번역 실패 영구 캐시
- ko/en 이외 언어

## 13. 구현 전 남은 설정값

구조 정책은 확정됐다. 다음은 구현 착수 시 정할 운영 설정값이다.

1. Gemini 모델명
2. Gemini 요청 timeout
3. 목록 최대 page size와 한 요청의 Gemini 병렬 호출 수

권장 초기값:

- 모델명: 설정 파일·환경변수로 교체 가능하게 관리
- timeout: 10초
- 목록 번역 동시 실행: 최대 3개
- 무제한 병렬 호출 금지
