// Home '한글은 문화다' section: cultural-experience merchandise.
import { useState } from "react";
import { merchFeatures } from "../../data/merchandise";
import { useLanguage } from "../../features/i18n/LanguageContext";
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
  const { t } = useLanguage();
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
          <img className="merch__gallery-img" src="/images/merchandise/cluster.webp" alt={t("문화 체험 기념품")} loading="lazy" decoding="async" />
        </div>

        <div className="merch__copy">
          <p className="eyebrow">{t("한글은 문화다")}</p>
          <h2 className="section-title">{t("문화 체험에서")}<br />{t("공유 가능한 기념품으로")}</h2>
          <p className="section-lead merch__desc">
            {t("생성된 한국 이름 작명 명예한국인증·명예시민증·학생증·방문증은 다운로드와 SNS 공유에 적합한 비주얼 콘텐츠입니다. 향후 실물 카드 제작, 프리미엄 디자인, 유료 관광 상품으로 확장할 수 있습니다.")}
          </p>
          <ul className="merch__features">
            {merchFeatures.map((feature) => (
              <li key={feature}>
                <CheckCircleIcon width={20} height={20} />
                <span>{t(feature)}</span>
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
          label={t("외국인 방문객 한국이름 작명 대상 보기")}
          summary={<>{t("외국인 방문객")}<br />{t("한국이름 작명 대상")}</>}
        >
          <ul className="info-card__audience">
            {audienceItems.map((item) => <li key={item}>{t(item)}</li>)}
          </ul>
        </InfoCard>

        <InfoCard
          index={1}
          active={activeCards.has(1)}
          onSelect={selectCard}
          label={t("디지털 카드 결과물 보기")}
          summary={<>{t("디지털")}<br />{t("명예한국인증 · 명예시민증")}<br />{t("학생증 · 방문증")}<br />{t("결과물")}</>}
        >
          <img className="info-card__result-image" src="/images/common/sample-card.webp" alt={t("디지털 카드 결과물")} loading="lazy" decoding="async" />
        </InfoCard>

        <InfoCard
          index={2}
          active={activeCards.has(2)}
          onSelect={selectCard}
          label={t("실물 카드 결과물 보기")}
          summary={<>{t("실물")}<br />{t("명예한국인증 · 명예시민증")}<br />{t("학생증 · 방문증")}<br />{t("결과물")}</>}
        >
          <img className="info-card__result-image" src="/images/merchandise/sample2.webp" alt={t("실물 카드 결과물")} loading="lazy" decoding="async" />
        </InfoCard>
      </div>
    </section>
  );
}



