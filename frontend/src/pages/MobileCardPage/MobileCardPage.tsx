import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api, type CardDownload, type LookupResult } from "../../services/api";
import "./MobileCardPage.css";

const CARD_FRONT = "/images/cards/width/kor-mouse-front.png";
const CARD_BACK = "/images/cards/width/kor-mouse-back.jpg";

export function MobileCardPage() {
  const location = useLocation();
  const [flipped, setFlipped] = useState(false);
  const lookup = (location.state as { application?: LookupResult } | null)?.application ?? (() => { try { return JSON.parse(sessionStorage.getItem("last-application-lookup") || "null") as LookupResult | null; } catch { return null; } })();
  const [card, setCard] = useState<CardDownload | null>(null);
  const [uploading, setUploading] = useState(false);
  useEffect(() => { if (lookup?.applicationId) void api.getCardDownload(lookup.applicationId).then(setCard).catch(() => undefined); }, [lookup?.applicationId]);

  return (
    <section className="mobile-card-page page-container">
      <header className="mobile-card-page__header subpage-hero">
        <p className="eyebrow">모바일 카드</p>
        <h1 className="subpage-hero__title">나의 모바일 신분증</h1>
        <p className="section-lead">카드를 누르면 앞면과 뒷면을 확인할 수 있습니다.</p>
      </header>

      <button
        type="button"
        className={`mobile-card${flipped ? " mobile-card--flipped" : ""}`}
        onClick={() => setFlipped((value) => !value)}
        aria-label={flipped ? "카드 앞면 보기" : "카드 뒷면 보기"}
        aria-pressed={flipped}
      >
        <span className="mobile-card__inner">
          <span className="mobile-card__face mobile-card__face--front">
            <img src={card?.cardFrontUrl || CARD_FRONT} alt="명예한국인증 모바일 카드 앞면" />
          </span>
          <span className="mobile-card__face mobile-card__face--back">
            <img src={card?.cardBackUrl || CARD_BACK} alt="명예한국인증 모바일 카드 뒷면 이름풀이" />
          </span>
        </span>
      </button>

      <p className="mobile-card-page__hint" aria-live="polite">
        현재 {flipped ? "뒷면" : "앞면"}을 보고 있습니다 · 카드를 눌러 뒤집기
      </p>
      {card?.downloadUrl && <a className="mobile-card-page__back" href={card.downloadUrl}>카드 ZIP 다운로드</a>}
      {lookup?.status === "PHOTO_REJECTED" && <label className="field"><span className="field__label">사진 재업로드</span><input type="file" accept="image/png,image/jpeg,image/webp" disabled={uploading} onChange={async (event) => { const file = event.target.files?.[0]; if (!file || !lookup) return; setUploading(true); const form = new FormData(); form.append("photo", file); try { await api.reuploadPhoto(lookup.applicationId, form); alert("사진이 재업로드되었습니다."); } finally { setUploading(false); } }} /></label>}
      <Link className="mobile-card-page__back" to="/lookup">신청 조회로 돌아가기</Link>
    </section>
  );
}
