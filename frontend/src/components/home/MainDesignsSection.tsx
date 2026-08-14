import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { zodiacSigns } from "../../data/zodiac";

const DESIGN_CHANGE_DELAY = 4000;

const LANDSCAPE_CARDS = [
  ["kor-mouse-front.png", "kor-mouse-back.jpg"],
  ["kor-cow-front.jpg", "kor-cow-back.jpg"],
  ["kor-tiger-front.png", "kor-tiger-back.jpg"],
  ["kor-rabbit-front.jpg", "kor-rabbit-back.jpg"],
  ["city-dragon2-front.jpg", "city-dragon2-back.jpg"],
  ["city-snake-front.jpg", "city-snake-back.jpg"],
  ["city-horse-front.jpg", "city-horse-back.jpg"],
  ["city-sheep-front.jpg", "city-sheep-back.jpg"],
  ["stu-monkey-front.png", "stu-monkey-back.png"],
  ["stu-chicken-front.png", "stu-chicken-back.png"],
  ["stu-dog-front.png", "stu-dog-back.png"],
  ["stu-pig-front.png", "stu-pig-back.png"],
] as const;

const PORTRAIT_CARDS = [
  ["visit-mouse-front.png", "visit-mouse-back.jpg"],
  ["visit-cow-front.jpg", "visit-cow-back.jpg"],
  ["visit-tiger-front.jpg", "visit-tiger-back.jpg"],
  ["visit-rabbit-front.jpg", "visit-rabbit-back.jpg"],
  ["visit-dragon-front.jpg", "visit-dragron-back.jpg"],
  ["visit-snake-front.jpg", "visit-snake-back.jpg"],
  ["student-horse-front.png", "student-horse-back.png"],
  ["student-sheep-front.png", "student-sheep-back.png"],
  ["student-monkey-front.png", "student-monkey-back.png"],
  ["student-chicken-front.png", "student-chicken-back.png"],
  ["student-dog-front.png", "student-dog-back.png"],
  ["student-pig-front.png", "student-pig-back.png"],
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
        <img className="design-group__img" src={`/images/cards/${folder}/${files[visibleFlipped ? 1 : 0]}`} alt={`${animalName} 카드 ${visibleFlipped ? "뒷면" : "앞면"}`} />
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
