import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { SelectField } from "../../components/ui/SelectField";
import { useAuth } from "../../features/auth/AuthContext";
import { getReviewFallbackImageUrl, getReviewImageUrl, getReviewImageUrls, loadReviews, type ReviewPost } from "../../data/reviews";
import { cardTypeLabels, type CardType } from "../../data/cards";
import "../../styles/ContentPages.css";
import "../SupportPage/SupportPage.css";
import "./ReviewsPage.css";

const PAGE_SIZE = 9;

export function ReviewsPage() {
  const { user } = useAuth();
  const [reviews] = useState(loadReviews);
  const [query, setQuery] = useState("");
  const [searchBy, setSearchBy] = useState("전체");
  const [photoFilter, setPhotoFilter] = useState<"all" | "photos">("all");
  const [cardFilter, setCardFilter] = useState<"all" | CardType>("all");
  const [page, setPage] = useState(1);
  const [activeReview, setActiveReview] = useState<(ReviewPost & { resolvedImageUrls: string[] }) | null>(null);

  const filteredReviews = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return reviews.filter((review) => {
      if (photoFilter === "photos" && getReviewImageUrls(review).length === 0) return false;
      if (cardFilter !== "all" && review.cardType !== cardFilter) return false;
      if (!keyword) return true;
      if (searchBy === "제목") return review.title.toLowerCase().includes(keyword);
      if (searchBy === "내용") return review.content.toLowerCase().includes(keyword);
      if (searchBy === "작성자") return review.author.toLowerCase().includes(keyword);
      return `${review.title} ${review.content} ${review.author}`.toLowerCase().includes(keyword);
    });
  }, [cardFilter, photoFilter, query, reviews, searchBy]);

  const totalPages = Math.max(1, Math.ceil(filteredReviews.length / PAGE_SIZE));
  const visibleReviews = filteredReviews.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  useEffect(() => setPage(1), [cardFilter, photoFilter, query, searchBy]);
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [page, totalPages]);

  return (
    <div className="support reviews-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">후기</p>
        <h1 className="support__title subpage-hero__title">후기</h1>
      </header>

      <section className="support__section page-container reviews-board">
        <div className="reviews-board__heading"><h2 className="support__heading">후기</h2><p>총 {filteredReviews.length}개의 후기가 있습니다.</p></div>

        <div className="reviews-board__tools">
          <div className="reviews-board__filters">
            <label><span>보기</span><SelectField ariaLabel="보기 필터" value={photoFilter} onChange={(value) => setPhotoFilter(value as "all" | "photos")} options={[{ value: "all", label: "전체 후기" }, { value: "photos", label: "사진 모아보기" }]} /></label>
            <label><span>카드 종류</span><SelectField ariaLabel="카드 종류 필터" value={cardFilter} onChange={(value) => setCardFilter(value as "all" | CardType)} options={[{ value: "all", label: "전체 카드" }, ...(Object.entries(cardTypeLabels) as [CardType, string][]).map(([value, label]) => ({ value, label }))]} /></label>
          </div>
          <form className="notice-search" onSubmit={(event) => event.preventDefault()}>
            <SelectField ariaLabel="검색 조건" value={searchBy} onChange={setSearchBy} options={[{ value: "전체", label: "전체" }, { value: "제목", label: "제목" }, { value: "내용", label: "내용" }, { value: "작성자", label: "작성자" }]} />
            <label><span className="visually-hidden">검색어 입력</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="검색어를 입력하세요" /><button type="submit" aria-label="검색"><SearchGlyph /></button></label>
          </form>
        </div>

        <div className="review-grid" aria-label="후기 목록">
          {visibleReviews.length === 0 ? <p className="review-grid__empty">검색 결과가 없습니다.</p> : visibleReviews.map((review) => {
            const imageUrl = getReviewImageUrl(review);
            const imageUrls = getReviewImageUrls(review);
            return (
              <button
                type="button"
                className={`review-card${imageUrl ? "" : " review-card--text-only"}`}
                key={review.id}
                onClick={() => setActiveReview({ ...review, resolvedImageUrls: imageUrls })}
              >
                {imageUrl && <div className="review-card__visual"><img src={imageUrl} alt={`${review.title} 후기 이미지`} onError={(event) => {
                  const img = event.currentTarget;
                  const fallback = getReviewFallbackImageUrl(review.id);
                  if (fallback && !img.dataset.fallbackApplied) {
                    img.dataset.fallbackApplied = "1";
                    img.src = fallback;
                  } else {
                    img.closest(".review-card__visual")?.remove();
                  }
                }} /></div>}
                <div className="review-card__body"><div className="review-card__tags"><span>{review.applicantType === "organization" ? "단체" : "개인"}</span><span>{cardTypeLabels[review.cardType]}</span></div><h3>{review.title}</h3><p>{review.content}</p><footer><strong>{review.author}</strong><time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time></footer></div>
              </button>
            );
          })}
        </div>
        <div className="reviews-board__footer"><nav className="support-pagination" aria-label="후기 페이지"><button aria-label="이전 페이지" disabled={page === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>‹</button>{Array.from({ length: totalPages }, (_, index) => index + 1).map((number) => <button className={number === page ? "is-current" : ""} aria-current={number === page ? "page" : undefined} key={number} onClick={() => setPage(number)}>{number}</button>)}<button aria-label="다음 페이지" disabled={page === totalPages} onClick={() => setPage((value) => Math.min(totalPages, value + 1))}>›</button></nav><div className="reviews-board__write">{user ? <Button to="/reviews/new">후기 작성</Button> : <Button to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>후기 작성</Button>}</div></div>
        {!user && <p className="reviews-board__login">후기를 작성하려면 <Link to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>로그인</Link>해 주세요.</p>}
      </section>

      <section className="reviews-apply-banner page-container"><div><p>나만의 한국 이름과 카드를 만들어보세요</p><h2>당신만의 특별한 한국 이야기를 카드에 담아보세요.</h2></div><div className="reviews-apply-banner__actions">{(Object.entries(cardTypeLabels) as [CardType, string][]).map(([type, label]) => <Link key={type} to={`/apply/${type}`}>{label} 신청</Link>)}</div></section>

      <Modal open={activeReview !== null} onClose={() => setActiveReview(null)} title={activeReview?.title ?? "후기"} className="review-modal">
        {activeReview && <ReviewModalContent review={activeReview} />}
      </Modal>
    </div>
  );
}

function SearchGlyph() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>;
}

function ReviewModalContent({ review }: { review: ReviewPost & { resolvedImageUrls: string[] } }) {
  const { isAdmin } = useAuth();
  const [imageIndex, setImageIndex] = useState(0);
  const images = review.resolvedImageUrls;
  const currentImage = images[imageIndex];
  const moveImage = (delta: number) => setImageIndex((index) => (index + delta + images.length) % images.length);
  return (
    <article className="review-modal__content">
      <header className="review-modal__head">
        <div className="review-card__tags">
          <span>{review.applicantType === "organization" ? "단체" : "개인"}</span>
          <span>{cardTypeLabels[review.cardType]}</span>
        </div>
        <div className="review-modal__meta">
          <strong>{review.author}</strong>
          <time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time>
        </div>
      </header>
      {currentImage && (
        <div className="review-modal__gallery">
          <div className="review-modal__image-wrap">
            {images.length > 1 && <button type="button" className="review-modal__arrow review-modal__arrow--prev" aria-label="이전 사진" onClick={() => moveImage(-1)}>‹</button>}
            <img
              className="review-modal__image"
              src={currentImage}
              alt={`${review.title} 후기 이미지 ${imageIndex + 1}`}
              onError={(event) => {
                const img = event.currentTarget;
                const fallback = getReviewFallbackImageUrl(review.id);
                if (fallback && !img.dataset.fallbackApplied) {
                  img.dataset.fallbackApplied = "1";
                  img.src = fallback;
                } else {
                  img.style.display = "none";
                }
              }}
            />
            {images.length > 1 && <button type="button" className="review-modal__arrow review-modal__arrow--next" aria-label="다음 사진" onClick={() => moveImage(1)}>›</button>}
          </div>
          {images.length > 1 && (
            <div className="review-modal__thumbs" aria-label="후기 이미지 미리보기">
              {images.map((src, index) => (
                <button type="button" key={`${src}-${index}`} className={index === imageIndex ? "is-current" : ""} aria-label={`${index + 1}번째 사진 보기`} aria-current={index === imageIndex ? "true" : undefined} onClick={() => setImageIndex(index)}>
                  <img src={src} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>
      )}
      <div className="review-modal__body">
        {review.content.split("\n").map((line, index) => <p key={`${index}-${line}`}>{line || "\u00a0"}</p>)}
      </div>
      {isAdmin && <div className="review-modal__actions"><Button variant="outline" to={`/reviews/${review.id}/edit`}>수정</Button></div>}
    </article>
  );
}
