import { Link } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { loadInquiries } from "../../data/inquiries";
import { adminStatusLabels, loadApplications } from "../../data/adminMock";
import { toReviewPost } from "../../data/reviews";
import "./MyPage.css";
import { useEffect, useState } from "react";
import { api } from "../../services/api";
import { Button } from "../../components/ui/Button";

export function MyPage() {
  const { user, refreshProfile, logout } = useAuth();
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState(() => ({ name: user?.name || "", phone: user?.phone || "", address: user?.address || "" }));
  // 내 후기는 백엔드 GET /api/my/reviews로 연동. (로그인 세션이 API 기반일 때만 호출)
  const [myReviews, setMyReviews] = useState<{ id: string; title: string; createdAt: string }[]>([]);

  useEffect(() => {
    if (user?.source !== "api") { setMyReviews([]); return; }
    let cancelled = false;
    api.listMyReviews({ size: 100 })
      .then((data) => { if (!cancelled) setMyReviews(data.content.map(toReviewPost).map((r) => ({ id: r.id, title: r.title, createdAt: r.createdAt }))); })
      .catch(() => { if (!cancelled) setMyReviews([]); });
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

  // 내 문의/신청 목록은 백엔드에 my-* 조회 API가 아직 없어(또는 미커밋) 목데이터로 남아 있다.
  const myInquiries = loadInquiries().filter((inquiry) => inquiry.email === user.email);
  const myApplications = loadApplications().filter((application) => application.applicantEmail === user.email);

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
          {myApplications.map((application) => <article key={application.applicationNumber}><strong>{application.applicationNumber}</strong><span>{application.cardType}</span><time>{application.submittedAt.replace(/-/g, ".")}</time><b className="mypage-status">{adminStatusLabels[application.status]}</b></article>)}
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
          {myInquiries.map((inquiry) => <article key={inquiry.id}><Link to={`/mypage/inquiry/${encodeURIComponent(inquiry.id)}`}><strong>{inquiry.title}</strong></Link><span className={`mypage-answer ${inquiry.status === "PENDING" ? "is-waiting" : ""}`}>{inquiry.status === "COMPLETED" ? "문의 완료" : "답변 대기"}</span><time>{new Date(inquiry.createdAt).toLocaleDateString("ko-KR")}</time></article>)}
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
