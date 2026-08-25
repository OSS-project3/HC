import { Link } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { toReviewPost } from "../../data/reviews";
import "./MyPage.css";
import { Fragment, useEffect, useState } from "react";
import { api, ApiError, type AdminApplicationDetail, type AdminApplicationListItem, type ApplicationStatus, type InquiryListItem } from "../../services/api";
import { Button } from "../../components/ui/Button";
import { showToast } from "../../components/ui/toast";

const APP_STATUS_LABELS: Record<ApplicationStatus, string> = {
  SUBMITTED: "접수", REVIEWING: "검토중", PHOTO_REJECTED: "사진반려", NAME_EDITING: "작명중",
  PRODUCTION_READY: "제작대기", PRODUCING: "제작중", COMPLETED: "발급완료", CANCELLED: "취소",
};
// 취소 가능 상태(백엔드 canCancelByUser와 동일).
const CANCELLABLE = new Set<ApplicationStatus>(["SUBMITTED", "REVIEWING", "PHOTO_REJECTED"]);

export function MyPage() {
  const { user, refreshProfile, logout } = useAuth();
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(() => ({ name: user?.name || "", phone: user?.phone || "", address: user?.address || "" }));
  // 비밀번호 변경(PATCH /api/users/me/password) — 서버 세션(source "api")에서만 노출.
  const [pwOpen, setPwOpen] = useState(false);
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  // 내 후기/신청/문의는 모두 백엔드 my-* API로 조회한다(서버 세션 = source "api"일 때만).
  const [myReviews, setMyReviews] = useState<{ id: string; title: string; createdAt: string }[]>([]);
  const [myApplications, setMyApplications] = useState<AdminApplicationListItem[]>([]);
  const [myInquiries, setMyInquiries] = useState<InquiryListItem[]>([]);
  // 내 신청 상세(GET /api/my/applications/{id}) — 행을 펼치면 로드.
  const [openAppId, setOpenAppId] = useState<number | null>(null);
  const [appDetail, setAppDetail] = useState<AdminApplicationDetail | null>(null);
  const [appDetailLoading, setAppDetailLoading] = useState(false);

  useEffect(() => {
    if (user?.source !== "api") { setMyReviews([]); setMyApplications([]); setMyInquiries([]); return; }
    let cancelled = false;
    Promise.all([
      api.listMyReviews({ size: 100 }).then((d) => d.content.map(toReviewPost).map((r) => ({ id: r.id, title: r.title, createdAt: r.createdAt }))).catch(() => []),
      api.listMyApplications({ size: 100 }).then((d) => d.content).catch(() => []),
      api.listMyInquiries().catch(() => []),
    ]).then(([reviews, apps, inquiries]) => {
      if (cancelled) return;
      setMyReviews(reviews);
      setMyApplications(apps);
      setMyInquiries(inquiries);
    });
    return () => { cancelled = true; };
  }, [user?.source]);

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

  const toggleAppDetail = async (id: number) => {
    if (openAppId === id) { setOpenAppId(null); setAppDetail(null); return; }
    setOpenAppId(id); setAppDetail(null); setAppDetailLoading(true);
    try {
      setAppDetail(await api.getMyApplication(id));
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "신청 상세를 불러오지 못했습니다.");
      setOpenAppId(null);
    } finally {
      setAppDetailLoading(false);
    }
  };

  const cancelApplication = async (id: number) => {
    if (!window.confirm("이 신청을 취소하시겠습니까? 취소 후에는 되돌릴 수 없습니다.")) return;
    try {
      await api.cancelApplication(id);
      showToast("신청이 취소되었습니다.");
      const page = await api.listMyApplications({ size: 100 });
      setMyApplications(page.content);
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "취소에 실패했습니다.");
    }
  };

  if (!user) {
    return (
      <section className="mypage mypage--guest page-container">
        <h1>로그인이 필요합니다.</h1>
        <p>제작 내역과 활동 내역을 확인하려면 먼저 로그인해 주세요.</p>
        <Link to="/login">로그인</Link>
      </section>
    );
  }

  return (
    <div className="mypage">
      <header className="mypage__hero subpage-hero page-container">
        <p className="eyebrow">MY PAGE</p>
        <h1 className="subpage-hero__title">마이페이지</h1>
        <p className="section-lead">{user.name}님의 제작 및 활동 내역을 확인할 수 있습니다.</p>
        <img className="mypage__hero-art" src="/images/support/support-bg.png" alt="" aria-hidden="true" />
      </header>

      <div className="mypage__actions page-container">
        <button type="button" className="mypage__edit" onClick={() => setEditing(!editing)}>
          수정 <span aria-hidden="true">›</span>
        </button>
        {user.source === "api" && <button type="button" className="mypage__edit" onClick={() => setPwOpen(!pwOpen)}>비밀번호 변경 <span aria-hidden="true">›</span></button>}
        {user.source === "api" && <button type="button" className="mypage__edit mypage__edit--muted" onClick={async () => { if (!confirm("회원 탈퇴를 진행할까요?")) return; await api.withdraw(); logout(); }}>회원 탈퇴</button>}
      </div>
      <section className="mypage__profile page-container">
        <div><span>이름</span><strong>{user.name}</strong></div>
        <div><span>이메일</span><strong>{user.email}</strong></div>
        <div><span>전화번호</span><strong>{user.phone || "-"}</strong></div>
      </section>
      {/* PATCH /api/users/me는 name·phone만 처리한다(주소 수정은 백엔드 미지원 — FRONTEND_API_GAPS §1.9). */}
      {editing && <form className="mypage__profile page-container" onSubmit={async (event) => { event.preventDefault(); if (user.source === "api") { await api.updateMe({ name: profile.name, phone: profile.phone }); await refreshProfile(); } setEditing(false); }}>
        <label className="field"><span className="field__label">이름</span><input className="field__input" value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} /></label>
        <label className="field"><span className="field__label">전화번호</span><input className="field__input" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} /></label>
        <Button type="submit">저장</Button>
      </form>}
      {pwOpen && user.source === "api" && <form className="mypage__profile page-container" onSubmit={submitPassword}>
        <label className="field"><span className="field__label">현재 비밀번호</span><input className="field__input" type="password" autoComplete="current-password" value={pw.current} onChange={(e) => setPw({ ...pw, current: e.target.value })} required /></label>
        <label className="field"><span className="field__label">새 비밀번호</span><input className="field__input" type="password" autoComplete="new-password" value={pw.next} onChange={(e) => setPw({ ...pw, next: e.target.value })} placeholder="8~72자" required /></label>
        <label className="field"><span className="field__label">새 비밀번호 확인</span><input className="field__input" type="password" autoComplete="new-password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} required /></label>
        <Button type="submit">비밀번호 변경</Button>
      </form>}

      <MySection id="production" title="제작 내역">
        <div className="mypage-list mypage-list--production">
          <div className="mypage-list__head"><span>신청번호</span><span>카드 종류</span><span>신청일</span><span>상태</span></div>
          {myApplications.map((application) => <Fragment key={application.applicationId}><article><strong><button type="button" className="mypage-appnum" onClick={() => toggleAppDetail(application.applicationId)} aria-expanded={openAppId === application.applicationId}>{application.applicationNumber}</button></strong><span>{application.cardTypeName}</span><time>{new Date(application.createdAt).toLocaleDateString("ko-KR")}</time><span className="mypage-status-cell"><b className="mypage-status">{APP_STATUS_LABELS[application.status]}</b>{CANCELLABLE.has(application.status) && <button type="button" className="mypage-cancel" onClick={() => cancelApplication(application.applicationId)}>신청 취소</button>}</span></article>{openAppId === application.applicationId && <ApplicationDetail loading={appDetailLoading} detail={appDetail} />}</Fragment>)}
          {myApplications.length === 0 && <p className="mypage-list__empty">제작 신청 내역이 없습니다.</p>}
        </div>
      </MySection>

      <MySection title="후기" action={<Link to="/reviews/new">후기 작성 ›</Link>}>
        <div className="mypage-list mypage-list--activity">
          {myReviews.map((review) => <article key={review.id}><Link to={`/reviews/${encodeURIComponent(review.id)}`}><strong>{review.title}</strong></Link><time>{review.createdAt.replace(/-/g, ".")}</time><Link className="mypage-list__edit" to={`/reviews/${encodeURIComponent(review.id)}/edit`} aria-label={`${review.title} 후기 수정`}>수정</Link></article>)}
          {myReviews.length === 0 && <p className="mypage-list__empty">작성한 후기가 없습니다.</p>}
        </div>
      </MySection>

      <MySection title="문의 내역" action={<Link to="/inquiry">문의하기 ›</Link>}>
        <div className="mypage-list mypage-list--activity">
          {myInquiries.map((inquiry) => <article key={inquiry.id}><Link to={`/mypage/inquiry/${inquiry.id}`}><strong>{inquiry.title}</strong></Link><span className={`mypage-answer ${inquiry.status === "PENDING" ? "is-waiting" : ""}`}>{inquiry.status === "COMPLETED" ? "문의 완료" : "답변 대기"}</span><time>{new Date(inquiry.createdAt).toLocaleDateString("ko-KR")}</time></article>)}
          {myInquiries.length === 0 && <p className="mypage-list__empty">접수한 문의가 없습니다.</p>}
        </div>
      </MySection>
    </div>
  );
}

// 내 신청 상세(GET /api/my/applications/{id}) 표시.
function ApplicationDetail({ loading, detail }: { loading: boolean; detail: AdminApplicationDetail | null }) {
  if (loading) return <div className="mypage-appdetail">불러오는 중…</div>;
  if (!detail) return null;
  const fmt = (iso?: string) => (iso ? new Date(iso).toLocaleString("ko-KR") : undefined);
  const rows: { label: string; value?: string }[] = [
    { label: "발급 방식", value: detail.issueType === "MOBILE_AND_PHYSICAL" ? "모바일+실물" : "모바일" },
    { label: "수량", value: `${detail.totalQuantity}매` },
    { label: "결제 상태", value: detail.paymentStatus === "CONFIRMED" ? "입금 확인" : "입금 대기" },
    { label: "환불", value: detail.refundedAt ? new Date(detail.refundedAt).toLocaleString("ko-KR") : undefined },
    { label: "사진 반려 사유", value: detail.photoRejectReason },
    { label: "카드 발급 완료", value: fmt(detail.cardReadyAt) },
    { label: "실물 발송", value: fmt(detail.physicalDispatchedAt) },
    { label: "취소", value: fmt(detail.cancelledAt) },
  ].filter((r) => r.value);
  return (
    <div className="mypage-appdetail">
      <dl>
        {rows.map((r) => <div key={r.label}><dt>{r.label}</dt><dd>{r.value}</dd></div>)}
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
