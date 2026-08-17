import { useCallback, useEffect, useRef, useState } from "react";

interface NameResult {
  number: number;
  name: string;
  hanja: string;
  english: string;
  meaning: string;
}

/**
 * "서비스 핵심" — two traditional-window cards. The lattice shutters open on
 * hover/focus to reveal the label, echoing the mock-up's 창문 style.
 */
export function ServiceCoreSection() {
  return (
    <section className="service-core page-container">
      <p className="eyebrow">서비스 핵심</p>
      <h2 className="section-title service-core__title">
        한국 이름과 명예한국인증·명예시민증·학생증·방문증을 한 번에
      </h2>
      <p className="section-lead service-core__lead">
        정보 입력, 한국 이름 추천, 명예 한국인증·명예 시민증·학생증·방문증 미리보기까지 하나의 흐름으로 구성했습니다.
        <br />
        관광객이 한국 문화 체험을 개인화된 기념 콘텐츠로 남길 수 있습니다.
      </p>

      <div className="service-core__windows">
        <WindowCard label="각종 한국 이름 풀이" variant="names" />
        <WindowCard label="한국 이름의 작명 원리" variant="principles" />
      </div>
    </section>
  );
}

function WindowCard({ label, variant }: { label: string; variant: "names" | "principles" }) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [hasOpened, setHasOpened] = useState(false);
  const [nameResults, setNameResults] = useState<NameResult[]>([]);

  const loadNameResults = useCallback(() => {
    if (variant !== "names" || nameResults.length > 0) return;
    void import("../../data/nameResults.json").then((module) => {
      setNameResults(module.default as NameResult[]);
    });
  }, [nameResults.length, variant]);

  useEffect(() => {
    if (variant !== "names") return;
    const node = buttonRef.current;
    if (!node || !("IntersectionObserver" in window)) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return;
        loadNameResults();
        observer.disconnect();
      },
      { rootMargin: "240px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [loadNameResults, variant]);

  const open = () => {
    loadNameResults();
    setIsOpen(true);
  };
  const toggle = () => {
    loadNameResults();
    setIsOpen((value) => {
      if (!value) return true;
      setHasOpened(false);
      return false;
    });
  };

  return (
    <button
      ref={buttonRef}
      className={`window window--${variant}${isOpen ? " is-open" : ""}${hasOpened ? " has-opened" : ""}`}
      aria-label={`${label} — ${hasOpened ? "열림" : "열어보기"}`}
      onMouseEnter={open}
      onFocus={open}
      onClick={toggle}
    >
      {variant === "names" && (
        <span className="window__name-scroll" aria-hidden="true">
          <span className="window__name-track">
            {[0, 1].map((copy) => (
              <span className="window__name-group" key={copy}>
                {nameResults.map((result) => (
                  <span className="window__name-row" key={`${copy}-${result.number}`}>
                    <b>{result.name}</b>
                    <i>{result.hanja}</i>
                    <em>{result.english}</em>
                    <span>{result.meaning}</span>
                  </span>
                ))}
              </span>
            ))}
          </span>
        </span>
      )}
      <span className="window__label">{label}</span>
      <span
        className="window__shutter window__shutter--left"
        aria-hidden="true"
        onTransitionEnd={(event) => {
          if (isOpen && event.propertyName === "transform") setHasOpened(true);
        }}
      />
      <span className="window__shutter window__shutter--right" aria-hidden="true" />
    </button>
  );
}
