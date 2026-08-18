# 프론트 ↔ 백엔드 통합 테스트 리포트

실행일: 2026-08-18
대상: 프론트 API 연동(후기·공지/FAQ·행사·신청·인증·내 후기) 이후 프론트↔백엔드 실연결 검증
방법: Docker Compose로 풀스택(backend + redis + frontend Nginx)을 실제 기동하고, 프론트 `services/api.ts`가 사용하는 것과 동일한 경로·쿼리로 라이브 호출.

---

## 1. 테스트 환경

| 항목 | 값 |
|---|---|
| 기동 방식 | `docker compose up -d --build` (루트 `docker-compose.yml`) |
| 백엔드 | Spring Boot, H2 인메모리, 포트 8080, placeholder env(JWT/OAuth/AWS) |
| 프론트 | Vite 빌드 → Nginx, `/api`·`/oauth2` 백엔드 프록시(same-origin) |
| Redis | 토큰 세션용, compose 내부 네트워크 |
| 데이터 | H2 인메모리라 초기값 없음 → **비어 있으나 형식이 정확한 응답**으로 계약 검증 |

> 인증(OAuth) 흐름은 실제 Google/Naver 자격증명이 필요해 placeholder로는 로그인 세션을 만들 수 없다. 따라서 비인증 공개 API는 전 구간 검증, 인증 필요 API는 "매핑 존재 + 401 인가 동작"까지 검증했다.

---

## 2. 결과 요약

**전체 판정: 프론트↔백엔드 API 연결은 계약(경로·파라미터·응답 envelope·인증) 수준에서 정상.**

백엔드 직접(:8080)과 프론트 Nginx same-origin 프록시(`/api/*`) 양쪽에서 동일하게 통과.

### 2.1 공개 GET — 응답 envelope 검증 (프론트 `PageResponse`/`ApiEnvelope`와 대조)

| 프론트 호출 | 경로 | 응답 | 판정 |
|---|---|---|---|
| `api.listReviews` | `GET /api/reviews?page=0&size=9` | `{"success":true,"data":{"content":[],"page":0,"size":9,"totalElements":0,"totalPages":0}}` | ✅ |
| `api.listBoards(NOTICE)` | `GET /api/boards?type=NOTICE` | 동일 `PageResponse` 형식 | ✅ |
| `api.listBoards(FAQ)` | `GET /api/boards?type=FAQ` | 동일 | ✅ |
| `api.listEvents(BOOTH)` | `GET /api/events?type=BOOTH` | `...,"size":10,...` | ✅ |
| `api.listEvents(COLLABORATION)` | `GET /api/events?type=COLLABORATION` | 동일 | ✅ |

응답 필드(`content/page/size/totalElements/totalPages`)와 최상위 `success/data`가 프론트 타입 정의와 정확히 일치.

### 2.2 인증 필요 엔드포인트 — 매핑 존재 + 인가 동작

비로그인 호출 시 **404가 아닌 401** 반환(= 라우트 매핑 존재, 인증 게이트 동작).

| 경로 | 메서드 | 응답 |
|---|---|---|
| `/api/users/me` | GET | 401 |
| `/api/my/reviews` | GET | 401 (이번에 신규 연동한 "내 후기" 대상) |
| `/api/reviews` | POST | 401 |
| `/api/admin/events/{id}` | PATCH | 401 |
| `/api/admin/boards/{id}` | DELETE | 401 |

### 2.3 에러 envelope — 프론트 `ApiError` 파싱과 대조

| 경로 | 요청 | 응답 |
|---|---|---|
| `POST /api/applications/lookup` | `{"method":"card","keyValue":"NONEXIST"}` | `{"success":false,"errorCode":"NOT_FOUND","errorMessage":"데이터를 찾을 수 없습니다."}` ✅ |

`errorCode`/`errorMessage`가 프론트 `request()`의 `ApiError` 생성 계약과 일치.

### 2.4 Same-origin 프록시 (실제 브라우저 흐름)

프론트 Nginx `/api/*` → 백엔드 프록시가 위 모든 경로에서 백엔드 직접 호출과 동일 결과 반환. 프론트가 올바른 앱(`<title>(주)한글과 세종 · 한글 오행 기반 한국 이름 발급</title>`) 서빙 확인. → 프론트 `api.ts`의 `VITE_API_BASE_URL=""`(동일 출처) + 쿠키 인증 전제가 실제로 성립.

---

## 3. 발견한 이슈 (API 연동 결함 아님 — 로컬 환경/배포)

### 3.1 [환경] 호스트 포트 3000 충돌
- 다른 프로젝트 컨테이너 `msa-shop-web-1`("픽셀 상점")이 `0.0.0.0:3000`을 이미 점유.
- 그 결과 compose의 frontend가 포트 바인딩 실패 → 네트워크 미참여 → Nginx `host not found in upstream "backend"`로 크래시 루프.
- **영향**: 이 PC에서 `docker-compose.yml` 기본 `"3000:80"` 그대로는 프론트가 뜨지 않는다.
- **검증 우회**: 프론트 이미지를 빈 포트(3100) + 동일 네트워크로 일회성 기동해 프록시 전 구간 통과 확인.
- **조치 방안**: `docker-compose.yml`의 프론트 `ports`를 비어 있는 포트로 변경하거나, `msa-shop-web-1`을 중지 후 기동.

---

## 4. 이번 테스트로 검증되지 않은 부분 (한계)

| 항목 | 사유 |
|---|---|
| 인증된 쓰기 흐름(후기/공지/행사 작성·수정·삭제, 내 후기 목록 실데이터) | OAuth 로그인에 실제 Google/Naver 자격증명 필요 → placeholder로 세션 생성 불가 |
| multipart 업로드(이미지/첨부/ZIP) 및 S3 presigned URL | 실제 AWS 자격증명·버킷 필요 |
| 비어 있지 않은 목록/페이지네이션의 실데이터 매핑 | H2 인메모리 초기 데이터 없음(형식만 검증) |
| 신청 생성/조회의 인증 사용자 왕복 | 위 인증 한계와 동일 |

> 위 항목은 실 자격증명(OAuth·AWS)과 시드 데이터가 갖춰진 스테이징 환경에서 별도 검증 필요.

---

## 5. 재현 방법

```bash
# 1) 풀스택 기동 (포트 3000이 비어 있어야 함)
docker compose up -d --build

# 2) 공개 엔드포인트 계약 확인 (백엔드 직접)
curl -s "http://localhost:8080/api/reviews?page=0&size=9"
curl -s "http://localhost:8080/api/boards?type=NOTICE"
curl -s "http://localhost:8080/api/events?type=BOOTH"

# 3) same-origin 프록시 확인 (프론트 경유)
curl -s "http://localhost:3000/api/reviews?page=0&size=9"

# 4) 정리
docker compose down
```

포트 3000이 점유된 경우: 프론트만 빈 포트로 일회성 기동
```bash
docker run -d --rm --name hc-fe-test --network hc_default -p 3100:80 hc-frontend
curl -s "http://localhost:3100/api/reviews?page=0&size=9"
docker rm -f hc-fe-test
```

---

## 6. 결론

- 프론트가 실제로 호출하는 공개 API(후기·공지/FAQ·행사)와 조회/에러 응답이 라이브 백엔드에서 프론트 타입 계약과 **정확히 일치**한다.
- 인증 필요 엔드포인트는 모두 매핑이 존재하고 401 인가가 동작한다(신규 `GET /api/my/reviews` 포함).
- same-origin 프록시 경로가 실제로 성립해, 배포 시 브라우저 쿠키 인증 전제가 유효하다.
- 남은 검증(인증 CRUD·업로드·실데이터)은 OAuth/AWS 자격증명이 있는 환경에서 진행한다.
- 유일한 차단 이슈는 로컬 포트 3000 충돌(다른 프로젝트)로, API 연동과 무관하다.
