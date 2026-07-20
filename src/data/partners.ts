/**
 * Partner / affiliated institutions.
 *
 * The logos in the mock-up are placeholders "roughly placed" by the client.
 * Real logos will be delivered later — set `logo` to the asset URL and it is
 * rendered inside a fixed box with `object-fit: contain` (never recolour a
 * partner logo). Until then a text chip stands in.
 */
export interface Partner {
  id: string;
  name: string;
  logo?: string;
}

export const partners: Partner[] = [
  { id: "kaist", name: "KAIST" },
  { id: "mogef", name: "여성가족부" },
  { id: "mohw", name: "보건복지부" },
  { id: "kohi", name: "한국건강증진개발원" },
  { id: "kta", name: "전북특별자치도" },
  { id: "kata-1", name: "한국어교원협회" },
  { id: "korean-council", name: "한국어교원협의회" },
  { id: "hrdk", name: "한국산업인력공단" },
  { id: "kywa", name: "한국청소년활동진흥원" },
  { id: "kista", name: "KISTA" },
  { id: "seoul-dokrip", name: "서울독립시" },
  { id: "kimec", name: "KIMEC" },
  { id: "kata-2", name: "한국아동청소년원" },
  { id: "jeonju", name: "전주시" },
  { id: "heritage-1", name: "국가유산청" },
  { id: "heritage-2", name: "국가유산진흥원" },
  { id: "korea-sports", name: "국기보운지" },
  { id: "seoul-univ", name: "서울대학교" },
  { id: "labor-welfare", name: "근로복지" },
  { id: "kata-3", name: "한국어교원협회" },
  { id: "coop", name: "협동조합" },
  { id: "kidp", name: "한국디자인진흥원" },
  { id: "amif", name: "국립박물관문화재단" },
  { id: "kor-museum", name: "고려대학교박물관" },
  { id: "welfare-corp", name: "근로복지공단" },
  { id: "gangwon", name: "강원특별자치도" },
  { id: "daean", name: "대안체육회" },
  { id: "korea-sport-council", name: "대한체육회" },
  { id: "chungnam-hs", name: "충남고등학교" },
  { id: "korean-arch", name: "진독미마교" },
  { id: "chungnam-univ", name: "충남고등학교" },
];
