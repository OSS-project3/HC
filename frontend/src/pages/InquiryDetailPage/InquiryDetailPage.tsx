// 문의 상세: 마이페이지 문의 내역에서 제목을 눌러 진입한다.
// 본인 문의만 열람 가능하며, 관리자 답변이 등록되면 답변 내용을 확인할 수 있다.
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { findInquiry } from "../../data/inquiries";
import "./InquiryDetailPage.css";

function formatDate(iso: string) {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString("ko-KR");
}

export function InquiryDetailPage() {
  const { inquiryId } = useParams();
  const { user } = useAuth();
  const inquiry = inquiryId ? findInquiry(decodeURIComponent(inquiryId)) : undefined;

  // 로그인하지 않았거나, 본인 문의가 아니면 열람할 수 없다.
  const canView = inquiry && user && (user.email === inquiry.email || user.role === "admin");

  if (!inquiry || !canView) {
    return (
      <section className="inquiry-detail page-container">
        <header className="inquiry-detail__hero subpage-hero">
          <p className="eyebrow">고객지원</p>
          <h1 className="subpage-hero__title">문의 내역</h1>
        </header>
        <p className="inquiry-detail__empty">
          {user ? "문의 내역을 찾을 수 없거나 열람 권한이 없습니다." : "문의 내역을 확인하려면 먼저 로그인해 주세요."}
        </p>
        <div className="inquiry-detail__actions">
          <Link className="inquiry-detail__list" to={user ? "/mypage" : "/login"}>{user ? "마이페이지" : "로그인"}</Link>
        </div>
      </section>
    );
  }

  const answered = inquiry.status === "COMPLETED" && !!inquiry.answer;

  return (
    <article className="inquiry-detail page-container">
      <header className="inquiry-detail__hero subpage-hero">
        <p className="eyebrow">고객지원</p>
        <h1 className="subpage-hero__title">문의 내역</h1>
      </header>

      <header className="inquiry-detail__head">
        <div className="inquiry-detail__meta">
          <span className="inquiry-detail__category">{inquiry.category}</span>
          <span className={`inquiry-detail__status ${inquiry.status === "PENDING" ? "is-waiting" : "is-completed"}`}>
            {inquiry.status === "COMPLETED" ? "문의 완료" : "답변 대기"}
          </span>
        </div>
        <h2>{inquiry.title}</h2>
        <time>{formatDate(inquiry.createdAt)}</time>
      </header>

      <section className="inquiry-detail__block">
        <h3>문의 내용</h3>
        <div className="inquiry-detail__body">
          {inquiry.content.split("\n").map((line, index) => <p key={index}>{line || " "}</p>)}
        </div>
      </section>

      <section className="inquiry-detail__block inquiry-detail__answer">
        <h3>답변</h3>
        {answered ? (
          <>
            <div className="inquiry-detail__body">
              {inquiry.answer!.split("\n").map((line, index) => <p key={index}>{line || " "}</p>)}
            </div>
            {inquiry.answeredAt && <time className="inquiry-detail__answered-at">{formatDate(inquiry.answeredAt)} 답변</time>}
          </>
        ) : (
          <p className="inquiry-detail__pending">아직 답변이 등록되지 않았습니다. 답변이 완료되면 이곳에서 확인하실 수 있습니다.</p>
        )}
      </section>

      <div className="inquiry-detail__actions">
        <Link className="inquiry-detail__list" to="/mypage">목록</Link>
        <Link className="inquiry-detail__more" to="/inquiry">문의하기</Link>
      </div>
    </article>
  );
}
