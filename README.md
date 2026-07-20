# (사)한글과 세종 — 서비스형 홈페이지

한글 오행 기반으로 외국인을 위한 한국 이름을 짓고,
**명예한국인증 · 명예시민증 · 학생증 · 방문증**을 제작·신청·조회할 수 있는 서비스형 홈페이지입니다.
전달받은 시안과 요구사항을 기준으로 **내용과 레이아웃을 우선 확정**했으며,
아직 전달되지 않은 이미지는 **"추가 필요" 검정 박스**로 표시해 두었습니다.

## 기술 스택

- React 18 + TypeScript
- Vite
- React Router v6
- 폰트: Pretendard (CDN)

## 실행 방법

```bash
npm install
npm run dev       # 개발 서버 (http://localhost:5173)
npm run build     # 타입체크 + 프로덕션 빌드
npm run preview   # 빌드 결과 미리보기
```

## 구현된 화면

| 경로 | 화면 | 상태 |
| --- | --- | --- |
| `/` | 메인 (히어로 · 서비스 핵심 · 기념품 · 상담문의 · 협력기관) | 구현 |
| `/design` | 카드 디자인 (카테고리별 캐러셀 + 제작 신청) | 구현 |
| `/apply` | 제작 신청 5단계 (유형→정보→파일→확인→완료) | 구현 |
| `/lookup` | 신청 조회 (번호 + 연락처 조합) | 구현 |
| `/support` | 고객지원 (공지·FAQ·제작이야기·상담문의 앵커) | 구현 |
| `/login` | 로그인 | 구현 |
| `/company` `/reviews` `/events` `/admin` | 회사소개·후기·행사사업·관리 | 시안 대기(임시 프레임) |

## 설계 원칙

- **격자무늬 미사용** — 시안의 모눈종이는 작업용 배경이며, 실제 배경은 평평한 베이지(`--color-background`)입니다.
- **이미지는 직접 그리지 않음** — 별도 이미지 파일이 없는 그래픽(로고 심볼 · 이팝나무 · 12지신 · 카드 앞면/이름풀이 · 전통 창문 · 기념품 · 기관 로고 등)은 전부 **검정 박스 "추가 필요"** 로 표시합니다. 컴포넌트: `src/components/ui/ImagePlaceholder.tsx`.
- **절대 좌표 미사용** — 3840×2160 시안 좌표를 그대로 복사하지 않고 `clamp()`·유동 그리드로 반응형 구성.
- **색상 토큰 단일 관리** — 모든 색상은 `src/styles/tokens.css` 에서만 정의(스포이드값이 아닌 담당자 표기값 사용).

## 콘텐츠 교체 지점 (데이터 기반)

이미지·문구가 늦게 전달돼도 아래 파일만 수정하면 반영됩니다.

| 항목 | 위치 |
| --- | --- |
| 색상 토큰 | `src/styles/tokens.css` |
| 폰트 | `src/styles/fonts.css` |
| 회사·계좌 정보 | `src/config/company.ts` |
| 메뉴·고객지원 앵커 | `src/config/navigation.ts` |
| 카드 디자인 목록 | `src/data/cards.ts` |
| 12지신 | `src/data/zodiac.ts` |
| 협력 기관 | `src/data/partners.ts` |
| 기념품 | `src/data/merchandise.ts` |
| 정책 문서(모달) | `src/data/policies.ts` |
| SNS | `src/data/social.ts` |

각 데이터의 `image` / `front` / `back` / `logo` 필드에 최종 URL을 넣으면 검정 박스가 실제 이미지로 자동 교체됩니다.

## 폴더 구조

```
src/
├─ pages/           페이지 (조립 역할)
│  └─ apply/        제작 신청 5단계
├─ components/
│  ├─ header/       헤더 (로고 · 내비 · 고객지원 드롭다운)
│  ├─ footer/       푸터 (정책 모달 · SNS · 사이트맵)
│  ├─ home/         메인 섹션들
│  ├─ gallery/      카드 캐러셀
│  ├─ apply/        신청 단계 컴포넌트
│  ├─ brand/        로고 · 카드 · 12지신 placeholder
│  └─ ui/           Button · Modal · Toast · ImagePlaceholder · icons
├─ features/apply/  신청 데이터 모델 · draft 상태(hook)
├─ config/          navigation · company
├─ data/            교체 가능한 콘텐츠 데이터
├─ lib/             유틸
└─ styles/          tokens · fonts · reset · forms · globals
```

## 백엔드 연동 예정 (미구현, 인터페이스만 정리)

신청 조회(`POST /api/applications/lookup`), presigned URL 기반 대용량 ZIP 업로드,
제출 후 서버 발급 신청번호, 우편번호 API, 관리자 권한 검사 등은 프론트 플레이스홀더 상태입니다.

## 미확정 · 추가 자료 대기

카드/캐릭터/이팝나무/일부 기관 로고 최종 이미지, 정확한 폰트 확정, 파일 업로드 최대 용량,
관리 메뉴 공개 여부, 회사 정보 확정값, 회사소개·후기·행사사업·관리·고객지원 세부 시안.
