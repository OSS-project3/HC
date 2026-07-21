// Brand logo lockup: symbol mark, wordmark, and 이팝나무 tree.
import { Link } from "react-router-dom";
import { companyInfo } from "../../config/company";
import "./Logo.css";

interface LogoProps {
  /** Show the "이팝나무" tree beside the wordmark (header uses it). */
  withTree?: boolean;
  size?: "sm" | "md";
}

/**
 * Brand logo. The symbol mark and 이팝나무 tree are delivered image assets
 * (public/images/logo/); the wordmark is real text.
 */
export function Logo({ withTree = false, size = "md" }: LogoProps) {
  return (
    <Link to="/" className={`logo logo--${size}`} aria-label={companyInfo.nameKo}>
      <span className="logo__lockup">
        {/* Mark + tree are decorative — the Link already carries the brand name. */}
        <span className="logo__mark" aria-hidden="true" />
        <span className="logo__words">
          <span className="logo__ko">{companyInfo.nameKo}</span>
          <span className="logo__en">{companyInfo.nameEn}</span>
        </span>
        {withTree && <span className="logo__tree" aria-hidden="true" />}
      </span>
    </Link>
  );
}
