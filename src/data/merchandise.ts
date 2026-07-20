/** Cultural-experience merchandise shown in the "한글은 문화다" section. */
export interface MerchItem {
  id: string;
  name: string;
  emoji: string;
  image?: string;
}

export const merchandise: MerchItem[] = [
  { id: "tshirt", name: "허지안", emoji: "👕" },
  { id: "namebar", name: "이소연", emoji: "🪧" },
  { id: "stamp", name: "강하린", emoji: "🔴" },
  { id: "necklace", name: "박지호", emoji: "📿" },
  { id: "tote", name: "정민준", emoji: "👜" },
  { id: "luggage", name: "추건우", emoji: "🏷️" },
  { id: "keyring", name: "한서아", emoji: "🔑" },
  { id: "badge", name: "차유준", emoji: "🎖️" },
];

export const merchFeatures: string[] = [
  "외국인 대상 한국 문화 체험 콘텐츠",
  "개인화된 한국 이름으로 된 각종 디지털 카드",
  "SNS 공유와 실물 카드 제작 확장 가능",
];
