/**
 * Partner / affiliated institutions. Logos in `public/images/partners/`.
 * Logos sit in fixed-size boxes with object-fit: contain — never recoloured.
 */
export interface Partner {
  id: string;
  name: string;
  logo: string;
}

const names = [
  "KAIST",
  "고용노동부",
  "성평등가족부",
  "보건복지부",
  "외교부",
  "국가보훈처",
  "국가유산청",
  "국가유산진흥원",
  "국립박물관문화재단",
  "한국관광공사",
  "한국콘텐츠진흥원",
  "한국디자인진흥원",
  "한국산업인력공단",
  "한국산업기술문화재단",
  "한국청소년활동진흥원",
  "한국특허전략개발원",
  "한국효문화진흥원",
  "한국여행협회",
  "한국MICE협회",
  "근로복지공단",
  "대한체육회",
  "대한태권도협회",
  "서울대학교",
  "고려대학교",
  "전북대학교",
  "충남고등학교",
  "서울특별시",
  "서울문화재단",
  "전북특별자치도",
  "전주시",
  "강원특별자치도",
  "제주특별자치도",
  "하이브",
];

export const partners: Partner[] = names.map((name) => ({
  id: name,
  name,
  logo: `/images/partners/${name}.png`,
}));
