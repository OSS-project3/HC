// Generic placeholder page for routes not yet built out.
import { useLanguage } from "../../features/i18n/LanguageContext";
import "./StubPage.css";

interface StubPageProps {
  title: string;
  note: string;
}

/**
 * Placeholder frame for pages whose final design has not been delivered yet
 * (회사 소개 / 후기 / 행사사업 / 관리 …). Intentionally minimal so it is never
 * mistaken for a finished screen.
 */
export function StubPage({ title, note }: StubPageProps) {
  const { t } = useLanguage();
  return (
    <section className="stub page-container">
      <header className="subpage-hero">
        <p className="eyebrow">HANGUL &amp; SEJONG</p>
        <h1 className="stub__title subpage-hero__title">{t(title)}</h1>
        <p className="stub__note">{t(note)}</p>
      </header>
      <div className="stub__frame" aria-hidden="true">
        {t("준비 중")}
      </div>
    </section>
  );
}
