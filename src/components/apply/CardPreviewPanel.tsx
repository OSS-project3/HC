// Apply flow: live preview panel for the selected card design.
import type { CardDesign } from "../../data/cards";
import { SampleCard } from "../brand/SampleCard";

interface CardPreviewPanelProps {
  design: CardDesign;
  logoOverlay?: string;
  sealOverlay?: string;
}

/** Right-hand sample preview shown alongside the application form. */
export function CardPreviewPanel({ design, logoOverlay, sealOverlay }: CardPreviewPanelProps) {
  return (
    <aside className="apply-preview" aria-label="예시 카드">
      <p className="apply-preview__note-top">※ 특허출원에 의한 견본품</p>
      <div className="apply-preview__card">
        <SampleCard design={design} variant="front" logoOverlay={logoOverlay} sealOverlay={sealOverlay} />
      </div>
      <div className="apply-preview__card">
        <SampleCard design={design} variant="reading" />
      </div>
      <p className="apply-preview__caption">
        이해를 돕기 위한 예시 이미지 입니다
        <br />
        실제 발급의 디자인과 구성은 변경될 수 있습니다
      </p>
    </aside>
  );
}
