/**
 * The 12 Korean zodiac signs ("십이지신").
 * Artwork provided in `public/images/zodiac/`.
 */
export interface ZodiacSign {
  id: string;
  nameKo: string;
  nameEn: string;
  emoji: string;
  color: string;
  image?: string;
}

const img = (ko: string) => `/images/zodiac/B_${ko}.png`;

export const zodiacSigns: ZodiacSign[] = [
  { id: "rat", nameKo: "쥐", nameEn: "Rat", emoji: "🐀", color: "#8b8378", image: img("쥐") },
  { id: "ox", nameKo: "소", nameEn: "Ox", emoji: "🐂", color: "#7a5c3e", image: img("소") },
  { id: "tiger", nameKo: "호랑이", nameEn: "Tiger", emoji: "🐅", color: "#c08a4a", image: img("호랑이") },
  { id: "rabbit", nameKo: "토끼", nameEn: "Rabbit", emoji: "🐇", color: "#d67c86", image: img("토끼") },
  { id: "dragon", nameKo: "용", nameEn: "Dragon", emoji: "🐉", color: "#5e8a63", image: img("용") },
  { id: "snake", nameKo: "뱀", nameEn: "Snake", emoji: "🐍", color: "#2f6fb0", image: img("뱀") },
  { id: "horse", nameKo: "말", nameEn: "Horse", emoji: "🐎", color: "#c39a4e", image: img("말") },
  { id: "goat", nameKo: "양", nameEn: "Goat", emoji: "🐐", color: "#9a9187", image: img("양") },
  { id: "monkey", nameKo: "원숭이", nameEn: "Monkey", emoji: "🐒", color: "#8a6b52", image: img("원숭이") },
  { id: "rooster", nameKo: "닭", nameEn: "Rooster", emoji: "🐓", color: "#a8663c", image: img("닭") },
  { id: "dog", nameKo: "개", nameEn: "Dog", emoji: "🐕", color: "#7f776d", image: img("개") },
  { id: "pig", nameKo: "돼지", nameEn: "Pig", emoji: "🐖", color: "#cf7f84", image: img("돼지") },
];
