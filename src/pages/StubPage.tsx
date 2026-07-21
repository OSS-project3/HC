// Generic placeholder page for routes not yet built out.
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
  return (
    <section className="stub page-container">
      <p className="eyebrow">HANGUL &amp; SEJONG</p>
      <h1 className="stub__title">{title}</h1>
      <p className="stub__note">{note}</p>
      <div className="stub__frame" aria-hidden="true">
        준비 중
      </div>
    </section>
  );
}
