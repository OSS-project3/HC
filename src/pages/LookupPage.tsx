// Application lookup / status-check page.
import { useState } from "react";
import { Button } from "../components/ui/Button";
import "./LookupPage.css";

/** Normalise a phone number to digits only, matching the DB storage rule. */
function normalizePhone(phone: string) {
  return phone.replace(/\D/g, "");
}

interface LookupResult {
  applicationNumber: string;
  status: string;
  applicantNameMasked: string;
  cardType: string;
  submittedAt: string;
}

const statusLabels: Record<string, string> = {
  IN_PRODUCTION: "제작 중",
  SUBMITTED: "접수 완료",
  PAYMENT_PENDING: "입금 대기",
  COMPLETED: "발급 완료",
};

export function LookupPage() {
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [result, setResult] = useState<LookupResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (normalizePhone(phone).length < 9 || !email.trim().includes("@")) {
      setError("조회 정보를 정확히 입력해 주세요.");
      setResult(null);
      return;
    }
    // Demo response. In production this calls POST /api/applications/lookup and
    // returns only the minimal, partially-masked fields shown below.
    setResult({
      applicationNumber: "APP-2026-000123",
      status: "IN_PRODUCTION",
      applicantNameMasked: "이*하",
      cardType: "명예한국인증",
      submittedAt: "2026-07-15",
    });
  };

  return (
    <section className="lookup page-container">
      <header className="subpage-hero lookup__hero">
        <p className="eyebrow">조회</p>
        <h1 className="subpage-hero__title">신청 조회</h1>
        <p className="section-lead">
          입력하신 <strong>전화번호와 이메일</strong>로 신청 내역을 조회할 수 있습니다.
        </p>
      </header>

      <form className="lookup__form" onSubmit={submit}>
        <label className="field">
          <span className="field__label">
            전화번호<span className="req">*</span>
          </span>
          <input
            className="field__input"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            inputMode="tel"
            placeholder="010-1234-5678"
          />
        </label>

        <label className="field">
          <span className="field__label">
            이메일<span className="req">*</span>
          </span>
          <input
            className="field__input"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="hong@example.com"
          />
        </label>

        {error && <p className="field-error">{error}</p>}

        <Button type="submit" block>
          조회하기
        </Button>
        <p className="lookup__note">
          전화번호와 이메일이 신청 정보와 모두 일치해야 조회할 수 있습니다.
        </p>
      </form>

      {result && (
        <div className="lookup__result">
          <h2 className="lookup__result-title">조회 결과</h2>
          <dl className="lookup__grid">
            <div>
              <dt>신청번호</dt>
              <dd>{result.applicationNumber}</dd>
            </div>
            <div>
              <dt>신청인</dt>
              <dd>{result.applicantNameMasked}</dd>
            </div>
            <div>
              <dt>카드 종류</dt>
              <dd>{result.cardType}</dd>
            </div>
            <div>
              <dt>진행 상태</dt>
              <dd>
                <span className="lookup__status">{statusLabels[result.status] ?? result.status}</span>
              </dd>
            </div>
            <div>
              <dt>접수일</dt>
              <dd>{result.submittedAt}</dd>
            </div>
          </dl>
        </div>
      )}
    </section>
  );
}
