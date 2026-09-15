import type { EventListItem } from "../services/api";

export interface FeedPost {
  /** Backend event id (present for API-loaded posts). */
  id?: number;
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

/**
 * Map a backend Event list item into the feed view model.
 * ✅ 2026-08-24: 백엔드가 `company`/`logoUrl`을 제공하므로 협업(COLLABORATION) 로고·회사명도 매핑한다.
 * (예전의 boothPosts/collabPosts 정적 배열과 localStorage 헬퍼(loadFeedPosts/saveFeedPosts)는 실 API
 * 연동 완료 후 어디서도 쓰이지 않아 제거했다 — 2026-09-14, FRONTEND_API_GAPS §4 목데이터 정리.)
 */
export function eventToFeedPost(dto: EventListItem): FeedPost {
  return {
    id: dto.id,
    date: dto.eventDateText,
    title: dto.title,
    place: dto.place,
    host: dto.host,
    company: dto.company,
    logoUrl: dto.logoUrl,
    cardLabel: dto.cardLabel,
    text: dto.content,
    image: dto.thumbnailImageUrl,
  };
}
