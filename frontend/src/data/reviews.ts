import { cardTypeById, type CardType } from "./cards";
import type { ReviewDetail, ReviewListItem } from "../services/api";

/**
 * Frontend review shape. Mapped from the backend Review DTOs — the review data
 * itself now lives on the server (`/api/reviews`), so this module only holds the
 * view model and the DTO→view mapping. A review can carry multiple images; the
 * detail DTO returns them as `images` ([{id, imageUrl}]), while the list DTO only
 * carries the single `imageUrl` thumbnail.
 */
export interface ReviewImageRef {
  id: number;
  imageUrl: string;
}

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
  /** Full images with ids — only present from the detail DTO (used by the editor). */
  images?: ReviewImageRef[];
  canEdit?: boolean;
  canDelete?: boolean;
}

/** Map a backend list/detail DTO into the frontend `ReviewPost` view model. */
export function toReviewPost(dto: ReviewListItem | ReviewDetail): ReviewPost {
  const detail = dto as ReviewDetail;
  const images = detail.images ?? [];
  const imageUrls = images.length ? images.map((image) => image.imageUrl) : dto.imageUrl ? [dto.imageUrl] : [];
  return {
    id: String(dto.id),
    title: dto.title,
    content: dto.content,
    author: dto.authorName,
    createdAt: (dto.createdAt ?? "").slice(0, 10),
    applicantType: dto.applicationType === "GROUP" ? "organization" : "personal",
    cardType: cardTypeById[dto.cardType.id] ?? "honorary-korean",
    imageUrl: dto.imageUrl ?? imageUrls[0],
    imageUrls,
    images: detail.images ? images : undefined,
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
