import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { zodiacSigns } from "../../data/zodiac";

const DESIGN_CHANGE_DELAY = 7000;

const LANDSCAPE_CARDS = [
  ["kor-mouse-front.webp", "kor-mouse-back.webp"],
  ["kor-cow-front.webp", "kor-cow-back.webp"],
  ["kor-tiger-front.webp", "kor-tiger-back.webp"],
  ["kor-rabbit-front.webp", "kor-rabbit-back.webp"],
  ["city-dragon2-front.webp", "city-dragon2-back.webp"],
  ["city-snake-front.webp", "city-snake-back.webp"],
  ["city-horse-front.webp", "city-horse-back.webp"],
  ["city-sheep-front.webp", "city-sheep-back.webp"],
  ["stu-monkey-front.webp", "stu-monkey-back.webp"],
  ["stu-chicken-front.webp", "stu-chicken-back.webp"],
  ["stu-dog-front.webp", "stu-dog-back.webp"],
  ["stu-pig-front.webp", "stu-pig-back.webp"],
] as const;

const PORTRAIT_CARDS = [
  ["visit-mouse-front.webp", "visit-mouse-back.webp"],
  ["visit-cow-front.webp", "visit-cow-back.webp"],
  ["visit-tiger-front.webp", "visit-tiger-back.webp"],
  ["visit-rabbit-front.webp", "visit-rabbit-back.webp"],
  ["visit-dragon-front.webp", "visit-dragron-back.webp"],
  ["visit-snake-front.webp", "visit-snake-back.webp"],
  ["student-horse-front.webp", "student-horse-back.webp"],
  ["student-sheep-front.webp", "student-sheep-back.webp"],
  ["student-monkey-front.webp", "student-monkey-back.webp"],
  ["student-chicken-front.webp", "student-chicken-back.webp"],
  ["student-dog-front.webp", "student-dog-back.webp"],
  ["student-pig-front.webp", "student-pig-back.webp"],
] as const;

interface DesignPreviewCardProps {
  files: readonly [string, string];
  folder: "width" | "length";
  animalName: string;
}

function DesignPreviewCard({ files, folder, animalName }: DesignPreviewCardProps) {
  const [flipped, setFlipped] = useState(false);
  const [visibleFlipped, setVisibleFlipped] = useState(false);
  const [turnPhase, setTurnPhase] = useState<"idle" | "out" | "swap">("idle");

  useEffect(() => {
    if (flipped === visibleFlipped) return;
    setTurnPhase("out");
    const timer = window.setTimeout(() => {
      setVisibleFlipped(flipped);
      setTurnPhase("swap");
      window.requestAnimationFrame(() => window.requestAnimationFrame(() => setTurnPhase("idle")));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [flipped, visibleFlipped]);

  return (
    <button
      type="button"
      className="design-group__card-button"
      onClick={() => turnPhase === "idle" && setFlipped((value) => !value)}
      aria-label={`${animalName} 카드 ${flipped ? "앞면" : "뒷면"} 보기`}
      aria-pressed={flipped}
      aria-busy={turnPhase !== "idle"}
    >
      <span className={`design-group__flip design-group__flip--${turnPhase}`}>
        <img className="design-group__img" src={`/images/cards/${folder}/${files[visibleFlipped ? 1 : 0]}`} alt={`${animalName} 카드 ${visibleFlipped ? "뒷면" : "앞면"}`} loading="lazy" decoding="async" />
      </span>
    </button>
  );
}

interface MainDesignsSectionProps {
  zodiacIndex: number;
  onZodiacChange: (index: number) => void;
}

export function MainDesignsSection({ zodiacIndex, onZodiacChange }: MainDesignsSectionProps) {
  useEffect(() => {
    const id = window.setInterval(() => {
      onZodiacChange((zodiacIndex + 1) % zodiacSigns.length);
    }, DESIGN_CHANGE_DELAY);
    return () => window.clearInterval(id);
  }, [onZodiacChange, zodiacIndex]);

  useEffect(() => {
    const nextIndex = (zodiacIndex + 1) % zodiacSigns.length;
    [...LANDSCAPE_CARDS[nextIndex], ...PORTRAIT_CARDS[nextIndex]].forEach((file, fileIndex) => {
      const image = new Image();
      image.src = `/images/cards/${fileIndex < 2 ? "width" : "length"}/${file}`;
    });
  }, [zodiacIndex]);

  const animalName = zodiacSigns[zodiacIndex].nameKo;

  return (
    <section className="main-designs page-container">
      <div className="main-designs__head">
        <h2 className="main-designs__title">주요 디자인</h2>
        <Link className="main-designs__all" to="/design">전체 보기 <span className="main-designs__arrow" aria-hidden="true">-&gt;</span></Link>
      </div>
      <div className="main-designs__grid">
        <div className="design-group">
          <p className="design-group__label">명예한국인증 · 명예시민증 · 학생증</p>
          <div className="design-group__card design-group__card--landscape" key={`landscape-${zodiacIndex}`}>
            <DesignPreviewCard files={LANDSCAPE_CARDS[zodiacIndex]} folder="width" animalName={animalName} />
          </div>
          <div className="design-group__buttons">
            <Link className="design-chip" to="/apply?designId=honorary-korean-01">명예한국인증 신청</Link>
            <Link className="design-chip" to="/apply?designId=honorary-citizen-01">명예시민증 신청</Link>
            <Link className="design-chip" to="/apply?designId=student-01">학생증 신청</Link>
          </div>
        </div>
        <div className="main-designs__divider" aria-hidden="true" />
        <div className="design-group">
          <p className="design-group__label">방문증 · 학생증</p>
          <div className="design-group__card design-group__card--portrait" key={`portrait-${zodiacIndex}`}>
            <DesignPreviewCard files={PORTRAIT_CARDS[zodiacIndex]} folder="length" animalName={animalName} />
          </div>
          <div className="design-group__buttons">
            <Link className="design-chip" to="/apply?designId=visitor-01">방문증 신청</Link>
            <Link className="design-chip" to="/apply?designId=student-01">학생증 신청</Link>
          </div>
        </div>
      </div>
    </section>
  );
}
