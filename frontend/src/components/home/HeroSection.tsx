import { useEffect, useMemo, useRef, useState } from "react";
import { companyInfo } from "../../config/company";
import { zodiacSigns } from "../../data/zodiac";
import { ZodiacIcon } from "../brand/ZodiacIcon";
import { Trigram } from "./Trigram";

const HERO_CHANGE_DELAY = 4000;

const HERO_LANDSCAPE = [
  ["kor-tiger-front.jpg", "kor-tiger-back.jpg"],
  ["city-dragon2-front.jpg", "city-dragon2-back.jpg"],
  ["stu-pig-front.png", "stu-pig-back.png"],
] as const;

const HERO_PORTRAIT = [
  ["student-horse-front.jpg", "student-horse-back.png"],
  ["visit-tiger-front.jpg", "visit-tiger-back.jpg"],
] as const;

interface FlipCardProps {
  files: readonly [string, string];
  orientation: "landscape" | "portrait";
  flipped: boolean;
  onFlip: () => void;
}

function FlipCard({ files, orientation, flipped, onFlip }: FlipCardProps) {
  const folder = orientation === "landscape" ? "width" : "length";
  const [visibleFlipped, setVisibleFlipped] = useState(flipped);
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
      className={`hero__card-float hero__card-float--${orientation}`}
      onClick={() => turnPhase === "idle" && onFlip()}
      aria-label={`${orientation === "landscape" ? "가로" : "세로"} 카드 ${flipped ? "앞면" : "뒷면"} 보기`}
      aria-pressed={flipped}
      aria-busy={turnPhase !== "idle"}
    >
      <span className={`hero__card-flip hero__card-flip--${turnPhase}`}>
        <img className="hero__card-face" src={`/images/cards/${folder}/${files[visibleFlipped ? 1 : 0]}`} alt={`${orientation === "landscape" ? "가로" : "세로"} 카드 ${visibleFlipped ? "뒷면" : "앞면"}`} />
      </span>
    </button>
  );
}

interface HeroSectionProps {
  zodiacIndex: number;
  onZodiacSelect: (index: number) => void;
}

export function HeroSection({ zodiacIndex, onZodiacSelect }: HeroSectionProps) {
  const [landscapeIndex, setLandscapeIndex] = useState(0);
  const [portraitIndex, setPortraitIndex] = useState(0);
  const [landscapeFlipped, setLandscapeFlipped] = useState(false);
  const [portraitFlipped, setPortraitFlipped] = useState(false);
  const paused = useRef(false);

  useEffect(() => {
    const id = window.setInterval(() => {
      if (paused.current) return;
      setLandscapeIndex((index) => (index + 1) % HERO_LANDSCAPE.length);
      setPortraitIndex((index) => (index + 1) % HERO_PORTRAIT.length);
      setLandscapeFlipped(false);
      setPortraitFlipped(false);
    }, HERO_CHANGE_DELAY);
    return () => window.clearInterval(id);
  }, []);

  const pauseHandlers = useMemo(() => ({
    onMouseEnter: () => { paused.current = true; },
    onMouseLeave: () => { paused.current = false; },
    onFocus: () => { paused.current = true; },
    onBlur: () => { paused.current = false; },
  }), []);

  return (
    <section className="hero">
      <div className="hero__bg" aria-hidden="true">
        <Trigram name="건" className="hero__tri hero__tri--tl" />
        <Trigram name="감" className="hero__tri hero__tri--tr" />
      </div>
      <div className="hero__inner page-container">
        <div className="hero__sejong" aria-hidden="true" />
        <div className="hero__copy">
          <p className="hero__eyebrow">한글 오행으로 만나는</p>
          <h1 className="hero__title">명예한국인증·명예시민증<br />학생증·방문증</h1>
          <p className="hero__lead">외국인을 위한 한국 이름 작명 발급 시스템</p>
          <p className="hero__patent">특허 출원 번호 {companyInfo.patentNumber}</p>
        </div>
        <div className="hero__stage">
          <div className="hero__cards" aria-label="카드 예시" {...pauseHandlers}>
            <FlipCard key={`landscape-${landscapeIndex}`} files={HERO_LANDSCAPE[landscapeIndex]} orientation="landscape" flipped={landscapeFlipped} onFlip={() => setLandscapeFlipped((value) => !value)} />
            <FlipCard key={`portrait-${portraitIndex}`} files={HERO_PORTRAIT[portraitIndex]} orientation="portrait" flipped={portraitFlipped} onFlip={() => setPortraitFlipped((value) => !value)} />
          </div>
        </div>
      </div>
      <div className="hero__zodiac">
        <div className="hero__zodiac-inner">
          <Trigram name="리" className="hero__tri hero__ztri hero__ztri--l" />
          <Trigram name="곤" className="hero__tri hero__ztri hero__ztri--r" />
          <p className="hero__zodiac-label hero__zodiac-label--top">KOREAN ZODIAC SIGNS</p>
          <ul className="hero__zodiac-row">
            {zodiacSigns.map((sign, index) => (
              <li key={sign.id} className="hero__zodiac-item">
                <button className="hero__zodiac-btn" onClick={() => onZodiacSelect(index)} aria-pressed={index === zodiacIndex} aria-label={`${sign.nameKo} (${sign.nameEn})`}>
                  <ZodiacIcon sign={sign} size={72} highlighted={index === zodiacIndex} />
                </button>
              </li>
            ))}
          </ul>
          <p className="hero__zodiac-label hero__zodiac-label--bottom">KOREAN ZODIAC SIGNS</p>
        </div>
      </div>
    </section>
  );
}
