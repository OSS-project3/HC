// Reusable mobile-card viewer: shows the front, flips to the back on click.
import { useState } from "react";
import "./FlipCard.css";

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
  const [flipped, setFlipped] = useState(false);

  return (
    <div className="flipcard-wrap">
      <button
        type="button"
        className={`flipcard${flipped ? " flipcard--flipped" : ""}`}
        onClick={() => setFlipped((value) => !value)}
        aria-label={flipped ? "카드 앞면 보기" : "카드 뒷면 보기"}
        aria-pressed={flipped}
      >
        <span className="flipcard__inner">
          <span className="flipcard__face flipcard__face--front">
            <img src={frontUrl} alt={frontAlt} />
          </span>
          <span className="flipcard__face flipcard__face--back">
            <img src={backUrl} alt={backAlt} />
          </span>
        </span>
      </button>
    </div>
  );
}
