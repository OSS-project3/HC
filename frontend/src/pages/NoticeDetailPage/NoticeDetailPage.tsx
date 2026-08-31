import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import "./NoticeDetailPage.css";
import { useLanguage } from "../../features/i18n/LanguageContext";
import { api, type BoardDetail } from "../../services/api";

function formatDate(iso: string) {
  return (iso ?? "").slice(0, 10).replace(/-/g, ".");
}

export function NoticeDetailPage() {
  const { noticeId } = useParams();
  const { t, language } = useLanguage();
  const [notice, setNotice] = useState<BoardDetail | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");

  useEffect(() => {
    const id = Number(noticeId);
    if (!Number.isFinite(id)) { setStatus("error"); return; }
    let cancelled = false;
    setStatus("loading");
    api.getBoard(id)
      .then((data) => { if (!cancelled) { setNotice(data); setStatus("ready"); } })
      .catch(() => { if (!cancelled) setStatus("error"); });
    return () => { cancelled = true; };
  }, [noticeId, language]); // 언어 전환 시 번역된 내용으로 재조회

  if (status === "loading") return <section className="notice-detail page-container"><h1>{t("공지사항을 불러오는 중입니다…")}</h1></section>;
  if (status === "error" || !notice) {
    return (
      <section className="notice-detail page-container">
        <h1>{t("공지사항을 찾을 수 없습니다.")}</h1>
        <Link className="notice-detail__list" to="/notices">{t("목록")}</Link>
      </section>
    );
  }

  return (
    <article className="notice-detail page-container">
      <header className="notice-detail__hero subpage-hero">
        <p className="eyebrow">{t("고객지원")}</p>
        <h1 className="subpage-hero__title">{t("공지사항")}</h1>
      </header>

      <header className="notice-detail__head">
        <h2>{t(notice.title)}</h2>
        <time>{formatDate(notice.createdAt)}</time>
      </header>

      <div className="notice-detail__body">
        {notice.content.split("\n").map((line, index) => <p key={`${index}-${line}`}>{line}</p>)}
      </div>

      {notice.attachments.length > 0 && (
        <section className="notice-detail__attachment">
          <h3>{t("첨부파일")}</h3>
          {notice.attachments.map((att) => (
            <div key={att.id}>
              <span aria-hidden="true">⌕</span>
              <p>{att.originalFileName}</p>
              <a href={att.url} target="_blank" rel="noreferrer" download={att.originalFileName}>{t("다운로드")} <b aria-hidden="true">↓</b></a>
            </div>
          ))}
        </section>
      )}

      <div className="notice-detail__actions">
        <Link className="notice-detail__list" to="/notices">{t("목록")}</Link>
      </div>

      {notice.next && (
        <Link className="notice-detail__next" to={`/notices/${notice.next.id}`}>
          <span>⌄　{t("다음글")}</span><strong>{t(notice.next.title)}</strong>
        </Link>
      )}
    </article>
  );
}
