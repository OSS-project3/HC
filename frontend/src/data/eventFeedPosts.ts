export interface FeedPost {
  date: string;
  title: string;
  place: string;
  host: string;
  company?: string;
  logoUrl?: string;
  cardLabel: string;
  text: string;
  image?: string;
}

const BOOTH_TEXT =
  "부스를 찾은 방문객에게 한글 오행으로 지은 한국 이름과 카드를 현장에서 제작해 전달했습니다. 참가자와 함께한 인증 사진과 현장 후기를 이곳에 기록으로 남깁니다.";

export const boothPosts: FeedPost[] = [
  { date: "2026. 12", title: "서울공예트렌드페어", place: "서울 코엑스 Hall C", host: "(재)한국공예·디자인문화진흥원", cardLabel: "명예한국인증 · 방문증", text: BOOTH_TEXT, image: "/images/events/booth-hero.webp" },
  { date: "2026. 10", title: "한국전통문화박람회", place: "경주 화백컨벤션센터", host: "문화체육관광부", cardLabel: "명예한국인증 · 학생증", text: BOOTH_TEXT, image: "/images/events/booth-calligraphy.webp" },
  { date: "2026. 08", title: "한글주간 문화행사", place: "국립한글박물관", host: "국립한글박물관", cardLabel: "방문증", text: BOOTH_TEXT, image: "/images/events/booth-display.webp" },
  { date: "2026. 06", title: "부산국제관광전", place: "부산 벡스코", host: "부산광역시", cardLabel: "방문증", text: BOOTH_TEXT, image: "/images/events/booth-card-delivery.webp" },
  { date: "2026. 04", title: "외국인 유학생 문화 교류전", place: "서울글로벌센터", host: "서울글로벌센터", cardLabel: "학생증 · 명예시민증", text: BOOTH_TEXT, image: "/images/events/collaboration-1.webp" },
  { date: "2026. 03", title: "K-컬처 관광 설명회", place: "인천 송도컨벤시아", host: "한국관광공사", cardLabel: "방문증", text: BOOTH_TEXT, image: "/images/events/collaboration-2.webp" },
  { date: "2026. 02", title: "세계 한글 체험 부스", place: "부산 문화회관", host: "부산문화재단", cardLabel: "명예한국인증", text: BOOTH_TEXT, image: "/images/events/collaboration-3.webp" },
  { date: "2026. 01", title: "국제 교류 환영 행사", place: "제주국제컨벤션센터", host: "제주특별자치도", cardLabel: "명예시민증 · 방문증", text: BOOTH_TEXT, image: "/images/events/collaboration-4.webp" },
];

export const BOOTH_GALLERY = [
  "/images/events/booth-hero.webp",
  "/images/events/booth-calligraphy.webp",
  "/images/events/booth-display.webp",
  "/images/events/booth-card-delivery.webp",
];

export const collabPosts: FeedPost[] = [
  { date: "2027.01.15", company: "Samsung", logoUrl: "/images/company-logos/samsung-wordmark.svg", title: "삼성 글로벌 임직원 한국 이름 체험", place: "삼성전자 글로벌 캠퍼스", host: "Samsung", cardLabel: "명예한국인증 · 방문증", text: "해외 임직원 온보딩 행사에 한글 이름 추천과 명예한국인증 발급 체험을 연계했습니다.", image: "/images/events/corporate-samsung.webp" },
  { date: "2027.01.08", company: "NAVER", logoUrl: "/images/company-logos/naver-wordmark.svg", title: "네이버 글로벌 파트너 문화 프로그램", place: "네이버 1784", host: "NAVER", cardLabel: "명예시민증", text: "글로벌 파트너 방문 일정에 맞춰 한국 이름 카드와 디지털 기념 콘텐츠를 제공했습니다.", image: "/images/events/corporate-naver.webp" },
  { date: "2026.12.22", company: "Hyundai", logoUrl: "/images/company-logos/hyundai-wordmark.svg", title: "현대 모빌리티 초청 고객 행사", place: "현대 모터스튜디오", host: "Hyundai", cardLabel: "방문증", text: "해외 초청 고객에게 한글 이름 방문증을 발급하고 브랜드 투어 경험과 연결했습니다.", image: "/images/events/corporate-hyundai.webp" },
  { date: "2026.12.12", company: "Kakao", logoUrl: "/images/company-logos/kakao-wordmark.svg", title: "카카오 외국인 크리에이터 밋업", place: "카카오 판교 아지트", host: "Kakao", cardLabel: "명예한국인증", text: "콘텐츠 크리에이터 교류 행사에서 한글 이름 카드 제작 부스를 운영했습니다.", image: "/images/events/corporate-kakao.webp" },
  { date: "2026.11.26", company: "LG", logoUrl: "/images/company-logos/lg-wordmark.svg", title: "LG 글로벌 고객 초청 문화 체험", place: "LG 사이언스파크", host: "LG", cardLabel: "명예시민증", text: "글로벌 고객 초청 행사에 한국 이름 풀이와 카드 수령 경험을 더했습니다.", image: "/images/events/collaboration-5.webp" },
  { date: "2026.11.18", company: "Kia", logoUrl: "/images/company-logos/kia-wordmark.svg", title: "기아 해외 딜러 네트워크 교류회", place: "기아 브랜드 체험관", host: "Kia", cardLabel: "방문증", text: "해외 딜러 초청 프로그램에서 참가자별 한글 이름 방문증을 제작했습니다.", image: "/images/events/collaboration-6.webp" },
  { date: "2026.10.30", company: "LINE", logoUrl: "/images/company-logos/line.svg", title: "라인 글로벌 팀 문화 교류 행사", place: "라인 오피스 라운지", host: "LINE", cardLabel: "학생증 · 방문증", text: "다국적 팀 교류 행사에서 한글 이름 카드와 팀별 기념 촬영을 연계했습니다.", image: "/images/events/collaboration-7.webp" },
  { date: "2026.10.14", company: "Google", logoUrl: "/images/company-logos/google-wordmark.svg", title: "구글 스타트업 캠퍼스 파트너 데이", place: "스타트업 캠퍼스", host: "Google", cardLabel: "명예한국인증", text: "해외 창업가 네트워킹 행사에 한국 이름 추천 카드 체험을 구성했습니다.", image: "/images/events/collaboration-8.webp" },
  { date: "2026.09.28", company: "Samsung", logoUrl: "/images/company-logos/samsung.svg", title: "삼성 글로벌 협력사 초청 데이", place: "삼성 디지털시티", host: "Samsung", cardLabel: "방문증", text: "해외 협력사 방문 일정에 맞춰 한글 이름 방문증과 현장 기념 촬영 프로그램을 운영했습니다.", image: "/images/events/collaboration-1.webp" },
  { date: "2026.09.12", company: "NAVER", logoUrl: "/images/company-logos/naver.svg", title: "네이버 해외 인턴 문화 온보딩", place: "네이버 1784", host: "NAVER", cardLabel: "학생증 · 명예시민증", text: "해외 인턴 참가자를 대상으로 한국 이름 추천과 디지털 카드 발급 체험을 제공했습니다.", image: "/images/events/collaboration-2.webp" },
  { date: "2026.08.26", company: "Kakao", logoUrl: "/images/company-logos/kakao.svg", title: "카카오 글로벌 파트너 교류회", place: "카카오 판교 아지트", host: "Kakao", cardLabel: "명예한국인증", text: "파트너 교류 행사에서 참가자별 한글 이름 카드와 협업 기록 콘텐츠를 함께 제작했습니다.", image: "/images/events/collaboration-3.webp" },
  { date: "2026.08.09", company: "LG", logoUrl: "/images/company-logos/lg.svg", title: "LG 해외 연구원 환영 프로그램", place: "LG 사이언스파크", host: "LG", cardLabel: "방문증", text: "해외 연구원 방문 프로그램에 한국 이름 풀이와 방문증 수령 경험을 더했습니다.", image: "/images/events/collaboration-4.webp" },
  { date: "2026.07.24", company: "Hyundai", logoUrl: "/images/company-logos/hyundai.svg", title: "현대 글로벌 고객 브랜드 투어", place: "현대 모터스튜디오", host: "Hyundai", cardLabel: "명예시민증", text: "브랜드 투어 참가 고객에게 한글 이름 카드와 맞춤형 기념 이미지를 제공했습니다.", image: "/images/events/collaboration-5.webp" },
  { date: "2026.07.10", company: "Kia", logoUrl: "/images/company-logos/kia.svg", title: "기아 글로벌 트레이닝 캠프", place: "기아 오토랜드", host: "Kia", cardLabel: "학생증 · 방문증", text: "해외 교육 참가자에게 프로그램 전용 한글 이름 학생증과 방문증을 발급했습니다.", image: "/images/events/collaboration-6.webp" },
  { date: "2026.06.21", company: "LINE", logoUrl: "/images/company-logos/line.svg", title: "라인 다국적 팀 리더 워크숍", place: "라인 오피스 라운지", host: "LINE", cardLabel: "명예한국인증", text: "다국적 팀 리더 워크숍에 한글 이름 체험과 팀별 인증 카드 제작을 연계했습니다.", image: "/images/events/collaboration-7.webp" },
  { date: "2026.06.06", company: "Google", logoUrl: "/images/company-logos/google.svg", title: "구글 글로벌 스타트업 밋업", place: "스타트업 캠퍼스", host: "Google", cardLabel: "방문증", text: "해외 스타트업 참가자에게 한국 이름 방문증과 네트워킹용 디지털 콘텐츠를 제공했습니다.", image: "/images/events/collaboration-8.webp" },
];

export const COLLAB_GALLERY = [
  "/images/events/collaboration-1.webp",
  "/images/events/collaboration-2.webp",
  "/images/events/collaboration-3.webp",
  "/images/events/collaboration-5.webp",
  "/images/events/collaboration-7.webp",
];

export function loadFeedPosts(key: string, fallback: FeedPost[]) {
  try {
    const saved = JSON.parse(localStorage.getItem(`managed-content:${key}`) || "null") as FeedPost[] | null;
    if (!Array.isArray(saved)) return fallback;
    const savedKeys = new Set(saved.map((item) => `${item.date}::${item.title}`));
    return [...saved, ...fallback.filter((item) => !savedKeys.has(`${item.date}::${item.title}`))];
  } catch {
    return fallback;
  }
}

export function saveFeedPosts(key: string, items: FeedPost[]) {
  localStorage.setItem(`managed-content:${key}`, JSON.stringify(items));
}

export const emptyFeedPost = (): FeedPost => ({
  date: new Date().toISOString().slice(0, 10),
  title: "",
  place: "",
  host: "",
  cardLabel: "",
  text: "",
  image: "",
  company: "",
  logoUrl: "",
});
