// Design page: full card-design gallery by type.
import { useState } from "react";
import { Link } from "react-router-dom";
import clsx from "clsx";
import { cardCategories, type CardCategory } from "../data/cards";
import { CardCarousel } from "../components/gallery/CardCarousel";
import { ChevronLeft, ChevronRight } from "../components/ui/icons";
import "./DesignPage.css";

const TOTAL_PAGES = 5;

export function DesignPage() {
  return (
    <div className="design">
      <header className="design__hero page-container">
        <h1 className="design__title">카드 디자인</h1>
        <p className="section-lead design__lead">한국의 결을 따라 전통과 현재를 담다</p>
        <div className="design__lead-line" />
      </header>

      {cardCategories.map((cat) => (
        <DesignCategory cat={cat} key={cat.cardType} />
      ))}
    </div>
  );
}

/** One card category. Its pagination (1–5) doesn't change which cards show —
 *  page 1 is the plain art; pages 2–5 tint the same cards a different colour
 *  (placeholder until the real variant art is delivered). */
function DesignCategory({ cat }: { cat: CardCategory }) {
  const [page, setPage] = useState(1);
  const orientation = cat.cards[0].orientation;
  const firstId = cat.cards[0]?.id ?? "";

  return (
    <section className={clsx("design__cat page-container", `design__cat--${orientation}`)} id={cat.cardType}>
      {/* Inner wrapper sizes to the (reduced) card group so the title, rule and
          footer all align to the cards' left/right edges. */}
      <div className="design__cat-inner">
        <div className="design__cat-head">
          <h2 className="design__cat-title">{cat.title}</h2>
        </div>

        {/* Long rule below the title, with the pager pinned to its right. */}
        <div className="design__cat-bar">
          <span className="design__cat-line" aria-hidden="true" />
          <nav className="design__pager" aria-label={`${cat.title} 페이지`}>
            <button
              type="button"
              className="design__pager-arrow"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              aria-label="이전 페이지"
            >
              <ChevronLeft width={18} height={18} />
            </button>
            <span className="design__pager-count">
              <b>{String(page).padStart(2, "0")}</b> <i>/</i> <em>{String(TOTAL_PAGES).padStart(2, "0")}</em>
            </span>
            <button
              type="button"
              className="design__pager-arrow"
              onClick={() => setPage((p) => Math.min(TOTAL_PAGES, p + 1))}
              disabled={page === TOTAL_PAGES}
              aria-label="다음 페이지"
            >
              <ChevronRight width={18} height={18} />
            </button>
          </nav>
        </div>

        <CardCarousel
          cards={cat.cards}
          orientation={orientation}
          page={page}
          layout={cat.cardType === "student" ? "student" : "default"}
        />

        <div className="design__cat-foot">
          <p className="design__note">※ 위 증은 특허출원에 의한 견본품입니다</p>
          <Link className="design__apply-btn" to={`/apply?designId=${firstId}`}>
            제작 신청
          </Link>
        </div>
      </div>
    </section>
  );
}
