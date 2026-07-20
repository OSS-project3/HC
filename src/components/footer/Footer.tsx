import { useState } from "react";
import { Link } from "react-router-dom";
import clsx from "clsx";
import { Logo } from "../brand/Logo";
import { footerNav } from "../../config/navigation";
import { companyInfo } from "../../config/company";
import { policyDocuments, type PolicyType } from "../../data/policies";
import { Modal } from "../ui/Modal";
import { SocialLinks } from "./SocialLinks";
import "./Footer.css";

const policyOrder: Exclude<PolicyType, "sitemap">[] = ["privacy", "terms", "email"];

export function Footer() {
  const [policy, setPolicy] = useState<Exclude<PolicyType, "sitemap"> | null>(null);
  const [sitemapOpen, setSitemapOpen] = useState(false);

  const active = policy ? policyDocuments[policy] : null;
  const idx = policy ? policyOrder.indexOf(policy) : -1;

  return (
    <footer className="footer">
      <div className="footer__inner page-container">
        {/* Row 1: menu + policy links */}
        <div className="footer__top">
          <nav className="footer__menu" aria-label="푸터 메뉴">
            {footerNav.map((item) => (
              <Link key={item.to} to={item.to} className="footer__menu-link">
                {item.label}
              </Link>
            ))}
          </nav>

          <div className="footer__policies">
            {policyOrder.map((p) => (
              <button key={p} className="footer__policy" onClick={() => setPolicy(p)}>
                {policyDocuments[p].title}
              </button>
            ))}
            <div className="sitemap">
              <button
                className="footer__policy"
                aria-expanded={sitemapOpen}
                onClick={() => setSitemapOpen((v) => !v)}
                onMouseEnter={() => setSitemapOpen(true)}
                onMouseLeave={() => setSitemapOpen(false)}
              >
                사이트맵
              </button>
              <div
                className={clsx("sitemap__panel", sitemapOpen && "sitemap__panel--open")}
                onMouseEnter={() => setSitemapOpen(true)}
                onMouseLeave={() => setSitemapOpen(false)}
              >
                {footerNav.map((item) => (
                  <Link key={item.to} to={item.to} className="sitemap__link">
                    {item.label}
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Row 2: logo + company info + SNS */}
        <div className="footer__main">
          <div className="footer__brand">
            <Logo size="sm" />
          </div>

          <address className="footer__info">
            <p>{companyInfo.address}</p>
            <p>
              <span>대표전화 {companyInfo.phone}</span>
              <i aria-hidden="true">|</i>
              <span>팩스 {companyInfo.fax}</span>
              <i aria-hidden="true">|</i>
              <span>
                {companyInfo.representativeTitle} {companyInfo.representative}
              </span>
              <i aria-hidden="true">|</i>
              <span>법인등록번호 {companyInfo.registrationNumber}</span>
            </p>
            <p>
              <span>
                {companyInfo.businessHours} {companyInfo.businessHoursNote}
              </span>
              <i aria-hidden="true">|</i>
              <span>특허출원번호 {companyInfo.patentNumber}</span>
            </p>
          </address>

          <SocialLinks />
        </div>

        <p className="footer__copyright">{companyInfo.copyright}</p>
      </div>

      <Modal
        open={active !== null}
        onClose={() => setPolicy(null)}
        title={active?.title ?? ""}
        onPrev={idx > 0 ? () => setPolicy(policyOrder[idx - 1]) : undefined}
        onNext={idx >= 0 && idx < policyOrder.length - 1 ? () => setPolicy(policyOrder[idx + 1]) : undefined}
      >
        {active?.paragraphs.map((para, i) => (
          <p key={i}>{para}</p>
        ))}
      </Modal>
    </footer>
  );
}
