// Apply flow: sample preview panel shown alongside the application form.
import type { CardDesign } from "../../data/cards";

interface CardPreviewPanelProps {
  design: CardDesign;
  logoOverlay?: string;
  sealOverlay?: string;
}

// Fixed 방문증 sample images (front / back) shown under the 견본품 note.
const VISITCARD_FRONT = "/images/cards/visitcard-front.png";
const VISITCARD_BACK = "/images/cards/visitcard-back.png";

/** Right-hand sample preview shown alongside the application form. */
export function CardPreviewPanel(_props: CardPreviewPanelProps) {
  return (
    <aside className="apply-preview" aria-label="예시 카드">
      <p className="apply-preview__note-top">※ 특허출원에 의한 견본품</p>
      <div className="apply-preview__row">
        <div className="apply-preview__card">
          <img className="apply-preview__img" src={VISITCARD_FRONT} alt="방문증 예시 앞면" />
        </div>
        <div className="apply-preview__card">
          <img className="apply-preview__img" src={VISITCARD_BACK} alt="방문증 예시 뒷면" />
        </div>
      </div>
      <p className="apply-preview__caption">
        이해를 돕기 위한 예시 이미지 입니다
        <br />
        실제 발급의 디자인과 구성은 변경될 수 있습니다
      </p>
    </aside>
  );
}
