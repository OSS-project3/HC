import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { showToast } from "../../components/ui/toast";
import { useAuth } from "../../features/auth/AuthContext";
import { getReviewImageUrl, loadReviews } from "../../data/reviews";
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

  const filteredReviews = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return reviews.filter((review) => {
      if (photoFilter === "photos" && !review.imageUrl) return false;
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
            <label><span>보기</span><select value={photoFilter} onChange={(event) => setPhotoFilter(event.target.value as "all" | "photos")}><option value="all">전체 후기</option><option value="photos">사진 모아보기</option></select></label>
            <label><span>카드 종류</span><select value={cardFilter} onChange={(event) => setCardFilter(event.target.value as "all" | CardType)}><option value="all">전체 카드</option>{(Object.entries(cardTypeLabels) as [CardType, string][]).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          </div>
          <form className="notice-search" onSubmit={(event) => event.preventDefault()}>
            <select value={searchBy} onChange={(event) => setSearchBy(event.target.value)} aria-label="검색 조건"><option>전체</option><option>제목</option><option>내용</option><option>작성자</option></select>
            <label><span className="visually-hidden">검색어 입력</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="검색어를 입력하세요" /><button type="submit" aria-label="검색"><SearchGlyph /></button></label>
          </form>
        </div>

        <div className="review-grid" aria-label="후기 목록">
          {visibleReviews.length === 0 ? <p className="review-grid__empty">검색 결과가 없습니다.</p> : visibleReviews.map((review) => {
            const imageUrl = getReviewImageUrl(review);
            return (
              <Link className={`review-card${imageUrl ? "" : " review-card--text-only"}`} key={review.id} to={`/reviews/${encodeURIComponent(review.id)}`} state={{ review: { ...review, imageUrl } }}>
                {imageUrl && <div className="review-card__visual"><img src={imageUrl} alt={`${review.title} 후기 이미지`} /></div>}
                <div className="review-card__body"><div className="review-card__tags"><span>{review.applicantType === "organization" ? "단체" : "개인"}</span><span>{cardTypeLabels[review.cardType]}</span></div><h3>{review.title}</h3><p>{review.content}</p><footer><strong>{review.author}</strong><time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time></footer></div>
              </Link>
            );
          })}
        </div>
        <div className="reviews-board__footer"><nav className="support-pagination" aria-label="후기 페이지"><button aria-label="이전 페이지" disabled={page === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>‹</button>{Array.from({ length: totalPages }, (_, index) => index + 1).map((number) => <button className={number === page ? "is-current" : ""} aria-current={number === page ? "page" : undefined} key={number} onClick={() => setPage(number)}>{number}</button>)}<button aria-label="다음 페이지" disabled={page === totalPages} onClick={() => setPage((value) => Math.min(totalPages, value + 1))}>›</button></nav><div className="reviews-board__write">{user ? <Button to="/reviews/new">후기 작성</Button> : <Button onClick={() => showToast("로그인 후 이용할 수 있습니다.")}>후기 작성</Button>}</div></div>
        {!user && <p className="reviews-board__login">후기를 작성하려면 <Link to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>로그인</Link>해 주세요.</p>}
      </section>

      <section className="reviews-apply-banner page-container"><div><p>나만의 한국 이름과 카드를 만들어보세요</p><h2>당신만의 특별한 한국 이야기를 카드에 담아보세요.</h2></div><div className="reviews-apply-banner__actions">{(Object.entries(cardTypeLabels) as [CardType, string][]).map(([type, label]) => <Link key={type} to={`/apply/${type}`}>{label} 신청</Link>)}</div></section>
    </div>
  );
}

function SearchGlyph() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>;
}
