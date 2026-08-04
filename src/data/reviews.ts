export interface ReviewPost {
  id: string;
  title: string;
  content: string;
  author: string;
  authorEmail: string;
  createdAt: string;
}

const STORAGE_KEY = "review-posts";
const initialReviews: ReviewPost[] = [
  { id: "review-1", title: "한국에서의 추억이 이름과 카드로 남았어요.", content: "이름의 뜻을 함께 설명해 주셔서 여행이 끝난 뒤에도 특별한 기억으로 간직하고 있습니다.", author: "명예한국인증 신청자", authorEmail: "sample1@example.com", createdAt: "2026-08-01" },
  { id: "review-2", title: "행사 참가자에게 색다른 경험을 선물했습니다.", content: "신청부터 수령까지 과정이 명확했고, 참가자들의 만족도도 높아 다음 행사에서도 활용하고 싶습니다.", author: "문화행사 운영 담당자", authorEmail: "sample2@example.com", createdAt: "2026-07-28" },
  { id: "review-3", title: "한국 문화를 자연스럽게 소개할 수 있었습니다.", content: "방문객 정보에 맞춘 카드가 행사 안내와 기념품 역할을 함께해 현장 반응이 좋았습니다.", author: "기관 방문 프로그램 담당자", authorEmail: "sample3@example.com", createdAt: "2026-07-21" },
  { id: "review-4", title: "모바일 카드로 간편하게 확인하고 공유했어요.", content: "실물 카드와 함께 모바일 카드도 받을 수 있어 가족과 친구들에게 쉽게 보여줄 수 있었습니다.", author: "개인 신청자", authorEmail: "sample4@example.com", createdAt: "2026-07-15" },
];

export function loadReviews(): ReviewPost[] {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return initialReviews;
    const parsed = JSON.parse(saved) as Array<Partial<ReviewPost> & { text?: string }>;
    const normalized = parsed.map((review, index) => ({
      id: review.id || `saved-review-${index + 1}`,
      title: review.title || "제목 없는 후기",
      content: review.content || review.text || "작성된 내용이 없습니다.",
      author: review.author || "작성자",
      authorEmail: review.authorEmail || "",
      createdAt: review.createdAt || "2026-08-04",
    }));
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
    return normalized;
  } catch { return initialReviews; }
}

export function saveReviews(reviews: ReviewPost[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(reviews));
}

export function findReview(id?: string) {
  return loadReviews().find((review) => review.id === id);
}
