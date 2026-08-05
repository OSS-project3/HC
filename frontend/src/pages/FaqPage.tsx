import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { PhoneIcon } from "../components/ui/icons";
import { companyInfo } from "../config/company";
import { faqs } from "./SupportPage";
import "./SupportPage.css";
import { useAuth } from "../features/auth/AuthContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../components/admin/ContentAdminPanel";

export function FaqPage() {
  const navigate = useNavigate();
  const { isAdmin } = useAuth();
  const defaults: ManagedContent[] = faqs.map((faq, index) => ({ id: `faq-${index}`, title: faq.q, content: faq.a }));
  const [managedFaqs, setManagedFaqs] = useState(() => loadManagedContent("faqs", defaults));
  const updateFaqs = (items: ManagedContent[]) => { localStorage.setItem("managed-content:faqs", JSON.stringify(items)); setManagedFaqs(items); };

  return (
    <div className="support faq-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">고객지원</p>
        <h1 className="support__title subpage-hero__title">자주 묻는 질문</h1>
      </header>

      <section className="support__section page-container">
        {isAdmin && <ContentAdminPanel label="FAQ" items={managedFaqs} onChange={updateFaqs} />}
        <div className="support-rule" aria-hidden="true"><i /></div>
        <h2 className="support__heading support__heading--plain">자주 묻는 질문</h2>
        <p className="faq__intro">
          자주 문의하시는 내용을 정리했습니다.<br />
          원하시는 내용을 찾지 못하신 경우 상담 문의를 이용해 주세요.
        </p>

        <div className="faq">
          {managedFaqs.map((faq, index) => (
            <details key={faq.id} className="faq__item" open={index === 0 ? true : undefined}>
              <summary className="faq__q"><b>Q.</b><span>{faq.title}</span></summary>
              <p className="faq__a"><b>A.</b><span>{faq.content}</span></p>
            </details>
          ))}
        </div>

        <div className="faq-help">
          <span className="faq-help__icon"><PhoneIcon /></span>
          <p>상담 문의 {companyInfo.phone}　 |　 {companyInfo.businessHours}<br />고객지원에서 다양한 상담 방법을 이용하실 수 있습니다.</p>
          <button onClick={() => navigate("/support#contact")}>고객 지원</button>
        </div>
      </section>
    </div>
  );
}
