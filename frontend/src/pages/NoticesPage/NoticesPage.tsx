import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import "../SupportPage/SupportPage.css";
import { useAuth } from "../../features/auth/AuthContext";
import { useLanguage } from "../../features/i18n/LanguageContext";
import { BoardAdminPanel } from "../../components/admin/BoardAdminPanel";
import { SelectField } from "../../components/ui/SelectField";
import { api, type BoardListItem } from "../../services/api";

function formatDate(iso: string) {
  return (iso ?? "").slice(0, 10).replace(/-/g, ".");
}

export function NoticesPage() {
  const { isAdmin } = useAuth();
  const { t, language } = useLanguage();
  const [notices, setNotices] = useState<BoardListItem[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [query, setQuery] = useState("");
  const [searchBy, setSearchBy] = useState("전체");

  const reload = useCallback(() => {
    setStatus("loading");
    api.listBoards({ type: "NOTICE", size: 100 })
      .then((data) => { setNotices(data.content); setStatus("ready"); })
      .catch(() => setStatus("error"));
  }, [language]); // Accept-Language가 응답 언어를 바꾸므로 언어 전환 시 재조회

  useEffect(() => { reload(); }, [reload]);

  const filteredNotices = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return notices;
    return notices.filter((notice) => {
      const target = searchBy === "작성일" ? formatDate(notice.createdAt) : notice.title;
      return target.toLowerCase().includes(keyword);
    });
  }, [query, searchBy, notices]);

  return (
    <div className="support notices-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">{t("고객지원")}</p>
        <h1 className="support__title subpage-hero__title">{t("공지사항")}</h1>
      </header>

      <section className="support__section page-container">
        {isAdmin && <BoardAdminPanel boardType="NOTICE" items={notices} onChanged={reload} />}
        <h2 className="support__heading">{t("공지사항")}</h2>

        <form className="notice-search" onSubmit={(event) => event.preventDefault()}>
          <SelectField
            ariaLabel={t("검색 조건")}
            value={searchBy}
            onChange={setSearchBy}
            options={[
              { value: "전체", label: t("전체") },
              { value: "제목", label: t("제목") },
              { value: "작성일", label: t("작성일") },
            ]}
          />
          <label>
            <span className="visually-hidden">{t("검색어 입력")}</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("검색어를 입력하세요")} />
            <button type="submit" aria-label={t("검색")}><SearchGlyph /></button>
          </label>
        </form>

        <div className="notice-table">
          <div className="notice-table__head"><span>{t("번호")}</span><span>{t("제목")}</span><span>{t("작성일")}</span></div>
          {status === "loading" && <p className="notice-table__empty">{t("공지사항을 불러오는 중입니다…")}</p>}
          {status === "error" && <p className="notice-table__empty">{t("공지사항을 불러오지 못했습니다.")}</p>}
          {status === "ready" && filteredNotices.map((notice) => (
            <article className="notice-table__row" key={notice.id}>
              <span className="notice-table__badge">{t("공지")}</span>
              <Link className="notice-table__title" to={`/notices/${notice.id}`}>{t(notice.title)}</Link>
              <time>{formatDate(notice.createdAt)}</time>
            </article>
          ))}
          {status === "ready" && filteredNotices.length === 0 && <p className="notice-table__empty">{t("검색 결과가 없습니다.")}</p>}
        </div>

        <nav className="support-pagination" aria-label={t("공지사항 페이지")}>
          <button aria-label={t("이전 페이지")} disabled>‹</button><b>1</b><button aria-label={t("다음 페이지")} disabled>›</button>
        </nav>
      </section>
    </div>
  );
}

function SearchGlyph() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>;
}
