import { Link, useParams } from "react-router-dom";
import { notices } from "../SupportPage/SupportPage";
import "./NoticeDetailPage.css";
import { loadManagedContent } from "../../components/admin/ContentAdminPanel";

export function NoticeDetailPage() {
  const { noticeId } = useParams();
  const managed = loadManagedContent("notices", notices.map((item) => ({ id: item.id, title: item.title, content: item.content, meta: item.date })));
  const index = managed.findIndex((notice) => notice.id === noticeId);
  const notice = managed[index];

  if (!notice) {
    return (
      <section className="notice-detail page-container">
        <h1>공지사항을 찾을 수 없습니다.</h1>
        <Link className="notice-detail__list" to="/notices">목록</Link>
      </section>
    );
  }

  const nextNotice = managed[index + 1];
  const downloadText = encodeURIComponent(`${notice.title}\n\n${notice.content}`);

  return (
    <article className="notice-detail page-container">
      <header className="notice-detail__hero subpage-hero">
        <p className="eyebrow">고객지원</p>
        <h1 className="subpage-hero__title">공지사항</h1>
      </header>

      <header className="notice-detail__head">
        <h2>{notice.title}</h2>
        <time>{notice.meta}</time>
      </header>

      <div className="notice-detail__body">
        {notice.content.split("\n").map((line) => <p key={line}>{line}</p>)}
      </div>

      <section className="notice-detail__attachment">
        <h3>첨부파일</h3>
        <div>
          <span aria-hidden="true">⌕</span>
          <p>{`${notice.title}.txt`}</p>
          <a href={`data:text/plain;charset=utf-8,${downloadText}`} download={`${notice.title}.txt`}>다운로드 <b aria-hidden="true">↓</b></a>
        </div>
      </section>

      <div className="notice-detail__actions">
        <Link className="notice-detail__list" to="/notices">목록</Link>
      </div>

      {nextNotice && (
        <Link className="notice-detail__next" to={`/notices/${nextNotice.id}`}>
          <span>⌄　다음글</span><strong>{nextNotice.title}</strong>
        </Link>
      )}
    </article>
  );
}
