import type { CardType } from "./cards";

export interface ReviewPost {
  id: string;
  title: string;
  content: string;
  author: string;
  authorEmail: string;
  createdAt: string;
  applicantType: "personal" | "organization";
  cardType: CardType;
  imageUrl?: string;
  imageUrls?: string[];
}

const STORAGE_KEY = "review-posts";
const legacyReviewImages: Record<string, string> = {
  "/images/cards/main%20sec1%20ex1.png": "/images/cards/width/kor-tiger-front.webp",
  "/images/cards/main%20sec1%20ex2.png": "/images/cards/width/city-dragon2-front.webp",
  "/images/cards/main%20sec2%20ex2.png": "/images/cards/length/visit-tiger-front.webp",
};
const sampleReviewImages: Record<string, string> = {
  "review-1": "/images/cards/width/kor-tiger-front.webp",
  // review-2 는 사진 없는 글씨 전용 후기 카드다.
  "review-3": "/images/cards/length/visit-tiger-front.webp",
  "review-4": "/images/cards/width/stu-pig-front.webp",
};
const sampleReviewImageSets: Record<string, string[]> = {
  "review-5": [
    "/images/events/booth-hero.webp",
    "/images/events/booth-calligraphy.webp",
    "/images/events/booth-display.webp",
    "/images/events/booth-card-delivery.webp",
  ],
};

function normalizeImageUrl(imageUrl?: string) {
  return imageUrl ? legacyReviewImages[imageUrl] ?? imageUrl : undefined;
}

export function getReviewImageUrls(review: Pick<ReviewPost, "id" | "imageUrl" | "imageUrls">) {
  const urls = review.imageUrls?.map(normalizeImageUrl).filter((url): url is string => Boolean(url)) ?? [];
  const legacy = normalizeImageUrl(review.imageUrl);
  const fallbackSet = sampleReviewImageSets[review.id] ?? [];
  const fallbackSingle = sampleReviewImages[review.id];
  return Array.from(new Set([...urls, ...(legacy ? [legacy] : []), ...fallbackSet, ...(fallbackSingle ? [fallbackSingle] : [])]));
}

export function getReviewImageUrl(review: Pick<ReviewPost, "id" | "imageUrl" | "imageUrls">) {
  return getReviewImageUrls(review)[0];
}

/** Canonical sample image for a review id — used as an onError fallback when a
 *  stored imageUrl (e.g. stale localStorage data) fails to load. */
export function getReviewFallbackImageUrl(id: string): string | undefined {
  return sampleReviewImageSets[id]?.[0] ?? sampleReviewImages[id];
}
const initialReviews: ReviewPost[] = [
  { id: "review-1", title: "한국에서의 추억이 이름과 카드로 남았어요.", content: "이름의 뜻을 함께 설명해 주셔서 여행이 끝난 뒤에도 특별한 기억으로 간직하고 있습니다.", author: "윤은재", authorEmail: "sample1@example.com", createdAt: "2026-08-01", applicantType: "personal", cardType: "honorary-korean", imageUrl: "/images/cards/width/kor-tiger-front.webp" },
  { id: "review-2", title: "행사 참가자에게 색다른 경험을 선물했습니다.", content: "신청부터 수령까지 과정이 명확했고, 참가자들의 만족도도 높아 다음 행사에서도 활용하고 싶습니다.", author: "문화행사 운영팀", authorEmail: "sample2@example.com", createdAt: "2026-07-28", applicantType: "organization", cardType: "honorary-citizen" },
  { id: "review-3", title: "한국 문화를 자연스럽게 소개할 수 있었습니다.", content: "방문객 정보에 맞춘 카드가 행사 안내와 기념품 역할을 함께해 현장 반응이 좋았습니다.", author: "한문화교류원", authorEmail: "sample3@example.com", createdAt: "2026-07-21", applicantType: "organization", cardType: "visitor", imageUrl: "/images/cards/length/visit-tiger-front.webp" },
  { id: "review-4", title: "모바일 카드로 간편하게 확인하고 공유했어요.", content: "실물 카드와 함께 모바일 카드도 받을 수 있어 가족과 친구들에게 쉽게 보여줄 수 있었습니다.", author: "이소연", authorEmail: "sample4@example.com", createdAt: "2026-07-15", applicantType: "personal", cardType: "student", imageUrl: "/images/cards/width/stu-pig-front.webp" },
  { id: "review-5", title: "부스 현장의 순간을 여러 장으로 남겼습니다.", content: "현장에서 받은 한국 이름 카드와 부스 분위기가 좋아 사진을 여러 장 남겼습니다. 가족에게 보여주기에도 좋고, 카드마다 담긴 의미를 다시 확인할 수 있어 만족스러웠습니다.", author: "마리아 킴", authorEmail: "sample5@example.com", createdAt: "2026-08-08", applicantType: "personal", cardType: "honorary-citizen", imageUrls: sampleReviewImageSets["review-5"] },
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
      applicantType: review.applicantType === "organization" ? "organization" as const : "personal" as const,
      cardType: review.cardType || "honorary-korean",
      imageUrl: getReviewImageUrl({ id: review.id || "", imageUrl: review.imageUrl, imageUrls: review.imageUrls }),
      imageUrls: getReviewImageUrls({ id: review.id || "", imageUrl: review.imageUrl, imageUrls: review.imageUrls }),
    }));
    const savedKeys = new Set(normalized.map((review) => review.id));
    const merged = [...normalized, ...initialReviews.filter((review) => !savedKeys.has(review.id))];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(merged));
    return merged;
  } catch { return initialReviews; }
}

export function saveReviews(reviews: ReviewPost[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(reviews));
}

export function findReview(id?: string) {
  return loadReviews().find((review) => review.id === id);
}
