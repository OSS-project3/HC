import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { showToast } from "../../components/ui/toast";
import { toReviewPost, type ReviewPost } from "../../data/reviews";
import { useAuth } from "../../features/auth/AuthContext";
import { cardTypeIds, cardTypeLabels, type CardType } from "../../data/cards";
import { api } from "../../services/api";
import "./ReviewEditorPage.css";

export function ReviewEditorPage() {
  const { reviewId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const editing = Boolean(reviewId);

  const [review, setReview] = useState<ReviewPost | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "ready" | "denied" | "error">(editing ? "loading" : "ready");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | undefined>();
  const [removeImage, setRemoveImage] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!editing) return;
    const id = Number(reviewId);
    if (!Number.isFinite(id)) { setLoadState("error"); return; }
    let cancelled = false;
    api.getReview(id)
      .then((data) => {
        if (cancelled) return;
        if (!data.canEdit) { setLoadState("denied"); return; }
        const mapped = toReviewPost(data);
        setReview(mapped);
        setPreviewUrl(mapped.imageUrl);
        setLoadState("ready");
      })
      .catch(() => { if (!cancelled) setLoadState("error"); });
    return () => { cancelled = true; };
  }, [editing, reviewId]);

  if (!user) return (
    <main className="review-write-page">
      <header className="subpage-hero page-container review-write-page__hero"><p className="eyebrow">후기</p><h1 className="subpage-hero__title">후기 작성</h1></header>
      <section className="review-write-page__login page-container"><h2>로그인이 필요한 서비스입니다.</h2><p>로그인한 회원만 후기를 작성할 수 있습니다.</p><Button to={`/login?returnTo=${encodeURIComponent(location.pathname)}`}>로그인하기</Button><Button variant="ghost" to="/reviews">목록으로</Button></section>
    </main>
  );
  if (editing && loadState === "loading") return <main className="review-write-page"><section className="review-write-page__login page-container"><h2>후기를 불러오는 중입니다…</h2></section></main>;
  if (editing && (loadState === "denied" || loadState === "error" || !review)) return <main className="review-write-page"><section className="review-write-page__login page-container"><h2>{loadState === "denied" ? "수정 권한이 없는 후기입니다." : "후기를 불러오지 못했습니다."}</h2><Button variant="ghost" to="/reviews">목록으로</Button></section></main>;

  const onPickFile = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) { showToast("사진은 2MB 이하로 첨부해 주세요."); return; }
    setImageFile(file);
    setPreviewUrl(URL.createObjectURL(file));
    setRemoveImage(false);
  };

  const clearImage = () => { setImageFile(null); setPreviewUrl(undefined); setRemoveImage(true); };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;
    const data = new FormData(event.currentTarget);
    const applicantType = String(data.get("applicantType")) as "personal" | "organization";
    const cardType = String(data.get("cardType")) as CardType;
    const body = {
      title: String(data.get("title")).trim(),
      content: String(data.get("content")).trim(),
      authorName: String(data.get("author")).trim(),
      applicationType: applicantType === "organization" ? ("GROUP" as const) : ("INDIVIDUAL" as const),
      cardTypeId: cardTypeIds[cardType],
    };
    setSubmitting(true);
    try {
      if (editing && review) {
        await api.updateReview(Number(review.id), { ...body, removeImage }, imageFile ?? undefined);
        showToast("후기가 수정되었습니다.");
        navigate(`/reviews/${review.id}`);
      } else {
        const created = await api.createReview(body, imageFile ?? undefined);
        showToast("후기가 등록되었습니다.");
        navigate(`/reviews/${created.id}`);
      }
    } catch (error) {
      showToast(error instanceof Error ? error.message : "후기 저장에 실패했습니다.");
      setSubmitting(false);
    }
  };

  return (
    <main className="review-write-page">
      <header className="subpage-hero page-container review-write-page__hero"><p className="eyebrow">후기</p><h1 className="subpage-hero__title">{editing ? "후기 수정" : "후기 작성"}</h1></header>
      <section className="review-write-page__wrap page-container">
        <form className="review-write-form" onSubmit={submit}>
          <label className="field"><span className="field__label">제목 <i className="req">*</i></span><input className="field__input" name="title" required maxLength={100} defaultValue={review?.title ?? ""} placeholder="후기 제목을 입력해 주세요" /></label>
          <fieldset className="review-write-form__choices"><legend>신청자 유형 <i className="req">*</i></legend><label className="check"><input type="radio" name="applicantType" value="personal" defaultChecked={review?.applicantType !== "organization"} /><span>개인</span></label><label className="check"><input type="radio" name="applicantType" value="organization" defaultChecked={review?.applicantType === "organization"} /><span>단체</span></label></fieldset>
          <fieldset className="review-write-form__choices"><legend>신청한 카드 <i className="req">*</i></legend>{(Object.entries(cardTypeLabels) as [CardType, string][]).map(([value, label]) => <label key={value} className="check"><input type="radio" name="cardType" value={value} defaultChecked={(review?.cardType ?? "honorary-korean") === value} /><span>{label}</span></label>)}</fieldset>
          <label className="field"><span className="field__label">작성자 이름 <i className="req">*</i></span><input className="field__input" name="author" required maxLength={50} defaultValue={review?.author ?? ""} placeholder="후기에 표시할 이름을 입력해 주세요" /></label>
          <label className="field"><span className="field__label">사진 첨부 <small>(1장, 2MB 이하)</small></span><input className="field__input review-write-form__file" type="file" accept="image/png,image/jpeg,image/webp" onChange={onPickFile} /></label>
          {previewUrl && <div className="review-write-form__preview"><figure><img src={previewUrl} alt="첨부 사진 미리보기" /><button type="button" onClick={clearImage}>사진 삭제</button></figure></div>}
          <label className="field"><span className="field__label">내용 <i className="req">*</i></span><textarea className="field__textarea" name="content" required maxLength={3000} defaultValue={review?.content ?? ""} placeholder="경험하신 내용을 자세히 들려주세요" /></label>
          <div className="review-write-form__actions"><Button type="button" variant="ghost" onClick={() => navigate(-1)}>취소</Button><Button type="submit" disabled={submitting}>{editing ? "수정 완료" : "등록하기"}</Button></div>
        </form>
      </section>
    </main>
  );
}
