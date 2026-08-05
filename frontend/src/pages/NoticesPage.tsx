import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { notices } from "./SupportPage";
import "./SupportPage.css";
import { useAuth } from "../features/auth/AuthContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../components/admin/ContentAdminPanel";

export function NoticesPage() {
  const { isAdmin } = useAuth();
  const defaults: ManagedContent[] = notices.map((notice) => ({ id: notice.id, title: notice.title, content: notice.content, meta: notice.date }));
  const [managedNotices, setManagedNotices] = useState(() => loadManagedContent("notices", defaults));
  const updateNotices = (items: ManagedContent[]) => { localStorage.setItem("managed-content:notices", JSON.stringify(items)); setManagedNotices(items); };
  const [query, setQuery] = useState("");
  const [searchBy, setSearchBy] = useState("전체");

  const filteredNotices = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return managedNotices;
    return managedNotices.filter((notice) => {
      const target = searchBy === "작성일" ? notice.meta || "" : notice.title;
      return target.toLowerCase().includes(keyword);
    });
  }, [query, searchBy, managedNotices]);

  return (
    <div className="support notices-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">고객지원</p>
        <h1 className="support__title subpage-hero__title">공지사항</h1>
      </header>

      <section className="support__section page-container">
        {isAdmin && <ContentAdminPanel label="공지사항" items={managedNotices} onChange={updateNotices} />}
        <div className="support-rule" aria-hidden="true"><i /></div>
        <h2 className="support__heading">공지사항</h2>

        <form className="notice-search" onSubmit={(event) => event.preventDefault()}>
          <select value={searchBy} onChange={(event) => setSearchBy(event.target.value)} aria-label="검색 조건">
            <option>전체</option>
            <option>제목</option>
            <option>작성일</option>
          </select>
          <label>
            <span className="visually-hidden">검색어 입력</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="검색어를 입력하세요" />
            <button type="submit" aria-label="검색"><SearchGlyph /></button>
          </label>
        </form>

        <div className="notice-table">
          <div className="notice-table__head"><span>번호</span><span>제목</span><span>작성일</span></div>
          {filteredNotices.map((notice) => (
            <article className="notice-table__row" key={notice.title}>
              <span className="notice-table__badge">공지</span>
              <Link className="notice-table__title" to={`/notices/${notice.id}`}>{notice.title}</Link>
              <time>{notice.meta}</time>
            </article>
          ))}
          {filteredNotices.length === 0 && <p className="notice-table__empty">검색 결과가 없습니다.</p>}
        </div>

        <nav className="support-pagination" aria-label="공지사항 페이지">
          <button aria-label="이전 페이지" disabled>‹</button><b>1</b><button aria-label="다음 페이지" disabled>›</button>
        </nav>
      </section>
    </div>
  );
}

function SearchGlyph() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>;
}
