import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { showToast } from "../components/ui/toast";
import { findReview, loadReviews, saveReviews, type ReviewPost } from "../data/reviews";
import { useAuth } from "../features/auth/AuthContext";
import "./NoticeDetailPage.css";
import "./ReviewDetailPage.css";

export function ReviewDetailPage() {
  const { reviewId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { isAdmin } = useAuth();
  const reviews = loadReviews();
  const index = reviews.findIndex((item) => item.id === reviewId);
  const stateReview = (location.state as { review?: ReviewPost } | null)?.review;
  const review = reviews[index] ?? (stateReview?.id === reviewId ? stateReview : findReview(reviewId));

  if (!review) return <section className="notice-detail page-container review-not-found"><h1>후기를 찾을 수 없습니다.</h1><Link className="notice-detail__list" to="/reviews">목록</Link></section>;

  const nextReview = index >= 0 ? reviews[index + 1] : undefined;
  const remove = () => {
    if (!window.confirm(`‘${review.title}’ 후기를 삭제하시겠습니까?`)) return;
    saveReviews(reviews.filter((item) => item.id !== review.id));
    showToast("후기가 삭제되었습니다.");
    navigate("/reviews", { replace: true });
  };

  return (
    <article className="notice-detail review-detail page-container">
      <header className="notice-detail__hero subpage-hero"><p className="eyebrow">후기</p><h1 className="subpage-hero__title">후기</h1></header>
      <div className="notice-detail__rule" aria-hidden="true"><i /></div>
      <header className="notice-detail__head"><h2>{review.title}</h2><div className="review-detail__meta"><span>{review.author}</span><time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time></div></header>
      <div className="notice-detail__body review-detail__body">{review.content.split("\n").map((line, lineIndex) => <p key={`${lineIndex}-${line}`}>{line || "\u00a0"}</p>)}</div>
      <div className="notice-detail__actions review-detail__actions">
        <Link className="notice-detail__list" to="/reviews">목록</Link>
        {isAdmin && <div><Button variant="outline" to={`/reviews/${review.id}/edit`}>수정</Button><Button variant="ghost" onClick={remove}>삭제</Button></div>}
      </div>
      {nextReview && <Link className="notice-detail__next" to={`/reviews/${encodeURIComponent(nextReview.id)}`} state={{ review: nextReview }}><span>⌄　다음글</span><strong>{nextReview.title}</strong></Link>}
    </article>
  );
}
