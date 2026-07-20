/**
 * The 12 Korean zodiac signs ("십이지신").
 *
 * `image` will hold the final paper-cut character artwork once delivered.
 * Until then a coloured silhouette placeholder is rendered from `color` +
 * `emoji`. Swap `image` in and the UI picks it up automatically.
 */
export interface ZodiacSign {
  id: string;
  nameKo: string;
  nameEn: string;
  emoji: string;
  color: string;
  image?: string;
}

export const zodiacSigns: ZodiacSign[] = [
  { id: "rat", nameKo: "쥐", nameEn: "Rat", emoji: "🐀", color: "#8b8378" },
  { id: "ox", nameKo: "소", nameEn: "Ox", emoji: "🐂", color: "#7a5c3e" },
  { id: "tiger", nameKo: "호랑이", nameEn: "Tiger", emoji: "🐅", color: "#c08a4a" },
  { id: "rabbit", nameKo: "토끼", nameEn: "Rabbit", emoji: "🐇", color: "#d67c86" },
  { id: "dragon", nameKo: "용", nameEn: "Dragon", emoji: "🐉", color: "#5e8a63" },
  { id: "snake", nameKo: "뱀", nameEn: "Snake", emoji: "🐍", color: "#2f6fb0" },
  { id: "horse", nameKo: "말", nameEn: "Horse", emoji: "🐎", color: "#c39a4e" },
  { id: "goat", nameKo: "양", nameEn: "Goat", emoji: "🐐", color: "#9a9187" },
  { id: "monkey", nameKo: "원숭이", nameEn: "Monkey", emoji: "🐒", color: "#8a6b52" },
  { id: "rooster", nameKo: "닭", nameEn: "Rooster", emoji: "🐓", color: "#a8663c" },
  { id: "dog", nameKo: "개", nameEn: "Dog", emoji: "🐕", color: "#7f776d" },
  { id: "pig", nameKo: "돼지", nameEn: "Pig", emoji: "🐖", color: "#cf7f84" },
];
