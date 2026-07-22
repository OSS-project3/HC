// Card-design gallery for one category on the design page.
// All cards are shown at once; the `page` prop (1–5) only changes the colour
// overlay tinting the cards — page 1 is the plain art.
import type { CardDesign } from "../../data/cards";
import { SampleCard } from "../brand/SampleCard";

interface CardCarouselProps {
  cards: CardDesign[];
  orientation: "landscape" | "portrait";
  /** Active page (1–5) — drives the colour overlay. Page 1 = no tint. */
  page?: number;
  /** "student" = bespoke two-row mix: a wide landscape pair over a portrait quartet. */
  layout?: "default" | "student";
}

// Per-page tint over the cards. Placeholder until the real variant art lands.
const PAGE_TINTS: Record<number, string> = {
  1: "transparent",
  2: "rgba(38, 61, 91, 0.34)", // navy
  3: "rgba(138, 105, 69, 0.34)", // brown
  4: "rgba(178, 59, 74, 0.32)", // red
  5: "rgba(80, 79, 51, 0.34)", // olive
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

  // 학생증: card[0] is the 가로(landscape) design, card[1] the 세로(portrait) design.
  // Row 1 = the landscape pair (front + reading), spaced wide. Row 2 = the
  // portrait pair shown twice, filling four columns (2개씩 4장).
  if (layout === "student") {
    const wide = cards[0];
    const tall = cards[1];
    return (
      <div className="carousel carousel--student">
        <div className="carousel__row carousel__row--wide">
          <TintedCard design={wide} variant="front" tint={tint} />
          <TintedCard design={wide} variant="reading" tint={tint} />
        </div>
        <div className="carousel__row carousel__row--narrow">
          <TintedCard design={tall} variant="front" tint={tint} />
          <TintedCard design={tall} variant="reading" tint={tint} />
          <TintedCard design={tall} variant="front" tint={tint} />
          <TintedCard design={tall} variant="reading" tint={tint} />
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
