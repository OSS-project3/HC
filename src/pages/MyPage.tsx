import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import "./MyPage.css";

export function MyPage() {
  const { user } = useAuth();

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
      <header className="subpage-hero page-container">
        <p className="eyebrow">MY PAGE</p>
        <h1 className="subpage-hero__title">마이페이지</h1>
        <p className="section-lead">{user.name}님의 제작 및 활동 내역을 확인할 수 있습니다.</p>
      </header>

      <section className="mypage__profile page-container">
        <div><span>이름</span><strong>{user.name}</strong></div>
        <div><span>이메일</span><strong>{user.email}</strong></div>
        <div><span>회원 유형</span><strong>{user.role === "admin" ? "관리자" : "일반 회원"}</strong></div>
      </section>

      <MySection title="제작 내역" action={<Link to="/lookup">신청 조회 ›</Link>}>
        <div className="mypage-list mypage-list--production">
          <div className="mypage-list__head"><span>신청번호</span><span>카드 종류</span><span>신청일</span><span>상태</span></div>
          <article><strong>APP-2026-000123</strong><span>명예 한국인증</span><time>2026.07.15</time><b className="mypage-status">제작 중</b></article>
        </div>
      </MySection>

      <MySection title="후기" action={<Link to="/reviews">후기 보기 ›</Link>}>
        <div className="mypage-list mypage-list--activity">
          <article><strong>한국에서의 추억이 이름과 카드로 남았어요.</strong><time>2026.07.28</time></article>
        </div>
      </MySection>

      <MySection title="문의 내역" action={<Link to="/support#contact">문의하기 ›</Link>}>
        <div className="mypage-list mypage-list--activity">
          <article><strong>모바일 카드 수령 관련 문의</strong><span className="mypage-answer">답변 완료</span><time>2026.07.30</time></article>
        </div>
      </MySection>
    </div>
  );
}

function MySection({ title, action, children }: { title: string; action: React.ReactNode; children: React.ReactNode }) {
  return (
    <section className="mypage__section page-container">
      <div className="mypage__rule" aria-hidden="true" />
      <header><h2>{title}</h2>{action}</header>
      {children}
    </section>
  );
}
