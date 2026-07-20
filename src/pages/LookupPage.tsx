import { useState } from "react";
import { Button } from "../components/ui/Button";
import "./LookupPage.css";

type Method = "card" | "application";

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
  const [method, setMethod] = useState<Method>("card");
  const [keyValue, setKeyValue] = useState("");
  const [phone, setPhone] = useState("");
  const [result, setResult] = useState<LookupResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!keyValue.trim() || normalizePhone(phone).length < 9) {
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
      <p className="eyebrow">조회</p>
      <h1 className="lookup__title">신청 조회</h1>
      <p className="section-lead">
        개인정보 보호를 위해 <strong>번호와 연락처를 함께</strong> 입력해야 조회할 수 있습니다.
      </p>

      <form className="lookup__form" onSubmit={submit}>
        <div className="lookup__methods" role="radiogroup" aria-label="조회 기준">
          <label className="radio">
            <input
              type="radio"
              name="method"
              checked={method === "card"}
              onChange={() => setMethod("card")}
            />
            카드번호로 조회
          </label>
          <label className="radio">
            <input
              type="radio"
              name="method"
              checked={method === "application"}
              onChange={() => setMethod("application")}
            />
            신청번호로 조회
          </label>
        </div>

        <label className="field">
          <span className="field__label">
            {method === "card" ? "카드번호" : "신청번호"}
            <span className="req">*</span>
          </span>
          <input
            className="field__input"
            value={keyValue}
            onChange={(e) => setKeyValue(e.target.value)}
            placeholder={method === "card" ? "HN-KR-2609-1188" : "APP-2026-000123"}
          />
        </label>

        <label className="field">
          <span className="field__label">
            연락처<span className="req">*</span>
          </span>
          <input
            className="field__input"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            inputMode="tel"
            placeholder="010-1234-5678"
          />
        </label>

        {error && <p className="field-error">{error}</p>}

        <Button type="submit" block>
          조회하기
        </Button>
        <p className="lookup__note">
          전화번호 단독 조회는 지원하지 않습니다. 조회가 반복 실패하면 잠시 후 다시 시도해 주세요.
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
