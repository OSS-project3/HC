import { useState } from "react";
import { Link } from "react-router-dom";
import "./MobileCardPage.css";

const CARD_FRONT = "/images/cards/design%20front1.png";
const CARD_BACK = "/images/cards/design%20back%201.png";

export function MobileCardPage() {
  const [flipped, setFlipped] = useState(false);

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
            <img src={CARD_FRONT} alt="명예한국인증 모바일 카드 앞면" />
          </span>
          <span className="mobile-card__face mobile-card__face--back">
            <img src={CARD_BACK} alt="명예한국인증 모바일 카드 뒷면 이름풀이" />
          </span>
        </span>
      </button>

      <p className="mobile-card-page__hint" aria-live="polite">
        현재 {flipped ? "뒷면" : "앞면"}을 보고 있습니다 · 카드를 눌러 뒤집기
      </p>
      <Link className="mobile-card-page__back" to="/lookup">신청 조회로 돌아가기</Link>
    </section>
  );
}
