// Card-design gallery for one category on the design page.
// Cards are supplied page-by-page by DesignPage.
import type { CardDesign } from "../../data/cards";
import { SampleCard } from "../brand/SampleCard";

interface CardCarouselProps {
  cards: CardDesign[];
  orientation: "landscape" | "portrait";
  /** Optional colour variant page. The design catalogue currently uses page 1. */
  page?: number;
  /** "student" = bespoke two-row mix: a wide landscape pair over a portrait quartet. */
  layout?: "default" | "student";
}

// Per-page tint over the cards. Placeholder until the real variant art lands.
const PAGE_TINTS: Record<number, string> = {
  1: "transparent",
  2: "var(--color-carousel-navy-ring)", // navy
  3: "var(--color-carousel-brown-ring)", // brown
  4: "var(--color-carousel-red-ring)", // red
  5: "var(--color-carousel-olive-ring)", // olive
};

/** A single card slot with its page-tint overlay. */
function TintedCard({
  design,
  variant,
  tint,
}: {
  design: CardDesign;
  variant: "front" | "reading";
  tint: string;
}) {
  return (
    <div className="carousel__frame">
      <SampleCard design={design} variant={variant} />
      <span className="carousel__tint" style={{ background: tint }} aria-hidden="true" />
    </div>
  );
}

export function CardCarousel({ cards, orientation, page = 1, layout = "default" }: CardCarouselProps) {
  const tint = PAGE_TINTS[page] ?? "transparent";

  // 학생증: card[0]은 가로 2장, card[1..]은 세로 4장에 사용한다.
  // Row 1 = the landscape pair (front + reading), spaced wide. Row 2 = the
  // portrait pair shown twice, filling four columns (2개씩 4장).
  if (layout === "student") {
    const wide = cards[0];
    const tallCards = cards.slice(1);
    return (
      <div className="carousel carousel--student">
        <div className="carousel__row carousel__row--wide">
          <TintedCard design={wide} variant="front" tint={tint} />
          <TintedCard design={wide} variant="reading" tint={tint} />
        </div>
        <div className="carousel__row carousel__row--narrow">
          {tallCards.flatMap((design) => ([
            <TintedCard key={`${design.id}-front`} design={design} variant="front" tint={tint} />,
            <TintedCard key={`${design.id}-reading`} design={design} variant="reading" tint={tint} />,
          ]))}
        </div>
      </div>
    );
  }

  return (
    <div className="carousel">
      <div className={`carousel__track carousel__track--${orientation}`}>
        {cards.map((card) => (
          <div className="carousel__slide" key={card.id}>
            <TintedCard design={card} variant="front" tint={tint} />
            <TintedCard design={card} variant="reading" tint={tint} />
          </div>
        ))}
      </div>
    </div>
  );
}
