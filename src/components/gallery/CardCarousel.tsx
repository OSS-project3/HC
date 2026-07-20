import { useEffect, useState } from "react";
import type { CardDesign } from "../../data/cards";
import { SampleCard } from "../brand/SampleCard";
import { ChevronLeft, ChevronRight } from "../ui/icons";

interface CardCarouselProps {
  cards: CardDesign[];
  orientation: "landscape" | "portrait";
}

/** Responsive slides-per-view: landscape 1→2→3, portrait 1→2. */
function usePerView(orientation: "landscape" | "portrait") {
  const compute = () => {
    if (typeof window === "undefined") return 1;
    const w = window.innerWidth;
    if (orientation === "landscape") {
      if (w >= 1200) return 3;
      if (w >= 768) return 2;
      return 1;
    }
    if (w >= 900) return 2;
    return 1;
  };
  const [perView, setPerView] = useState(compute);
  useEffect(() => {
    const onResize = () => setPerView(compute());
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orientation]);
  return perView;
}

export function CardCarousel({ cards, orientation }: CardCarouselProps) {
  const perView = usePerView(orientation);
  const totalPages = Math.max(1, Math.ceil(cards.length / perView));
  const [page, setPage] = useState(0);

  // Keep the current page valid when perView changes.
  useEffect(() => {
    setPage((p) => Math.min(p, totalPages - 1));
  }, [totalPages]);

  const start = page * perView;
  const visible = cards.slice(start, start + perView);

  return (
    <div className="carousel">
      <div className={`carousel__track carousel__track--${orientation}`}>
        {visible.map((card) => (
          <div className="carousel__slide" key={card.id}>
            <SampleCard design={card} variant="front" />
            <SampleCard design={card} variant="reading" />
          </div>
        ))}
      </div>

      <div className="carousel__pager">
        <button
          className="carousel__arrow"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={page === 0}
          aria-label="이전"
        >
          <ChevronLeft width={20} height={20} />
        </button>
        <span className="carousel__count">
          {String(page + 1).padStart(2, "0")} <i>/</i> {String(totalPages).padStart(2, "0")}
        </span>
        <button
          className="carousel__arrow"
          onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
          disabled={page >= totalPages - 1}
          aria-label="다음"
        >
          <ChevronRight width={20} height={20} />
        </button>
      </div>
    </div>
  );
}
