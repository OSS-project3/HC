// Home '한글은 문화다' section: cultural-experience merchandise.
import { useState } from "react";
import { merchFeatures } from "../../data/merchandise";
import { CheckCircleIcon } from "../ui/icons";

const audienceItems = [
  "유학생",
  "각종 체육·관광 임원·선수단",
  "외국인 근로자",
  "외국인 요양 보호사",
  "MICE 산업 관련자",
  "지자체 협력 관련자",
  "관광 협력 관련자",
  "각종 비즈니스 관련자",
  "엔터테인먼트 기획사 회원증 및 팬 인증서",
];

interface InfoCardProps {
  index: number;
  active: boolean;
  onSelect: (index: number) => void;
  summary: React.ReactNode;
  children: React.ReactNode;
  label: string;
}

function InfoCard({ index, active, onSelect, summary, children, label }: InfoCardProps) {
  return (
    <button
      type="button"
      className={`info-card${index > 0 ? " info-card--image-result" : ""}${active ? " is-active" : ""}`}
      onClick={() => onSelect(index)}
      aria-expanded={active}
      aria-label={label}
    >
      <span className="info-card__summary">{summary}</span>
      <span className="info-card__result" aria-hidden={!active}>{children}</span>
    </button>
  );
}

export function MerchandiseSection() {
  const [activeCards, setActiveCards] = useState<Set<number>>(() => new Set());
  const selectCard = (index: number) => {
    setActiveCards((current) => {
      const next = new Set(current);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  };

  return (
    <section className="merch page-container">
      <div className="merch__top">
        <div className="merch__gallery">
          <img className="merch__gallery-img" src="/images/merchandise/cluster.png" alt="문화 체험 기념품" />
        </div>

        <div className="merch__copy">
          <p className="eyebrow">한글은 문화다</p>
          <h2 className="section-title">문화 체험에서<br />공유 가능한 기념품으로</h2>
          <p className="section-lead merch__desc">
            생성된 한국 이름 작명 명예한국인증·명예시민증·학생증·방문증은 다운로드와 SNS 공유에 적합한 비주얼
            콘텐츠입니다. 향후 실물 카드 제작, 프리미엄 디자인, 유료 관광 상품으로 확장할 수 있습니다.
          </p>
          <ul className="merch__features">
            {merchFeatures.map((feature) => (
              <li key={feature}>
                <CheckCircleIcon width={20} height={20} />
                <span>{feature}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="merch__cards">
        <InfoCard
          index={0}
          active={activeCards.has(0)}
          onSelect={selectCard}
          label="외국인 방문객 한국이름 작명 대상 보기"
          summary={<>외국인 방문객<br />한국이름 작명 대상</>}
        >
          <ul className="info-card__audience">
            {audienceItems.map((item) => <li key={item}>{item}</li>)}
          </ul>
        </InfoCard>

        <InfoCard
          index={1}
          active={activeCards.has(1)}
          onSelect={selectCard}
          label="디지털 카드 결과물 보기"
          summary={<>디지털<br />명예한국인증 · 명예시민증<br />학생증 · 방문증<br />결과물</>}
        >
          <img className="info-card__result-image" src="/images/merchandise/sample.jpg" alt="디지털 카드 결과물" />
        </InfoCard>

        <InfoCard
          index={2}
          active={activeCards.has(2)}
          onSelect={selectCard}
          label="실물 카드 결과물 보기"
          summary={<>실물<br />명예한국인증 · 명예시민증<br />학생증 · 방문증<br />결과물</>}
        >
          <img className="info-card__result-image" src="/images/merchandise/sample2.png" alt="실물 카드 결과물" />
        </InfoCard>
      </div>
    </section>
  );
}



