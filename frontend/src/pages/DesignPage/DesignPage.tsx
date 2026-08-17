// Design page: full card-design gallery by type.
import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import clsx from "clsx";
import { cardCategories, type CardCategory } from "../../data/cards";
import { CardCarousel } from "../../components/gallery/CardCarousel";
import { ChevronLeft, ChevronRight } from "../../components/ui/icons";
import "./DesignPage.css";

const PAIRS_PER_PAGE = 3; // 앞면 3 + 뒷면 3 = 한 페이지당 3쌍

export function DesignPage() {
  const { hash } = useLocation();

  useEffect(() => {
    const id = hash.slice(1);
    if (!id) return;
    requestAnimationFrame(() => {
      document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }, [hash]);

  return (
    <div className="design">
      <header className="design__hero subpage-hero page-container">
        <p className="eyebrow">디자인</p>
        <h1 className="design__title subpage-hero__title">카드 디자인</h1>
        <p className="section-lead design__lead">한국의 결을 따라 전통과 현재를 담다</p>
      </header>

      {cardCategories.map((cat) => (
        <DesignCategory cat={cat} key={cat.cardType} />
      ))}
    </div>
  );
}

/** One card category. Each page displays the next three delivered card pairs. */
function DesignCategory({ cat }: { cat: CardCategory }) {
  const [page, setPage] = useState(1);
  const orientation = cat.cards[0].orientation;
  const firstId = cat.cards[0]?.id ?? "";
  const isStudent = cat.cardType === "student";
  const totalPages = Math.max(1, Math.ceil(cat.cards.length / PAIRS_PER_PAGE));
  const pageCards = cat.cards.slice((page - 1) * PAIRS_PER_PAGE, page * PAIRS_PER_PAGE);

  return (
    <section
      className={clsx(
        "design__cat page-container",
        `design__cat--${orientation}`,
        `design__cat--${cat.cardType}`,
      )}
      id={cat.cardType}
    >
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
              <b>{String(page).padStart(2, "0")}</b> <i>/</i> <em>{String(totalPages).padStart(2, "0")}</em>
            </span>
            <button
              type="button"
              className="design__pager-arrow"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              aria-label="다음 페이지"
            >
              <ChevronRight width={18} height={18} />
            </button>
          </nav>
        </div>

        <CardCarousel
          cards={pageCards}
          orientation={orientation}
          page={1}
          layout={isStudent ? "student" : "default"}
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
