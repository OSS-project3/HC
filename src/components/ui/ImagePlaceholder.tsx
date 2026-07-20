import clsx from "clsx";
import type { CSSProperties } from "react";
import "./ImagePlaceholder.css";

interface ImagePlaceholderProps {
  /** What image belongs here (shown inside the black box). */
  label?: string;
  className?: string;
  style?: CSSProperties;
  /** Optional overlay children rendered on top (e.g. uploaded logo/seal). */
  children?: React.ReactNode;
}

/**
 * Black box that marks a slot where a REAL image file is required but has not
 * been delivered yet. We intentionally do NOT draw substitute artwork — only
 * the content and layout are finalised; images are dropped in later.
 */
export function ImagePlaceholder({ label, className, style, children }: ImagePlaceholderProps) {
  return (
    <div
      className={clsx("img-ph", className)}
      style={style}
      role="img"
      aria-label={label ? `${label} — 이미지 추가 필요` : "이미지 추가 필요"}
    >
      {label && <span className="img-ph__label">{label}</span>}
      <span className="img-ph__tag">추가 필요</span>
      {children}
    </div>
  );
}
