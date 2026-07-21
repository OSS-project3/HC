// Brand logo lockup: symbol mark, wordmark, 이팝나무 tree, and optional home tagline.
import { Link } from "react-router-dom";
import { companyInfo } from "../../config/company";
import "./Logo.css";

interface LogoProps {
  /** Show the "이팝나무" tree slot beside the wordmark (header uses it). */
  withTree?: boolean;
  size?: "sm" | "md";
}

/**
 * Brand logo. The symbol mark and 이팝나무 tree are IMAGE assets the client will
 * deliver (ideally the whole lockup as one SVG/PNG). Until then those slots are
 * black-box placeholders; only the wordmark text is real content.
 */
export function Logo({ withTree = false, size = "md" }: LogoProps) {
  return (
    <Link to="/" className={`logo logo--${size}`} aria-label={companyInfo.nameKo}>
      <span className="logo__lockup">
        <span className="logo__mark" role="img" aria-label="로고 심볼 — 이미지 추가 필요" />
        <span className="logo__words">
          <span className="logo__ko">{companyInfo.nameKo}</span>
          <span className="logo__en">{companyInfo.nameEn}</span>
        </span>
        {withTree && <span className="logo__tree" role="img" aria-label="이팝나무 — 이미지 추가 필요" />}
      </span>
    </Link>
  );
}
