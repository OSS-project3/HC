# 이미지 파일 넣는 위치

모든 이미지는 이 `public/images/` 폴더에 넣습니다.
`public/` 안의 파일은 빌드 시 그대로 복사되어 `/images/...` 경로로 접근됩니다.

예) `public/images/cards/honorary-korean-01.png` → 코드에서는 `"/images/cards/honorary-korean-01.png"`

---

## 폴더별 용도 & 연결되는 데이터 파일

| 폴더 | 넣을 이미지 | 연결 파일 · 필드 |
| --- | --- | --- |
| `images/cards/` | 카드 앞면 / 이름풀이(뒷면) | `src/data/cards.ts` → 각 카드의 `front`, `back` |
| `images/zodiac/` | 12지신 캐릭터 (쥐~돼지) | `src/data/zodiac.ts` → 각 항목의 `image` |
| `images/partners/` | 협력 기관 로고 | `src/data/partners.ts` → 각 항목의 `logo` |
| `images/logo/` | 로고 심볼 · 이팝나무 | `src/components/brand/Logo.tsx` (아래 설명) |
| `images/windows/` | 전통 창문(열림/닫힘) | `src/components/home/ServiceCoreSection.tsx` |
| `images/merchandise/` | 문화 체험 기념품 | `src/components/home/MerchandiseSection.tsx` |
| `images/common/` | 세종대왕 등 기타 | 사용하는 컴포넌트 |

---

## 데이터 파일로 바로 교체되는 항목 (코드 수정 없이 경로만 입력)

### 1) 카드 — `src/data/cards.ts`
```ts
{
  id: "honorary-korean-01",
  // ...
  front: "/images/cards/honorary-korean-01-front.png", // 앞면
  back:  "/images/cards/honorary-korean-01-back.png",  // 이름풀이
}
```

### 2) 12지신 — `src/data/zodiac.ts`
```ts
{ id: "rat", nameKo: "쥐", /* ... */ image: "/images/zodiac/rat.png" }
```

### 3) 협력 기관 — `src/data/partners.ts`
```ts
{ id: "kaist", name: "KAIST", logo: "/images/partners/kaist.png" }
```

경로를 입력하면 해당 자리의 "추가 필요" 검정 박스가 자동으로 이미지로 바뀝니다.

---

## 컴포넌트에서 직접 교체하는 항목 (검정 박스 → `<img>`)

아래는 데이터 배열이 아니라 화면에 1개씩만 있는 요소라, 해당 파일에서
`ImagePlaceholder` 를 `<img src="/images/...">` 로 바꿔주면 됩니다.

- **로고 · 이팝나무** — `src/components/brand/Logo.tsx`
  가장 좋은 방법은 담당자가 말한 대로 **로고 전체를 하나의 PNG/SVG** 로 받아
  `/images/logo/logo.png` 로 교체하는 것입니다.
- **전통 창문 2개** — `src/components/home/ServiceCoreSection.tsx`
- **기념품 이미지** — `src/components/home/MerchandiseSection.tsx`

> 권장 형식: 투명 배경 PNG 또는 SVG. 로고·기관 로고·창문은 SVG가 가장 깔끔합니다.
> 파일명은 영문·숫자·하이픈(`-`)만 사용하세요.
