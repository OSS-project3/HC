import { useEffect, useMemo, useRef, useState } from "react";
import { companyInfo } from "../../config/company";
import { zodiacSigns } from "../../data/zodiac";
import { ZodiacIcon } from "../brand/ZodiacIcon";
import { Trigram } from "./Trigram";

const ZODIAC_DELAY = 3200;

// Temporary hero card images (provided in public/images/cards/).
const HERO_LANDSCAPE = "/images/cards/main%20sec1%20ex1.png";
const HERO_PORTRAIT = "/images/cards/main%20sec1%20ex2.png";

export function HeroSection() {
  const [zodiacIndex, setZodiacIndex] = useState(0);
  const paused = useRef(false);

  useEffect(() => {
    const id = window.setInterval(() => {
      if (!paused.current) setZodiacIndex((i) => (i + 1) % zodiacSigns.length);
    }, ZODIAC_DELAY);
    return () => window.clearInterval(id);
  }, []);

  const pauseHandlers = useMemo(
    () => ({
      onMouseEnter: () => (paused.current = true),
      onMouseLeave: () => (paused.current = false),
      onFocus: () => (paused.current = true),
      onBlur: () => (paused.current = false),
    }),
    [],
  );

  return (
    <section className="hero">
      {/* Low-priority background: 건곤감리 trigrams flush to the corners
          (the top two overlap the header). */}
      <div className="hero__bg" aria-hidden="true">
        <Trigram name="건" className="hero__tri hero__tri--tl" />
        <Trigram name="감" className="hero__tri hero__tri--tr" />
        <Trigram name="리" className="hero__tri hero__tri--bl" />
        <Trigram name="곤" className="hero__tri hero__tri--br" />
      </div>

      <div className="hero__inner page-container">
        <div className="hero__copy">
          <p className="hero__eyebrow">한글 오행으로 만나는</p>
          <h1 className="hero__title">
            명예한국인증·명예시민증
            <br />
            학생증·방문증
          </h1>
          <p className="hero__lead">외국인을 위한 한국 이름 작명 발급 시스템</p>
          <p className="hero__patent">특허 출원 번호 {companyInfo.patentNumber}</p>
        </div>

        <div className="hero__stage">
          <div className="hero__cards" aria-label="카드 예시">
            <img className="hero__card hero__card--landscape" src={HERO_LANDSCAPE} alt="명예한국인증 카드 예시" />
            <img className="hero__card hero__card--portrait" src={HERO_PORTRAIT} alt="방문증 카드 예시" />
          </div>
        </div>
      </div>

      <div className="hero__zodiac" {...pauseHandlers}>
        <p className="hero__zodiac-label">KOREAN ZODIAC SIGNS</p>
        <ul className="hero__zodiac-row">
          {zodiacSigns.map((sign, i) => (
            <li key={sign.id}>
              <button
                className="hero__zodiac-btn"
                onClick={() => setZodiacIndex(i)}
                aria-pressed={i === zodiacIndex}
                aria-label={`${sign.nameKo} (${sign.nameEn})`}
              >
                <ZodiacIcon sign={sign} size={72} highlighted={i === zodiacIndex} />
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
