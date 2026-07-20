import { useEffect, useMemo, useRef, useState } from "react";
import { companyInfo } from "../../config/company";
import { zodiacSigns } from "../../data/zodiac";
import { honoraryKoreanCards, visitorCards, studentCards, honoraryCitizenCards } from "../../data/cards";
import { ZodiacIcon } from "../brand/ZodiacIcon";
import { SampleCard } from "../brand/SampleCard";

const AUTOPLAY_DELAY = 5000;
const ZODIAC_DELAY = 3200;

// The hero shows exactly TWO cards: one landscape slot and one portrait slot.
// Each slot cycles through its own pool of designs (landscape advances on every
// tick, portrait on every second tick — "가로형 두 번 바뀔 때 세로형 한 번").
const landscapePool = [...honoraryKoreanCards, ...honoraryCitizenCards];
const portraitPool = [...visitorCards, ...studentCards];

export function HeroSection() {
  const [tick, setTick] = useState(0);
  const [zodiacIndex, setZodiacIndex] = useState(0);
  const paused = useRef(false);

  useEffect(() => {
    const id = window.setInterval(() => {
      if (!paused.current) setTick((t) => t + 1);
    }, AUTOPLAY_DELAY);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    const id = window.setInterval(() => {
      if (!paused.current) setZodiacIndex((i) => (i + 1) % zodiacSigns.length);
    }, ZODIAC_DELAY);
    return () => window.clearInterval(id);
  }, []);

  const landscape = landscapePool[tick % landscapePool.length];
  const portrait = portraitPool[Math.floor(tick / 2) % portraitPool.length];

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

        <div className="hero__stage" {...pauseHandlers}>
          <div className="hero__cards" aria-label="카드 예시">
            <div className="hero__card hero__card--landscape" key={`l-${landscape.id}`}>
              <SampleCard design={landscape} />
            </div>
            <div className="hero__card hero__card--portrait" key={`p-${portrait.id}`}>
              <SampleCard design={portrait} />
            </div>
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
