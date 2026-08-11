import { useState } from "react";
import { Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { showToast } from "../components/ui/toast";
import { findReview, loadReviews, saveReviews } from "../data/reviews";
import { useAuth } from "../features/auth/AuthContext";
import { cardTypeLabels, type CardType } from "../data/cards";
import "./ReviewEditorPage.css";

export function ReviewEditorPage() {
  const { reviewId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAdmin } = useAuth();
  const editing = Boolean(reviewId);
  const review = editing ? findReview(reviewId) : undefined;
  const [imageUrl, setImageUrl] = useState(review?.imageUrl ?? "");

  if (!user) return (
    <main className="review-write-page">
      <header className="subpage-hero page-container review-write-page__hero"><p className="eyebrow">후기</p><h1 className="subpage-hero__title">후기 작성</h1></header>
      <section className="review-write-page__login page-container"><h2>로그인이 필요한 서비스입니다.</h2><p>로그인한 회원만 후기를 작성할 수 있습니다.</p><Button to={`/login?returnTo=${encodeURIComponent(location.pathname)}`}>로그인하기</Button><Button variant="ghost" to="/reviews">목록으로</Button></section>
    </main>
  );
  if (editing && !review) return <Navigate to="/reviews" replace />;
  if (editing && review && !isAdmin && review.authorEmail !== user.email) return <Navigate to="/mypage" replace />;

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const title = String(data.get("title")).trim();
    const content = String(data.get("content")).trim();
    const author = String(data.get("author")).trim();
    const applicantType = String(data.get("applicantType")) as "personal" | "organization";
    const cardType = String(data.get("cardType")) as CardType;
    const reviews = loadReviews();
    if (review) {
      saveReviews(reviews.map((item) => item.id === review.id ? { ...item, title, content, author, applicantType, cardType, imageUrl: imageUrl || undefined } : item));
      showToast("후기가 수정되었습니다.");
      navigate(`/reviews/${review.id}`);
    } else {
      const id = crypto.randomUUID();
      saveReviews([{ id, title, content, author, authorEmail: user.email, createdAt: new Date().toISOString().slice(0, 10), applicantType, cardType, imageUrl: imageUrl || undefined }, ...reviews]);
      showToast("후기가 등록되었습니다.");
      navigate(`/reviews/${id}`);
    }
  };

  return (
    <main className="review-write-page">
      <header className="subpage-hero page-container review-write-page__hero"><p className="eyebrow">후기</p><h1 className="subpage-hero__title">{editing ? "후기 수정" : "후기 작성"}</h1></header>
      <section className="review-write-page__wrap page-container">
        <form className="review-write-form" onSubmit={submit}>
          <label className="field"><span className="field__label">제목 <i className="req">*</i></span><input className="field__input" name="title" required maxLength={100} defaultValue={review?.title ?? ""} placeholder="후기 제목을 입력해 주세요" /></label>
          <fieldset className="review-write-form__choices"><legend>신청자 유형 <i className="req">*</i></legend><label><input type="radio" name="applicantType" value="personal" defaultChecked={review?.applicantType !== "organization"} /> 개인</label><label><input type="radio" name="applicantType" value="organization" defaultChecked={review?.applicantType === "organization"} /> 단체</label></fieldset>
          <fieldset className="review-write-form__choices"><legend>신청한 카드 <i className="req">*</i></legend>{(Object.entries(cardTypeLabels) as [CardType, string][]).map(([value, label]) => <label key={value}><input type="radio" name="cardType" value={value} defaultChecked={(review?.cardType ?? "honorary-korean") === value} /> {label}</label>)}</fieldset>
          <label className="field"><span className="field__label">작성자 이름 <i className="req">*</i></span><input className="field__input" name="author" required maxLength={40} defaultValue={review?.author ?? ""} placeholder="후기에 표시할 이름을 입력해 주세요" /></label>
          <label className="field"><span className="field__label">사진 첨부</span><input className="field__input review-write-form__file" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => { const file = event.target.files?.[0]; if (!file) return; if (file.size > 2 * 1024 * 1024) { showToast("사진은 2MB 이하로 첨부해 주세요."); event.target.value = ""; return; } const reader = new FileReader(); reader.onload = () => setImageUrl(String(reader.result)); reader.readAsDataURL(file); }} /></label>
          {imageUrl && <div className="review-write-form__preview"><img src={imageUrl} alt="첨부 사진 미리보기" /><button type="button" onClick={() => setImageUrl("")}>사진 삭제</button></div>}
          <label className="field"><span className="field__label">내용 <i className="req">*</i></span><textarea className="field__textarea" name="content" required maxLength={3000} defaultValue={review?.content ?? ""} placeholder="경험하신 내용을 자세히 들려주세요" /></label>
          <div className="review-write-form__actions"><Button type="button" variant="ghost" onClick={() => navigate(-1)}>취소</Button><Button type="submit">{editing ? "수정 완료" : "등록하기"}</Button></div>
        </form>
      </section>
    </main>
  );
}
