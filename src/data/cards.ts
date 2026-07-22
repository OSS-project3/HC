/**
 * Card design catalogue.
 *
 * `front` = top slot image, `back` = bottom slot image in the design gallery.
 * Images are the temporary "design front/back" set in public/images/cards/.
 */
export type CardType = "honorary-korean" | "honorary-citizen" | "student" | "visitor";

export interface CardDesign {
  id: string;
  name: string;
  cardType: CardType;
  orientation: "landscape" | "portrait";
  accent: string;
  sample: {
    titleKo: string;
    titleEn: string;
    nameKo: string;
    nameHanja?: string;
    nameEn: string;
    cardNumber: string;
    address: string;
    issuedAt: string;
    issuer: string;
  };
  reading: {
    nameKo: string;
    nameHanja?: string;
    nameEn: string;
    meaning: string;
    poem: string[];
  };
  /** Top-slot image */
  front?: string;
  /** Bottom-slot image */
  back?: string;
}

// Provided image files (filenames contain spaces → URL-encoded).
const df = (n: number) => `/images/cards/design%20front${n}.png`;
const db = (n: number) => (n === 1 ? `/images/cards/design%20back%201.png` : `/images/cards/design%20back${n}.png`);

const monkeyReading = {
  nameKo: "윤 은 재",
  nameHanja: "(尹 殷 齋)",
  nameEn: "Yoon Eun-jae",
  meaning: "은나라 은(殷) 재계할 재(齋)",
  poem: [
    "널리 축복을 세상에 펼치는 성스러운 날을 위해",
    "몸과 마음을 경건하고 가지런히 하며, 몸과 마음이 하나 되어",
    "성대한 열매를 맺고 축복된 세상을 이룬다.",
  ],
};

function makeSeries(
  cardType: CardType,
  orientation: "landscape" | "portrait",
  accent: string,
  sample: CardDesign["sample"],
  reading: CardDesign["reading"],
  slots: { front: string; back: string }[],
): CardDesign[] {
  return slots.map((imgs, i) => ({
    id: `${cardType}-${String(i + 1).padStart(2, "0")}`,
    name: `${sample.titleKo} ${String(i + 1).padStart(2, "0")}`,
    cardType,
    orientation,
    accent,
    sample,
    reading,
    front: imgs.front,
    back: imgs.back,
  }));
}

// 명예한국인증 — 3 columns: top row front1×3, bottom row back1×3.
export const honoraryKoreanCards = makeSeries(
  "honorary-korean",
  "landscape",
  "#b23b4a",
  {
    titleKo: "명예한국인증",
    titleEn: "CERTIFICATE OF HONORARY CITIZENSHIP",
    nameKo: "윤 은 재",
    nameHanja: "(尹 殷 齋)",
    nameEn: "Yoon Eun-jae",
    cardNumber: "HN-KR-2609-1188",
    address: "대한민국 경기도 수원시 장안구 서부로 2166",
    issuedAt: "2026.01.15",
    issuer: "발행처 대한민국 고용노동부",
  },
  monkeyReading,
  [
    { front: df(1), back: db(1) },
    { front: df(1), back: db(1) },
    { front: df(1), back: db(1) },
  ],
);

// 명예시민증 — 3 columns: top row front2×3, bottom row back2×3.
export const honoraryCitizenCards = makeSeries(
  "honorary-citizen",
  "landscape",
  "#8a5a4a",
  {
    titleKo: "한국명예시민증",
    titleEn: "REPUBLIC OF KOREA HONORARY CITIZENSHIP",
    nameKo: "정 은 성",
    nameHanja: "(鄭 銀 星)",
    nameEn: "Jeong Eun-seong",
    cardNumber: "HN-KR-2609-0134",
    address: "대한민국 서울특별시 마포구 성암로 3",
    issuedAt: "2026.01.15",
    issuer: "발행처 서울특별시 마포구",
  },
  {
    nameKo: "정 은 성",
    nameHanja: "(鄭 銀 星)",
    nameEn: "Jeong Eun-seong",
    meaning: "은빛 은(銀) 별 성(星)",
    poem: [
      "재화와 재물로 넉넉하고 풍요로운 삶을 살며,",
      "하늘의 별처럼 빛나 명예로운 이름을 얻고",
      "밝게 빛나는 부유하고 명예로운 인생을 산다.",
    ],
  },
  [
    { front: df(2), back: db(2) },
    { front: df(2), back: db(2) },
    { front: df(2), back: db(2) },
  ],
);

// 학생증 — 2 columns. Top row [front3, back3], bottom row [front4, back4].
export const studentCards = makeSeries(
  "student",
  "portrait",
  "#4a6a8a",
  {
    titleKo: "학생증",
    titleEn: "STUDENT ID",
    nameKo: "이 소 연",
    nameHanja: "(李 素 淵)",
    nameEn: "Lee So-yeon",
    cardNumber: "HN-KR-2602-3515",
    address: "대한민국 전주시 완산구 최명희길 11",
    issuedAt: "2026.01.15",
    issuer: "발행처 전북특별자치도 전주시",
  },
  {
    nameKo: "이 소 연",
    nameHanja: "(李 素 淵)",
    nameEn: "Lee So-yeon",
    meaning: "흴 소(素) 못 연(淵)",
    poem: [
      "본디 소박하고 절박하여",
      "바탕이 깨끗한 성품으로 맑은 기운을 내뿜으며,",
      "기품과 성실을 가득 담아 세상을 부유하게 하고 있게 빛낸다.",
    ],
  },
  [
    // 디자인별로 묶음: 1번은 가로(명예시민증+이름풀이), 2번은 세로(방문증+이름풀이).
    { front: df(3), back: db(3) }, // 이소연 — 가로 한 쌍
    { front: df(4), back: db(4) }, // 정재이 — 세로 한 쌍
  ],
);

// 방문증 — 2 columns, front5/back5 alternating (교차).
export const visitorCards = makeSeries(
  "visitor",
  "portrait",
  "#3f5b9a",
  {
    titleKo: "방문증",
    titleEn: "VISITOR",
    nameKo: "최 인 서",
    nameHanja: "(崔 仁 序)",
    nameEn: "Choi In-seo",
    cardNumber: "HN-KR-2609-1188",
    address: "대한민국 서울특별시 종로구 사직로8길 60",
    issuedAt: "2026.08.16",
    issuer: "발행처 대한민국 외교부",
  },
  {
    nameKo: "최 인 서",
    nameHanja: "(崔 仁 序)",
    nameEn: "Choi In-seo",
    meaning: "어질 인(仁) 차례 서(序)",
    poem: [
      "만세에 신의와 예절을 다하는",
      "어진 마음을 근본으로 삼으며,",
      "깊은 지혜로 모든 일이 밝고 그름을 따지고 조화로운 삶을 이룬다.",
    ],
  },
  [
    { front: df(5), back: db(5) },
    { front: db(5), back: df(5) },
  ],
);

export interface CardCategory {
  cardType: CardType;
  title: string;
  cards: CardDesign[];
}

export const cardCategories: CardCategory[] = [
  { cardType: "honorary-korean", title: "명예 한국인증", cards: honoraryKoreanCards },
  { cardType: "honorary-citizen", title: "명예 시민증", cards: honoraryCitizenCards },
  { cardType: "student", title: "학생증", cards: studentCards },
  { cardType: "visitor", title: "방문증", cards: visitorCards },
];

export const cardTypeLabels: Record<CardType, string> = {
  "honorary-korean": "명예 한국인증",
  "honorary-citizen": "명예 시민증",
  student: "학생증",
  visitor: "방문증",
};

export function findCardDesign(designId: string | null | undefined): CardDesign | undefined {
  if (!designId) return undefined;
  return cardCategories.flatMap((c) => c.cards).find((d) => d.id === designId);
}
