import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { SelectField } from "../../components/ui/SelectField";
import { useAuth } from "../../features/auth/AuthContext";
import { useLanguage } from "../../features/i18n/LanguageContext";
import { getReviewFallbackImageUrl, getReviewImageUrl, getReviewImageUrls, toReviewPost, type ReviewPost } from "../../data/reviews";
import { cardTypeIds, cardTypeLabels, type CardType } from "../../data/cards";
import { api, type ReviewSearchType } from "../../services/api";
import "../../styles/ContentPages.css";
import "../SupportPage/SupportPage.css";
import "./ReviewsPage.css";

const PAGE_SIZE = 9;
const searchTypeByLabel: Record<string, ReviewSearchType> = { 전체: "ALL", 제목: "TITLE", 내용: "CONTENT", 작성자: "AUTHOR" };

export function ReviewsPage() {
  const { user } = useAuth();
  const { t, language } = useLanguage();
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [searchBy, setSearchBy] = useState("전체");
  const [photoFilter, setPhotoFilter] = useState<"all" | "photos">("all");
  const [cardFilter, setCardFilter] = useState<"all" | CardType>("all");
  const [page, setPage] = useState(1);
  const [reviews, setReviews] = useState<ReviewPost[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [activeReview, setActiveReview] = useState<(ReviewPost & { resolvedImageUrls: string[] }) | null>(null);

  useEffect(() => { setPage(1); }, [cardFilter, photoFilter, submittedQuery, searchBy]);

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");
    api.listReviews({
      cardTypeId: cardFilter === "all" ? undefined : cardTypeIds[cardFilter],
      hasPhoto: photoFilter === "photos" ? true : undefined,
      searchType: submittedQuery ? searchTypeByLabel[searchBy] : undefined,
      keyword: submittedQuery || undefined,
      page: page - 1,
      size: PAGE_SIZE,
    })
      .then((data) => {
        if (cancelled) return;
        setReviews(data.content.map(toReviewPost));
        setTotalPages(Math.max(1, data.totalPages));
        setTotalElements(data.totalElements);
        setStatus("ready");
      })
      .catch(() => { if (!cancelled) setStatus("error"); });
    return () => { cancelled = true; };
  }, [cardFilter, photoFilter, submittedQuery, searchBy, page, language]); // 언어 전환 시 번역된 내용으로 재조회(페이지·필터는 유지)

  const visibleReviews = reviews;

  return (
    <div className="support reviews-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">{t("후기")}</p>
        <h1 className="support__title subpage-hero__title">{t("후기")}</h1>
      </header>

      <section className="support__section page-container reviews-board">
        <div className="reviews-board__heading"><h2 className="support__heading">{t("후기")}</h2><p>{language === "en" ? `${totalElements} reviews in total.` : `총 ${totalElements}개의 후기가 있습니다.`}</p></div>

        <div className="reviews-board__tools">
          <div className="reviews-board__filters">
            <label><span>{t("보기")}</span><SelectField ariaLabel={t("보기 필터")} value={photoFilter} onChange={(value) => setPhotoFilter(value as "all" | "photos")} options={[{ value: "all", label: t("전체 후기") }, { value: "photos", label: t("사진 모아보기") }]} /></label>
            <label><span>{t("카드 종류")}</span><SelectField ariaLabel={t("카드 종류 필터")} value={cardFilter} onChange={(value) => setCardFilter(value as "all" | CardType)} options={[{ value: "all", label: t("전체 카드") }, ...(Object.entries(cardTypeLabels) as [CardType, string][]).map(([value, label]) => ({ value, label: t(label) }))]} /></label>
          </div>
          <form className="notice-search" onSubmit={(event) => { event.preventDefault(); setSubmittedQuery(query.trim()); }}>
            <SelectField ariaLabel={t("검색 조건")} value={searchBy} onChange={setSearchBy} options={[{ value: "전체", label: t("전체") }, { value: "제목", label: t("제목") }, { value: "내용", label: t("내용") }, { value: "작성자", label: t("작성자") }]} />
            <label><span className="visually-hidden">{t("검색어 입력")}</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("검색어를 입력하세요")} /><button type="submit" aria-label={t("검색")}><SearchGlyph /></button></label>
          </form>
        </div>

        <div className="review-grid" aria-label={t("후기 목록")}>
          {status === "loading" ? <p className="review-grid__empty">{t("후기를 불러오는 중입니다…")}</p> : status === "error" ? <p className="review-grid__empty">{t("후기를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.")}</p> : visibleReviews.length === 0 ? <p className="review-grid__empty">{t("검색 결과가 없습니다.")}</p> : visibleReviews.map((review) => {
            const imageUrl = getReviewImageUrl(review);
            const imageUrls = getReviewImageUrls(review);
            return (
              <button
                type="button"
                className={`review-card${imageUrl ? "" : " review-card--text-only"}`}
                key={review.id}
                onClick={() => setActiveReview({ ...review, resolvedImageUrls: imageUrls })}
              >
                {imageUrl && <div className="review-card__visual"><img src={imageUrl} alt={language === "en" ? `Review photo for ${review.title}` : `${review.title} 후기 이미지`} onError={(event) => {
                  const img = event.currentTarget;
                  const fallback = getReviewFallbackImageUrl(review.id);
                  if (fallback && !img.dataset.fallbackApplied) {
                    img.dataset.fallbackApplied = "1";
                    img.src = fallback;
                  } else {
                    img.closest(".review-card__visual")?.remove();
                  }
                }} /></div>}
                <div className="review-card__body"><div className="review-card__tags"><span>{review.applicantType === "organization" ? t("단체") : t("개인")}</span><span>{t(cardTypeLabels[review.cardType])}</span></div><h3>{review.title}</h3><p>{review.content}</p><footer><strong>{review.author}</strong><time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time></footer></div>
              </button>
            );
          })}
        </div>
        <div className="reviews-board__footer"><nav className="support-pagination" aria-label={t("후기 페이지")}><button aria-label={t("이전 페이지")} disabled={page === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>‹</button>{Array.from({ length: totalPages }, (_, index) => index + 1).map((number) => <button className={number === page ? "is-current" : ""} aria-current={number === page ? "page" : undefined} key={number} onClick={() => setPage(number)}>{number}</button>)}<button aria-label={t("다음 페이지")} disabled={page === totalPages} onClick={() => setPage((value) => Math.min(totalPages, value + 1))}>›</button></nav><div className="reviews-board__write">{user ? <Button to="/reviews/new">{t("후기 작성")}</Button> : <Button to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>{t("후기 작성")}</Button>}</div></div>
        {!user && <p className="reviews-board__login">{language === "en" ? <>Please <Link to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>sign in</Link> to write a review.</> : <>후기를 작성하려면 <Link to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>로그인</Link>해 주세요.</>}</p>}
      </section>

      <section className="reviews-apply-banner page-container"><div><p>{t("나만의 한국 이름과 카드를 만들어보세요")}</p><h2>{t("당신만의 특별한 한국 이야기를 카드에 담아보세요.")}</h2></div><div className="reviews-apply-banner__actions">{(Object.entries(cardTypeLabels) as [CardType, string][]).map(([type, label]) => <Link key={type} to={`/apply/${type}`}>{language === "en" ? `Apply for ${t(label)}` : `${label} 신청`}</Link>)}</div></section>

      <Modal open={activeReview !== null} onClose={() => setActiveReview(null)} title={activeReview?.title ?? t("후기")} className="review-modal">
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
  const { t, language } = useLanguage();
  const [imageIndex, setImageIndex] = useState(0);
  const images = review.resolvedImageUrls;
  const currentImage = images[imageIndex];
  const moveImage = (delta: number) => setImageIndex((index) => (index + delta + images.length) % images.length);
  return (
    <article className="review-modal__content">
      <header className="review-modal__head">
        <div className="review-card__tags">
          <span>{review.applicantType === "organization" ? t("단체") : t("개인")}</span>
          <span>{t(cardTypeLabels[review.cardType])}</span>
        </div>
        <div className="review-modal__meta">
          <strong>{review.author}</strong>
          <time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time>
        </div>
      </header>
      {currentImage && (
        <div className="review-modal__gallery">
          <div className="review-modal__image-wrap">
            {images.length > 1 && <button type="button" className="review-modal__arrow review-modal__arrow--prev" aria-label={t("이전 사진")} onClick={() => moveImage(-1)}>‹</button>}
            <img
              className="review-modal__image"
              src={currentImage}
              alt={language === "en" ? `Review photo ${imageIndex + 1} for ${review.title}` : `${review.title} 후기 이미지 ${imageIndex + 1}`}
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
            {images.length > 1 && <button type="button" className="review-modal__arrow review-modal__arrow--next" aria-label={t("다음 사진")} onClick={() => moveImage(1)}>›</button>}
          </div>
          {images.length > 1 && (
            <div className="review-modal__thumbs" aria-label={t("후기 이미지 미리보기")}>
              {images.map((src, index) => (
                <button type="button" key={`${src}-${index}`} className={index === imageIndex ? "is-current" : ""} aria-label={language === "en" ? `View photo ${index + 1}` : `${index + 1}번째 사진 보기`} aria-current={index === imageIndex ? "true" : undefined} onClick={() => setImageIndex(index)}>
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
      {isAdmin && <div className="review-modal__actions"><Button variant="outline" to={`/reviews/${review.id}/edit`}>{t("수정")}</Button></div>}
    </article>
  );
}
