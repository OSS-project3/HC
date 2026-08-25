import { Link } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { toReviewPost } from "../../data/reviews";
import "./MyPage.css";
import { useEffect, useState } from "react";
import { api, type AdminApplicationListItem, type ApplicationStatus, type InquiryListItem } from "../../services/api";
import { Button } from "../../components/ui/Button";

const APP_STATUS_LABELS: Record<ApplicationStatus, string> = {
  SUBMITTED: "접수", REVIEWING: "검토중", PHOTO_REJECTED: "사진반려", NAME_EDITING: "작명중",
  PRODUCTION_READY: "제작대기", PRODUCING: "제작중", COMPLETED: "발급완료", CANCELLED: "취소",
};

export function MyPage() {
  const { user, refreshProfile, logout } = useAuth();
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(() => ({ name: user?.name || "", phone: user?.phone || "", address: user?.address || "" }));
  // 내 후기/신청/문의는 모두 백엔드 my-* API로 조회한다(서버 세션 = source "api"일 때만).
  const [myReviews, setMyReviews] = useState<{ id: string; title: string; createdAt: string }[]>([]);
  const [myApplications, setMyApplications] = useState<AdminApplicationListItem[]>([]);
  const [myInquiries, setMyInquiries] = useState<InquiryListItem[]>([]);

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
        {user.source === "api" && <button type="button" className="mypage__edit mypage__edit--muted" onClick={async () => { if (!confirm("회원 탈퇴를 진행할까요?")) return; await api.withdraw(); logout(); }}>회원 탈퇴</button>}
      </div>
      <section className="mypage__profile page-container">
        <div><span>이름</span><strong>{user.name}</strong></div>
        <div><span>이메일</span><strong>{user.email}</strong></div>
        <div><span>회원 유형</span><strong>{user.role === "admin" ? "관리자" : "일반 회원"}</strong></div>
      </section>
      {/* PATCH /api/users/me는 name·phone만 처리한다(주소 수정은 백엔드 미지원 — FRONTEND_API_GAPS §1.9). */}
      {editing && <form className="mypage__profile page-container" onSubmit={async (event) => { event.preventDefault(); if (user.source === "api") { await api.updateMe({ name: profile.name, phone: profile.phone }); await refreshProfile(); } setEditing(false); }}>
        <label className="field"><span className="field__label">이름</span><input className="field__input" value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} /></label>
        <label className="field"><span className="field__label">전화번호</span><input className="field__input" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} /></label>
        <Button type="submit">저장</Button>
      </form>}

      <MySection id="production" title="제작 내역">
        <div className="mypage-list mypage-list--production">
          <div className="mypage-list__head"><span>신청번호</span><span>카드 종류</span><span>신청일</span><span>상태</span></div>
          {myApplications.map((application) => <article key={application.applicationId}><strong>{application.applicationNumber}</strong><span>{application.cardTypeName}</span><time>{new Date(application.createdAt).toLocaleDateString("ko-KR")}</time><b className="mypage-status">{APP_STATUS_LABELS[application.status]}</b></article>)}
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

function MySection({ id, title, action, children }: { id?: string; title: string; action?: React.ReactNode; children: React.ReactNode }) {
  return (
    <section id={id} className="mypage__section page-container">
      <div className="mypage__rule" aria-hidden="true" />
      <header><h2>{title}</h2>{action}</header>
      {children}
    </section>
  );
}
