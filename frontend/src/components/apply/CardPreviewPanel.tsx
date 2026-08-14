// Apply flow: sample preview panel shown alongside the application form.
import type { CardDesign } from "../../data/cards";

interface CardPreviewPanelProps {
  design: CardDesign;
  logoOverlay?: string;
  sealOverlay?: string;
}

const sampleImages: Record<CardDesign["cardType"], { src: string; side: string }[]> = {
  "honorary-korean": [
    { src: "/images/cards/width/kor-tiger-front.png", side: "앞면" },
    { src: "/images/cards/width/kor-tiger-back.jpg", side: "뒷면" },
  ],
  "honorary-citizen": [
    { src: "/images/cards/width/city-dragon2-front.jpg", side: "앞면" },
    { src: "/images/cards/width/city-dragon2-back.jpg", side: "뒷면" },
  ],
  student: [
    { src: "/images/cards/width/stu-pig-front.png", side: "앞면" },
    { src: "/images/cards/width/stu-pig-back.png", side: "뒷면" },
  ],
  visitor: [
    { src: "/images/cards/visit-sample.png", side: "견본" },
  ],
};

/** Right-hand sample preview shown alongside the application form. */
export function CardPreviewPanel({ design }: CardPreviewPanelProps) {
  const images = sampleImages[design.cardType];

  return (
    <aside className="apply-preview" aria-label="예시 카드">
      <p className="apply-preview__note-top">※ 특허출원에 의한 견본품</p>
      <div className={`apply-preview__row${images.length === 1 ? " apply-preview__row--single" : ""}`}>
        {images.map((image) => (
          <div className="apply-preview__card" key={image.src}>
            <img className="apply-preview__img" src={image.src} alt={`${design.name} 견본품 ${image.side}`} />
          </div>
        ))}
      </div>
      <p className="apply-preview__caption">
        이해를 돕기 위한 예시 이미지 입니다
        <br />
        실제 발급의 디자인과 구성은 변경될 수 있습니다
      </p>
    </aside>
  );
}
