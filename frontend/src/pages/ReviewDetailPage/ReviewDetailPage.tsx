import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { showToast } from "../../components/ui/toast";
import { toReviewPost, type ReviewPost } from "../../data/reviews";
import { cardTypeLabels } from "../../data/cards";
import { api } from "../../services/api";
import "../NoticeDetailPage/NoticeDetailPage.css";
import "./ReviewDetailPage.css";

export function ReviewDetailPage() {
  const { reviewId } = useParams();
  const navigate = useNavigate();
  const [review, setReview] = useState<ReviewPost | null>(null);
  const [next, setNext] = useState<{ id: number; title: string } | undefined>();
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");

  useEffect(() => {
    const id = Number(reviewId);
    if (!Number.isFinite(id)) { setStatus("error"); return; }
    let cancelled = false;
    setStatus("loading");
    api.getReview(id)
      .then((data) => {
        if (cancelled) return;
        setReview(toReviewPost(data));
        setNext(data.next);
        setStatus("ready");
      })
      .catch(() => { if (!cancelled) setStatus("error"); });
    return () => { cancelled = true; };
  }, [reviewId]);

  if (status === "loading") return <section className="notice-detail page-container review-not-found"><h1>후기를 불러오는 중입니다…</h1></section>;
  if (status === "error" || !review) return <section className="notice-detail page-container review-not-found"><h1>후기를 찾을 수 없습니다.</h1><Link className="notice-detail__list" to="/reviews">목록</Link></section>;

  const remove = async () => {
    if (!window.confirm(`‘${review.title}’ 후기를 삭제하시겠습니까?`)) return;
    try {
      await api.deleteReview(Number(review.id));
      showToast("후기가 삭제되었습니다.");
      navigate("/reviews", { replace: true });
    } catch (error) {
      showToast(error instanceof Error ? error.message : "후기 삭제에 실패했습니다.");
    }
  };

  return (
    <article className="notice-detail review-detail page-container">
      <header className="notice-detail__hero subpage-hero"><p className="eyebrow">후기</p><h1 className="subpage-hero__title">후기</h1></header>
      <div className="notice-detail__rule" aria-hidden="true"><i /></div>
      <header className="notice-detail__head"><h2>{review.title}</h2><div className="review-detail__meta"><span>{review.author}</span><span>{review.applicantType === "organization" ? "단체" : "개인"}</span><span>{cardTypeLabels[review.cardType]}</span><time dateTime={review.createdAt}>{review.createdAt.replace(/-/g, ".")}</time></div></header>
      {review.imageUrl && (
        <img
          className="review-detail__image"
          src={review.imageUrl}
          alt={`${review.title} 첨부 사진`}
          onError={(event) => { event.currentTarget.style.display = "none"; }}
        />
      )}
      <div className="notice-detail__body review-detail__body">{review.content.split("\n").map((line, lineIndex) => <p key={`${lineIndex}-${line}`}>{line || " "}</p>)}</div>
      <div className="notice-detail__actions review-detail__actions">
        <Link className="notice-detail__list" to="/reviews">목록</Link>
        {(review.canEdit || review.canDelete) && <div>{review.canEdit && <Button variant="outline" to={`/reviews/${review.id}/edit`}>수정</Button>}{review.canDelete && <Button variant="ghost" onClick={remove}>삭제</Button>}</div>}
      </div>
      {next && <Link className="notice-detail__next" to={`/reviews/${next.id}`}><span>⌄　다음글</span><strong>{next.title}</strong></Link>}
    </article>
  );
}
