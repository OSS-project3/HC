import { useNavigate } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import { PhoneIcon } from "../../components/ui/icons";
import { companyInfo } from "../../config/company";
import "../SupportPage/SupportPage.css";
import { useAuth } from "../../features/auth/AuthContext";
import { BoardAdminPanel } from "../../components/admin/BoardAdminPanel";
import { api, type BoardListItem } from "../../services/api";

export function FaqPage() {
  const navigate = useNavigate();
  const { isAdmin } = useAuth();
  const [managedFaqs, setManagedFaqs] = useState<BoardListItem[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");

  const reload = useCallback(() => {
    setStatus("loading");
    api.listBoards({ type: "FAQ", size: 100 })
      .then((data) => { setManagedFaqs(data.content); setStatus("ready"); })
      .catch(() => setStatus("error"));
  }, []);

  useEffect(() => { reload(); }, [reload]);

  return (
    <div className="support faq-page">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">고객지원</p>
        <h1 className="support__title subpage-hero__title">자주 묻는 질문</h1>
      </header>

      <section className="support__section page-container">
        {isAdmin && <BoardAdminPanel boardType="FAQ" items={managedFaqs} onChanged={reload} />}
        <h2 className="support__heading support__heading--plain">자주 묻는 질문</h2>
        <p className="faq__intro">
          자주 문의하시는 내용을 정리했습니다.<br />
          원하시는 내용을 찾지 못하신 경우 상담 문의를 이용해 주세요.
        </p>

        <div className="faq">
          {status === "loading" && <p className="faq__intro">불러오는 중입니다…</p>}
          {status === "error" && <p className="faq__intro">FAQ를 불러오지 못했습니다.</p>}
          {status === "ready" && managedFaqs.map((faq, index) => (
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
