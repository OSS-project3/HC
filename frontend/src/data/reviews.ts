import { cardTypeById, type CardType } from "./cards";
import type { ReviewDetail, ReviewListItem } from "../services/api";

/**
 * Frontend review shape. Mapped from the backend Review DTOs — the review data
 * itself now lives on the server (`/api/reviews`), so this module only holds the
 * view model and the DTO→view mapping. The backend stores a single image per
 * review; `imageUrls` is kept for the existing gallery UI but will contain at
 * most one URL.
 */
export interface ReviewPost {
  id: string;
  title: string;
  content: string;
  author: string;
  authorEmail?: string;
  createdAt: string;
  applicantType: "personal" | "organization";
  cardType: CardType;
  imageUrl?: string;
  imageUrls?: string[];
  canEdit?: boolean;
  canDelete?: boolean;
}

/** Map a backend list/detail DTO into the frontend `ReviewPost` view model. */
export function toReviewPost(dto: ReviewListItem | ReviewDetail): ReviewPost {
  const detail = dto as ReviewDetail;
  return {
    id: String(dto.id),
    title: dto.title,
    content: dto.content,
    author: dto.authorName,
    createdAt: (dto.createdAt ?? "").slice(0, 10),
    applicantType: dto.applicationType === "GROUP" ? "organization" : "personal",
    cardType: cardTypeById[dto.cardType.id] ?? "honorary-korean",
    imageUrl: dto.imageUrl,
    imageUrls: dto.imageUrl ? [dto.imageUrl] : [],
    canEdit: detail.canEdit,
    canDelete: detail.canDelete,
  };
}

export function getReviewImageUrls(review: Pick<ReviewPost, "imageUrl" | "imageUrls">): string[] {
  if (review.imageUrls?.length) return review.imageUrls;
  return review.imageUrl ? [review.imageUrl] : [];
}

export function getReviewImageUrl(review: Pick<ReviewPost, "imageUrl" | "imageUrls">): string | undefined {
  return getReviewImageUrls(review)[0];
}

/** Server images are presigned URLs with no local fallback. */
export function getReviewFallbackImageUrl(_id: string): string | undefined {
  return undefined;
}
