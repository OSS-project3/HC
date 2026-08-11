# 이미지 자산 안내

`public/images`의 파일은 빌드 시 `/images/...` 경로로 그대로 제공됩니다.

| 폴더 | 용도 | 주요 연결 위치 |
| --- | --- | --- |
| `background/` | 메인 배경과 세종대왕 이미지 | `src/components/home/home.css` |
| `cards/width/` | 가로형 카드 앞·뒷면 | `src/data/cards.ts`, 홈·신청 견본 |
| `cards/length/` | 세로형 카드 앞·뒷면 | `src/data/cards.ts`, 홈·신청 견본 |
| `common/` | 전통 창호와 회사 약도 | 홈 서비스 섹션, `CompanyPage.tsx` |
| `company/` | 인사말 인물 사진 | `GreetingsPage.tsx` |
| `events/` | 부스 운영·브랜드 협업 WebP | `EventsPage.tsx` |
| `logo/` | 브랜드 심볼과 이팝나무 | 공통 로고, 회사 소개 |
| `merchandise/` | 기념품·결과물 | `MerchandiseSection.tsx` |
| `partners/` | 협력기관 로고 | `src/data/partners.ts` |
| `support/` | 고객지원·마이페이지 장식 | 지원 및 마이페이지 |
| `zodiac/` | 십이지 캐릭터 12종 | `src/data/zodiac.ts` |

## 관리 원칙

- 실제 사용 경로를 코드나 데이터에 연결한 뒤 자산을 추가합니다.
- 카드 파일은 `*-front`와 `*-back` 쌍을 유지합니다.
- 사진은 가능한 WebP로 최적화하고, 로고·아이콘은 원본 품질을 보존합니다.
- `dist`의 복사본은 직접 수정하지 않습니다.
- 행사사업 사진은 현재 AI 생성 임시 자산이며 최종 승인 사진 수급 시 같은 경로로 교체할 수 있습니다.
