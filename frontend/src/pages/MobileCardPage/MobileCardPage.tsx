import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api, type CardDownload, type LookupResult } from "../../services/api";
import { useLanguage } from "../../features/i18n/LanguageContext";
import "./MobileCardPage.css";

const CARD_FRONT = "/images/cards/width/kor-mouse-front.webp";
const CARD_BACK = "/images/cards/width/kor-mouse-back.webp";

export function MobileCardPage() {
  const { t } = useLanguage();
  const location = useLocation();
  const [flipped, setFlipped] = useState(false);
  const lookup = (location.state as { application?: LookupResult } | null)?.application ?? (() => { try { return JSON.parse(sessionStorage.getItem("last-application-lookup") || "null") as LookupResult | null; } catch { return null; } })();
  const [card, setCard] = useState<CardDownload | null>(null);
  const [uploading, setUploading] = useState(false);
  useEffect(() => { if (lookup?.applicationId) void api.getCardDownload(lookup.applicationId).then(setCard).catch(() => undefined); }, [lookup?.applicationId]);

  return (
    <section className="mobile-card-page page-container">
      <header className="mobile-card-page__header subpage-hero">
        <p className="eyebrow">{t("모바일 카드")}</p>
        <h1 className="subpage-hero__title">{t("나의 모바일 신분증")}</h1>
        <p className="section-lead">{t("카드를 누르면 앞면과 뒷면을 확인할 수 있습니다.")}</p>
      </header>

      <button
        type="button"
        className={`mobile-card${flipped ? " mobile-card--flipped" : ""}`}
        onClick={() => setFlipped((value) => !value)}
        aria-label={flipped ? t("카드 앞면 보기") : t("카드 뒷면 보기")}
        aria-pressed={flipped}
      >
        <span className="mobile-card__inner">
          <span className="mobile-card__face mobile-card__face--front">
            <img src={card?.cardFrontUrl || CARD_FRONT} alt={t("명예한국인증 모바일 카드 앞면")} />
          </span>
          <span className="mobile-card__face mobile-card__face--back">
            <img src={card?.cardBackUrl || CARD_BACK} alt={t("명예한국인증 모바일 카드 뒷면 이름풀이")} />
          </span>
        </span>
      </button>

      <p className="mobile-card-page__hint" aria-live="polite">
        {flipped ? t("현재 뒷면을 보고 있습니다 · 카드를 눌러 뒤집기") : t("현재 앞면을 보고 있습니다 · 카드를 눌러 뒤집기")}
      </p>
      {card?.downloadUrl && <a className="mobile-card-page__back" href={card.downloadUrl}>{t("카드 ZIP 다운로드")}</a>}
      {lookup?.status === "PHOTO_REJECTED" && (() => {
        // 스펙 §3.7: 개인은 photo, 단체는 submitFile(ZIP) 파트로 분기한다.
        const isGroup = lookup.applicationType === "GROUP";
        return <label className="field">
          <span className="field__label">{isGroup ? t("제출 파일(ZIP) 재업로드") : t("사진 재업로드")}</span>
          {lookup.photoRejectReason && <small className="mobile-card-page__reject">{t("반려 사유")}: {lookup.photoRejectReason}</small>}
          <input type="file" accept={isGroup ? ".zip,application/zip,application/x-zip-compressed" : "image/png,image/jpeg,image/webp"} disabled={uploading} onChange={async (event) => {
            const file = event.target.files?.[0]; if (!file || !lookup) return;
            setUploading(true);
            const form = new FormData();
            form.append(isGroup ? "submitFile" : "photo", file);
            try { await api.reuploadPhoto(lookup.applicationId, form); alert(isGroup ? t("제출 파일이 재업로드되었습니다.") : t("사진이 재업로드되었습니다.")); } finally { setUploading(false); }
          }} />
        </label>;
      })()}
      <Link className="mobile-card-page__back" to="/lookup">{t("신청 조회로 돌아가기")}</Link>
    </section>
  );
}
