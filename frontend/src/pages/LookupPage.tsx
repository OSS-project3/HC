// Application lookup page with contact and issued-card-number lookup methods.
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { loadApplications } from "../data/adminMock";
import "./LookupPage.css";
import { api } from "../services/api";

type LookupMethod = "contact" | "card";

const DEMO_PHONE = "01012345678";
const DEMO_EMAIL = "qwer@gmail.com";
const DEMO_CARD_NUMBER = "ROK-12345-6789";

function normalizePhone(value: string) {
  return value.replace(/\D/g, "");
}

function normalizeCardNumber(value: string) {
  return value.trim().toUpperCase().replace(/\s/g, "");
}

export function LookupPage() {
  const navigate = useNavigate();
  const [method, setMethod] = useState<LookupMethod>("contact");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [cardNumber, setCardNumber] = useState("");
  const [applicationNumber, setApplicationNumber] = useState("");
  const [error, setError] = useState<string | null>(null);

  const changeMethod = (next: LookupMethod) => {
    setMethod(next);
    setError(null);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const contactMatches =
      normalizePhone(phone) === DEMO_PHONE && email.trim().toLowerCase() === DEMO_EMAIL;
    const savedApplicationMatches = loadApplications().some((application) => normalizePhone(application.phone) === normalizePhone(phone) && application.applicantEmail?.toLowerCase() === email.trim().toLowerCase());
    const cardMatches = normalizeCardNumber(cardNumber) === DEMO_CARD_NUMBER;

    try {
      const result = await api.lookupApplication({ method: method === "card" ? "card" : "application", keyValue: method === "card" ? cardNumber : applicationNumber, phone: phone || undefined, email: email || undefined });
      sessionStorage.setItem("last-application-lookup", JSON.stringify(result));
      navigate("/mobile-card", { state: { application: result } });
      return;
    } catch {
      // Preserve mock data while the content APIs and local account system are pending.
    }
    if ((method === "contact" && (contactMatches || savedApplicationMatches)) || (method === "card" && cardMatches)) {
      navigate("/mobile-card");
      return;
    }

    setError(
      method === "contact"
        ? "입력하신 전화번호와 이메일에 해당하는 발급 내역을 찾을 수 없습니다."
        : "입력하신 카드번호에 해당하는 발급 내역을 찾을 수 없습니다.",
    );
  };

  return (
    <section className="lookup page-container">
      <header className="subpage-hero lookup__hero">
        <p className="eyebrow">조회</p>
        <h1 className="subpage-hero__title">신청 조회</h1>
        <p className="section-lead">발급 신청 정보 또는 카드번호로 모바일 카드를 확인할 수 있습니다.</p>
      </header>

      <div className="lookup__panel">
        <div className="lookup__tabs" role="tablist" aria-label="신청 조회 방법">
          <button
            type="button"
            role="tab"
            aria-selected={method === "contact"}
            className={`lookup__tab${method === "contact" ? " lookup__tab--active" : ""}`}
            onClick={() => changeMethod("contact")}
          >
            전화번호 · 이메일
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={method === "card"}
            className={`lookup__tab${method === "card" ? " lookup__tab--active" : ""}`}
            onClick={() => changeMethod("card")}
          >
            카드번호
          </button>
        </div>

        <form className="lookup__form" onSubmit={submit}>
          {method === "contact" ? (
            <>
              <label className="field"><span className="field__label">신청번호<span className="req">*</span></span><input className="field__input" value={applicationNumber} onChange={(event) => setApplicationNumber(event.target.value)} placeholder="APP-2026-000001" required /></label>
              <label className="field">
                <span className="field__label">전화번호<span className="req">*</span></span>
                <input
                  className="field__input"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  type="tel"
                  inputMode="tel"
                  autoComplete="tel"
                  placeholder="010-1234-5678"
                  required
                />
              </label>
              <label className="field">
                <span className="field__label">이메일<span className="req">*</span></span>
                <input
                  className="field__input"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoComplete="email"
                  placeholder="hong@example.com"
                  required
                />
              </label>
            </>
          ) : (
            <label className="field">
              <span className="field__label">발급 카드번호<span className="req">*</span></span>
              <input
                className="field__input"
                value={cardNumber}
                onChange={(event) => setCardNumber(event.target.value)}
                placeholder="ROK-12345-6789"
                autoComplete="off"
                required
              />
            </label>
          )}

          {error && <p className="field-error" role="alert">{error}</p>}

          <Button type="submit" block>모바일 카드 확인</Button>
          <p className="lookup__note">
            {method === "contact"
              ? "전화번호와 이메일이 신청 정보와 모두 일치해야 조회할 수 있습니다."
              : "발급받은 카드에 표시된 카드번호를 입력해 주세요."}
          </p>
        </form>
      </div>
    </section>
  );
}
