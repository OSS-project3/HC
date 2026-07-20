import type { ZodiacSign } from "../../data/zodiac";
import { ImagePlaceholder } from "../ui/ImagePlaceholder";

interface ZodiacIconProps {
  sign: ZodiacSign;
  size?: number;
  highlighted?: boolean;
}

/**
 * Zodiac characters are paper-cut ARTWORK the client delivers later. Each slot
 * is a black-box placeholder (never fabricated art). The highlighted state
 * still enlarges + rings the active slot so the rotation behaviour is visible.
 */
export function ZodiacIcon({ sign, size = 88, highlighted = false }: ZodiacIconProps) {
  if (sign.image) {
    return (
      <img src={sign.image} alt={`${sign.nameKo} (${sign.nameEn})`} style={{ width: size, height: size, objectFit: "contain" }} />
    );
  }

  return (
    <span
      style={{
        display: "inline-block",
        width: size,
        height: size,
        transition: "transform 180ms ease, box-shadow 180ms ease",
        transform: highlighted ? "scale(1.08)" : "scale(1)",
        boxShadow: highlighted ? "0 0 0 4px #fff, 0 6px 16px rgb(80 79 51 / 22%)" : "none",
        borderRadius: 6,
      }}
    >
      <ImagePlaceholder label={sign.nameKo} />
    </span>
  );
}
