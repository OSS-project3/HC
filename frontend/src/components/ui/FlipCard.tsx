// Reusable mobile-card viewer: shows the front, flips to the back on click.
import { useState } from "react";
import "./FlipCard.css";
import { useLanguage } from "../../features/i18n/LanguageContext";

interface FlipCardProps {
  frontUrl: string;
  backUrl: string;
  frontAlt?: string;
  backAlt?: string;
}

export function FlipCard({
  frontUrl,
  backUrl,
  frontAlt = "모바일 카드 앞면",
  backAlt = "모바일 카드 뒷면",
}: FlipCardProps) {
  const { t } = useLanguage();
  const [flipped, setFlipped] = useState(false);

  return (
    <div className="flipcard-wrap">
      <button
        type="button"
        className={`flipcard${flipped ? " flipcard--flipped" : ""}`}
        onClick={() => setFlipped((value) => !value)}
        aria-label={flipped ? t("카드 앞면 보기") : t("카드 뒷면 보기")}
        aria-pressed={flipped}
      >
        <span className="flipcard__inner">
          <span className="flipcard__face flipcard__face--front">
            <img src={frontUrl} alt={t(frontAlt)} />
          </span>
          <span className="flipcard__face flipcard__face--back">
            <img src={backUrl} alt={t(backAlt)} />
          </span>
        </span>
      </button>
    </div>
  );
}
