import type { CardDesign } from "../../data/cards";
import { ImagePlaceholder } from "../ui/ImagePlaceholder";
import "./SampleCard.css";

interface SampleCardProps {
  design: CardDesign;
  variant?: "front" | "reading";
  /** Uploaded logo/seal previews overlaid on the sample card (step 3). */
  logoOverlay?: string;
  sealOverlay?: string;
}

/**
 * Card slots are IMAGES the client delivers later (front artwork and the
 * "한국이름풀이" reading card). Until the files arrive we show a black box that
 * preserves the exact slot size/orientation — never fabricated card artwork.
 */
export function SampleCard({ design, variant = "front", logoOverlay, sealOverlay }: SampleCardProps) {
  if (variant === "reading") {
    if (design.back) {
      return <img className="sample-card sample-card--portrait" src={design.back} alt={`${design.name} 한국이름풀이`} />;
    }
    return (
      <div className="sample-card sample-card--reading">
        <ImagePlaceholder label="이름풀이 이미지" />
      </div>
    );
  }

  if (design.front) {
    return <img className={`sample-card sample-card--${design.orientation}`} src={design.front} alt={design.name} />;
  }

  return (
    <div className={`sample-card sample-card--${design.orientation}`}>
      <ImagePlaceholder label="카드 이미지">
        {(logoOverlay || sealOverlay) && (
          <span className="sample-card__overlays">
            {logoOverlay && <img className="sample-card__ov sample-card__ov--logo" src={logoOverlay} alt="" />}
            {sealOverlay && <img className="sample-card__ov sample-card__ov--seal" src={sealOverlay} alt="" />}
          </span>
        )}
      </ImagePlaceholder>
    </div>
  );
}
