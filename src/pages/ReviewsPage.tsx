import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { showToast } from "../components/ui/toast";
import { useAuth } from "../features/auth/AuthContext";
import { loadReviews } from "../data/reviews";
import "./ContentPages.css";
import "./SupportPage.css";
import "./ReviewsPage.css";

export function ReviewsPage() {
  const { user } = useAuth();
  const [reviews] = useState(loadReviews);
  const [query, setQuery] = useState("");
  const [searchBy, setSearchBy] = useState("전체");

  const filteredReviews = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return reviews;
    return reviews.filter((review) => {
      if (searchBy === "제목") return review.title.toLowerCase().includes(keyword);
      if (searchBy === "내용") return review.content.toLowerCase().includes(keyword);
      if (searchBy === "작성자") return review.author.toLowerCase().includes(keyword);
      return `${review.title} ${review.content} ${review.author}`.toLowerCase().includes(keyword);
    });
  }, [query, reviews, searchBy]);

  return (
    <div className="support reviews-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">후기</p>
        <h1 className="support__title subpage-hero__title">후기</h1>
      </header>

      <section className="support__section page-container reviews-board">
        <div className="support-rule" aria-hidden="true"><i /></div>
        <div className="reviews-board__heading"><h2 className="support__heading">후기</h2><p>총 {filteredReviews.length}개의 후기가 있습니다.</p></div>

        <form className="notice-search" onSubmit={(event) => event.preventDefault()}>
          <select value={searchBy} onChange={(event) => setSearchBy(event.target.value)} aria-label="검색 조건"><option>전체</option><option>제목</option><option>내용</option><option>작성자</option></select>
          <label><span className="visually-hidden">검색어 입력</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="검색어를 입력하세요" /><button type="submit" aria-label="검색"><SearchGlyph /></button></label>
        </form>

        <div className="notice-table review-table" aria-label="후기 목록">
          <div className="notice-table__head"><span>번호</span><span>제목</span><span>작성자</span><span>작성일</span></div>
          {filteredReviews.length === 0 ? <p className="notice-table__empty">검색 결과가 없습니다.</p> : filteredReviews.map((review, index) => (
            <article className="review-row" key={review.id}>
              <div className="review-row__summary">
                <span>{reviews.indexOf(review) > -1 ? reviews.length - reviews.indexOf(review) : filteredReviews.length - index}</span>
                <Link className="notice-table__title" to={`/reviews/${encodeURIComponent(review.id)}`} state={{ review }}>{review.title}</Link>
                <span className="review-row__author">{review.author}</span>
                <time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time>
              </div>
            </article>
          ))}
        </div>
        <div className="reviews-board__footer"><nav className="support-pagination" aria-label="후기 페이지"><button aria-label="이전 페이지" disabled>‹</button><b>1</b><button aria-label="다음 페이지" disabled>›</button></nav><div className="reviews-board__write">{user ? <Button to="/reviews/new">후기 작성</Button> : <Button onClick={() => showToast("로그인 후 이용할 수 있습니다.")}>후기 작성</Button>}</div></div>
        {!user && <p className="reviews-board__login">후기를 작성하려면 <Link to={`/login?returnTo=${encodeURIComponent("/reviews/new")}`}>로그인</Link>해 주세요.</p>}
      </section>
    </div>
  );
}

function SearchGlyph() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>;
}
