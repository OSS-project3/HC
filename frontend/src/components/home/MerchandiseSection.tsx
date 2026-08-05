// Home '한글은 문화다' section: cultural-experience merchandise.
import { merchFeatures } from "../../data/merchandise";
import { CheckCircleIcon } from "../ui/icons";
import { ImagePlaceholder } from "../ui/ImagePlaceholder";

/**
 * "한글은 문화다" — merchandise showcase + feature checklist + 3 info cards.
 * Product photos and the 2nd/3rd cards are delivered images → black boxes.
 * The 1st card is text (per the client) and the checklist is real content.
 */
export function MerchandiseSection() {
  return (
    <section className="merch page-container">
      <div className="merch__top">
        <div className="merch__gallery">
          <img className="merch__gallery-img" src="/images/merchandise/cluster.png" alt="문화 체험 기념품" />
        </div>

        <div className="merch__copy">
          <p className="eyebrow">한글은 문화다</p>
          <h2 className="section-title">
            문화 체험에서
            <br />
            공유 가능한 기념품으로
          </h2>
          <p className="section-lead merch__desc">
            생성된 한국 이름 작명 명예한국인증·명예시민증·학생증·방문증은 다운로드와 SNS 공유에 적합한 비주얼
            콘텐츠입니다. 향후 실물 카드 제작, 프리미엄 디자인, 유료 관광 상품으로 확장할 수 있습니다.
          </p>
          <ul className="merch__features">
            {merchFeatures.map((f) => (
              <li key={f}>
                <CheckCircleIcon width={20} height={20} />
                <span>{f}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="merch__cards">
        {/* 1st card: text. */}
        <article className="info-card">
          <p className="info-card__text">
            개인화된 한국 이름과 각종 증(證)을 하나의 흐름으로 제작하고, 언제든지 다시 확인할 수 있습니다.
          </p>
        </article>
        <span className="merch__dot" aria-hidden="true" />
        {/* 2nd & 3rd cards: images. */}
        <article className="info-card info-card--image">
          <ImagePlaceholder label="카드 이미지" />
        </article>
        <span className="merch__dot" aria-hidden="true" />
        <article className="info-card info-card--image">
          <ImagePlaceholder label="카드 이미지" />
        </article>
      </div>
    </section>
  );
}
