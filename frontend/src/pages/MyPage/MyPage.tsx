import { Link } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { toReviewPost } from "../../data/reviews";
import "./MyPage.css";
import { Fragment, useEffect, useState } from "react";
import { api, ApiError, type AdminApplicationDetail, type AdminApplicationListItem, type ApplicationStatus, type InquiryListItem } from "../../services/api";
import { Button } from "../../components/ui/Button";
import { showToast } from "../../components/ui/toast";
import { useLanguage } from "../../features/i18n/LanguageContext";

const APP_STATUS_LABELS: Record<ApplicationStatus, string> = {
  SUBMITTED: "접수", REVIEWING: "검토중", PHOTO_REJECTED: "사진반려", NAME_EDITING: "작명중",
  PRODUCTION_READY: "제작대기", PRODUCING: "제작중", COMPLETED: "발급완료", CANCELLED: "취소됨",
};
// 취소 가능 상태(백엔드 canCancelByUser와 동일).
const CANCELLABLE = new Set<ApplicationStatus>(["SUBMITTED", "REVIEWING", "PHOTO_REJECTED"]);
// 신청·후기 목록 페이지 크기(서버 Pageable, §1.20).
const PAGE_SIZE = 20;

export function MyPage() {
  const { t, language } = useLanguage();
  const { user, status, refreshProfile, logout } = useAuth();
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(() => ({ name: user?.name || "", phone: user?.phone || "", address: user?.address || "" }));
  // 비밀번호 변경(PATCH /api/users/me/password).
  const [pwOpen, setPwOpen] = useState(false);
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  // 내 후기/신청/문의는 모두 백엔드 my-* API로 조회한다(서버 세션 확인 후에만).
  // 신청·후기 목록은 서버 페이지네이션(§1.20)과 연결한다 — 첫 페이지 로드 후 "더보기"로 누적.
  const [myReviews, setMyReviews] = useState<{ id: string; title: string; createdAt: string }[]>([]);
  const [reviewsPaging, setReviewsPaging] = useState({ page: 0, totalPages: 1 });
  const [myApplications, setMyApplications] = useState<AdminApplicationListItem[]>([]);
  const [appsPaging, setAppsPaging] = useState({ page: 0, totalPages: 1 });
  const [myInquiries, setMyInquiries] = useState<InquiryListItem[]>([]);
  // 내 신청 상세(GET /api/my/applications/{id}) — 행을 펼치면 로드.
  const [openAppId, setOpenAppId] = useState<number | null>(null);
  const [appDetail, setAppDetail] = useState<AdminApplicationDetail | null>(null);
  const [appDetailLoading, setAppDetailLoading] = useState(false);

  useEffect(() => {
    if (status !== "authenticated") { setMyReviews([]); setMyApplications([]); setMyInquiries([]); return; }
    let cancelled = false;
    Promise.all([
      api.listMyReviews({ page: 0, size: PAGE_SIZE }).catch(() => null),
      api.listMyApplications({ page: 0, size: PAGE_SIZE }).catch(() => null),
      api.listMyInquiries().catch(() => []),
    ]).then(([reviews, apps, inquiries]) => {
      if (cancelled) return;
      setMyReviews(reviews ? reviews.content.map(toReviewPost).map((r) => ({ id: r.id, title: r.title, createdAt: r.createdAt })) : []);
      setReviewsPaging({ page: 0, totalPages: reviews ? Math.max(1, reviews.totalPages) : 1 });
      setMyApplications(apps ? apps.content : []);
      setAppsPaging({ page: 0, totalPages: apps ? Math.max(1, apps.totalPages) : 1 });
      setMyInquiries(inquiries);
    });
    return () => { cancelled = true; };
  }, [status, language]); // 언어 전환 시 번역된 내용(사진 반려 사유·문의·후기)으로 재조회

  const loadMoreApplications = async () => {
    const next = appsPaging.page + 1;
    try {
      const result = await api.listMyApplications({ page: next, size: PAGE_SIZE });
      setMyApplications((cur) => [...cur, ...result.content]);
      setAppsPaging({ page: next, totalPages: Math.max(1, result.totalPages) });
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "목록을 더 불러오지 못했습니다.");
    }
  };

  const loadMoreReviews = async () => {
    const next = reviewsPaging.page + 1;
    try {
      const result = await api.listMyReviews({ page: next, size: PAGE_SIZE });
      setMyReviews((cur) => [...cur, ...result.content.map(toReviewPost).map((r) => ({ id: r.id, title: r.title, createdAt: r.createdAt }))]);
      setReviewsPaging({ page: next, totalPages: Math.max(1, result.totalPages) });
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "목록을 더 불러오지 못했습니다.");
    }
  };

  const submitPassword = async (event: React.FormEvent) => {
    event.preventDefault();
    if (pw.next.length < 8 || pw.next.length > 72) { showToast("새 비밀번호는 8~72자로 입력해 주세요."); return; }
    if (pw.next !== pw.confirm) { showToast("새 비밀번호가 일치하지 않습니다."); return; }
    try {
      await api.changePassword(pw.current, pw.next);
      showToast("비밀번호가 변경되었습니다.");
      setPw({ current: "", next: "", confirm: "" });
      setPwOpen(false);
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "비밀번호 변경에 실패했습니다.");
    }
  };

  const toggleAppDetail = (id: number) => {
    setOpenAppId(openAppId === id ? null : id);
  };

  // 펼친 신청 상세 로드. 언어 전환 시에도 재조회해 사진 반려 사유 등을 번역된 내용으로 갱신한다.
  useEffect(() => {
    if (openAppId === null) { setAppDetail(null); return; }
    let cancelled = false;
    setAppDetail(null); setAppDetailLoading(true);
    api.getMyApplication(openAppId)
      .then((detail) => { if (!cancelled) setAppDetail(detail); })
      .catch((e) => {
        if (cancelled) return;
        showToast(e instanceof ApiError ? e.message : "신청 상세를 불러오지 못했습니다.");
        setOpenAppId(null);
      })
      .finally(() => { if (!cancelled) setAppDetailLoading(false); });
    return () => { cancelled = true; };
  }, [openAppId, language]);

  const cancelApplication = async (id: number) => {
    if (!window.confirm(t("이 신청을 취소하시겠습니까? 취소 후에는 되돌릴 수 없습니다."))) return;
    try {
      await api.cancelApplication(id);
      showToast("신청이 취소되었습니다.");
      // 취소 후에는 첫 페이지부터 다시 조회한다("더보기"로 누적된 상태는 초기화).
      const result = await api.listMyApplications({ page: 0, size: PAGE_SIZE });
      setMyApplications(result.content);
      setAppsPaging({ page: 0, totalPages: Math.max(1, result.totalPages) });
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "취소에 실패했습니다.");
    }
  };

  // 세션 확인 전(loading)에는 비로그인 화면을 먼저 그리지 않는다. 네트워크 장애(error)는 로그아웃과 구분해 재시도를 안내한다.
  if (status === "loading") {
    return (
      <section className="mypage mypage--guest page-container">
        <p>{t("로그인 상태를 확인하는 중입니다…")}</p>
      </section>
    );
  }
  if (status === "error") {
    return (
      <section className="mypage mypage--guest page-container">
        <h1>{t("일시적인 오류가 발생했습니다.")}</h1>
        <p>{t("네트워크 상태를 확인한 뒤 다시 시도해 주세요.")}</p>
        <button type="button" className="mypage__edit" onClick={() => { void refreshProfile(); }}>{t("다시 시도")}</button>
      </section>
    );
  }
  if (!user) {
    return (
      <section className="mypage mypage--guest page-container">
        <h1>{t("로그인이 필요합니다.")}</h1>
        <p>{t("제작 내역과 활동 내역을 확인하려면 먼저 로그인해 주세요.")}</p>
        <Link to="/login">{t("로그인")}</Link>
      </section>
    );
  }

  return (
    <div className="mypage">
      <header className="mypage__hero subpage-hero page-container">
        <p className="eyebrow">MY PAGE</p>
        <h1 className="subpage-hero__title">{t("마이페이지")}</h1>
        <p className="section-lead">{language === "en" ? `View your orders and account activity, ${user.name}.` : `${user.name}님의 제작 및 활동 내역을 확인할 수 있습니다.`}</p>
        <img className="mypage__hero-art" src="/images/support/support-bg.png" alt="" aria-hidden="true" />
      </header>

      <div className="mypage__actions page-container">
        <button type="button" className="mypage__edit" onClick={() => setEditing(!editing)}>
          {t("수정")} <span aria-hidden="true">›</span>
        </button>
        <button type="button" className="mypage__edit" onClick={() => setPwOpen(!pwOpen)}>{t("비밀번호 변경")} <span aria-hidden="true">›</span></button>
        <button type="button" className="mypage__edit mypage__edit--muted" onClick={async () => { if (!confirm(t("회원 탈퇴를 진행할까요?"))) return; await api.withdraw(); logout(); }}>{t("회원 탈퇴")}</button>
      </div>
      <section className="mypage__profile page-container">
        <div><span>{t("이름")}</span><strong>{user.name}</strong></div>
        <div><span>{t("이메일")}</span><strong>{user.email}</strong></div>
        <div><span>{t("전화번호")}</span><strong>{user.phone || "-"}</strong></div>
      </section>
      {/* PATCH /api/users/me는 name·phone만 처리한다(주소 수정은 백엔드 미지원 — FRONTEND_API_GAPS §1.9). */}
      {editing && <form className="mypage__profile page-container" onSubmit={async (event) => { event.preventDefault(); await api.updateMe({ name: profile.name, phone: profile.phone }); await refreshProfile(); setEditing(false); }}>
        <label className="field"><span className="field__label">{t("이름")}</span><input className="field__input" value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} /></label>
        <label className="field"><span className="field__label">{t("전화번호")}</span><input className="field__input" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} /></label>
        <Button type="submit">{t("저장")}</Button>
      </form>}
      {pwOpen && <form className="mypage__profile page-container" onSubmit={submitPassword}>
        <label className="field"><span className="field__label">{t("현재 비밀번호")}</span><input className="field__input" type="password" autoComplete="current-password" value={pw.current} onChange={(e) => setPw({ ...pw, current: e.target.value })} required /></label>
        <label className="field"><span className="field__label">{t("새 비밀번호")}</span><input className="field__input" type="password" autoComplete="new-password" value={pw.next} onChange={(e) => setPw({ ...pw, next: e.target.value })} placeholder={t("8~72자")} required /></label>
        <label className="field"><span className="field__label">{t("새 비밀번호 확인")}</span><input className="field__input" type="password" autoComplete="new-password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} required /></label>
        <Button type="submit">{t("비밀번호 변경")}</Button>
      </form>}

      <MySection id="production" title={t("제작 내역")}>
        <div className="mypage-list mypage-list--production">
          <div className="mypage-list__head"><span>{t("신청번호")}</span><span>{t("카드 종류")}</span><span>{t("신청일")}</span><span>{t("상태")}</span></div>
          {myApplications.map((application) => <Fragment key={application.applicationId}><article><strong><button type="button" className="mypage-appnum" onClick={() => toggleAppDetail(application.applicationId)} aria-expanded={openAppId === application.applicationId}>{application.applicationNumber}</button></strong><span>{application.cardTypeName}</span><time>{new Date(application.createdAt).toLocaleDateString(language === "en" ? "en-US" : "ko-KR")}</time><span className="mypage-status-cell"><b className="mypage-status">{t(APP_STATUS_LABELS[application.status])}</b>{CANCELLABLE.has(application.status) && <button type="button" className="mypage-cancel" onClick={() => cancelApplication(application.applicationId)}>{t("신청 취소")}</button>}</span></article>{openAppId === application.applicationId && <ApplicationDetail loading={appDetailLoading} detail={appDetail} />}</Fragment>)}
          {myApplications.length === 0 && <p className="mypage-list__empty">{t("제작 신청 내역이 없습니다.")}</p>}
          {appsPaging.page + 1 < appsPaging.totalPages && (
            <button type="button" className="mypage__edit" onClick={() => void loadMoreApplications()}>{t("더보기")} ›</button>
          )}
        </div>
      </MySection>

      <MySection title={t("후기")} action={<Link to="/reviews/new">{t("후기 작성")} ›</Link>}>
        <div className="mypage-list mypage-list--activity">
          {myReviews.map((review) => <article key={review.id}><Link to={`/reviews/${encodeURIComponent(review.id)}`}><strong>{review.title}</strong></Link><time>{review.createdAt.replace(/-/g, ".")}</time><Link className="mypage-list__edit" to={`/reviews/${encodeURIComponent(review.id)}/edit`} aria-label={language === "en" ? `Edit review "${review.title}"` : `${review.title} 후기 수정`}>{t("수정")}</Link></article>)}
          {myReviews.length === 0 && <p className="mypage-list__empty">{t("작성한 후기가 없습니다.")}</p>}
          {reviewsPaging.page + 1 < reviewsPaging.totalPages && (
            <button type="button" className="mypage__edit" onClick={() => void loadMoreReviews()}>{t("더보기")} ›</button>
          )}
        </div>
      </MySection>

      <MySection title={t("문의 내역")} action={<Link to="/inquiry">{t("문의하기")} ›</Link>}>
        <div className="mypage-list mypage-list--activity">
          {myInquiries.map((inquiry) => <article key={inquiry.id}><Link to={`/mypage/inquiry/${inquiry.id}`}><strong>{inquiry.title}</strong></Link><span className={`mypage-answer ${inquiry.status === "PENDING" ? "is-waiting" : ""}`}>{inquiry.status === "COMPLETED" ? t("문의 완료") : t("답변 대기")}</span><time>{new Date(inquiry.createdAt).toLocaleDateString(language === "en" ? "en-US" : "ko-KR")}</time></article>)}
          {myInquiries.length === 0 && <p className="mypage-list__empty">{t("접수한 문의가 없습니다.")}</p>}
        </div>
      </MySection>
    </div>
  );
}

// 내 신청 상세(GET /api/my/applications/{id}) 표시.
function ApplicationDetail({ loading, detail }: { loading: boolean; detail: AdminApplicationDetail | null }) {
  const { t, language } = useLanguage();
  if (loading) return <div className="mypage-appdetail">{t("불러오는 중…")}</div>;
  if (!detail) return null;
  const locale = language === "en" ? "en-US" : "ko-KR";
  const fmt = (iso?: string) => (iso ? new Date(iso).toLocaleString(locale) : undefined);
  const quantity = language === "en"
    ? `${detail.totalQuantity} ${detail.totalQuantity === 1 ? "card" : "cards"}`
    : `${detail.totalQuantity}매`;
  const rows: { label: string; value?: string }[] = [
    { label: "발급 방식", value: detail.issueType === "MOBILE_AND_PHYSICAL" ? t("모바일+실물") : t("모바일") },
    { label: "수량", value: quantity },
    { label: "입금자명", value: detail.depositorName },
    { label: "결제 상태", value: detail.paymentStatus === "CONFIRMED" ? t("입금 확인") : t("입금 대기") },
    { label: "환불", value: detail.refundedAt ? new Date(detail.refundedAt).toLocaleString(locale) : undefined },
    { label: "사진 반려 사유", value: detail.photoRejectReason },
    { label: "카드 발급 완료", value: fmt(detail.cardReadyAt) },
    { label: "실물 발송", value: fmt(detail.physicalDispatchedAt) },
    { label: "취소됨", value: fmt(detail.cancelledAt) },
  ].filter((r) => r.value);
  return (
    <div className="mypage-appdetail">
      <dl>
        {rows.map((r) => <div key={r.label}><dt>{t(r.label)}</dt><dd>{r.value}</dd></div>)}
      </dl>
    </div>
  );
}

function MySection({ id, title, action, children }: { id?: string; title: string; action?: React.ReactNode; children: React.ReactNode }) {
  return (
    <section id={id} className="mypage__section page-container">
      <div className="mypage__rule" aria-hidden="true" />
      <header><h2>{title}</h2>{action}</header>
      {children}
    </section>
  );
}
