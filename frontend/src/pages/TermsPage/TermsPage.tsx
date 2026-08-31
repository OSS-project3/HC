// 약관 동의 페이지.
// 백엔드 OAuth2SuccessHandler가 신규 사용자를 이 경로(`/terms`)로 리다이렉트한다.
// 이메일 회원가입 직후에도 사용할 수 있다. 제출 시 `POST /api/auth/terms`를 호출한다.
// 약관 본문/정책 버전은 아직 확정되지 않아 [TBD]로 표기하며 임의 문구를 확정하지 않는다.
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { useAuth } from "../../features/auth/AuthContext";
import { useLanguage } from "../../features/i18n/LanguageContext";
import { api } from "../../services/api";
import "../LoginPage/LoginPage.css";

export function TermsPage() {
  const navigate = useNavigate();
  const { t, language } = useLanguage();
  const { user, refreshProfile } = useAuth();
  const [agreements, setAgreements] = useState({ privacyAgreed: false, imageUploadAgreed: false, shippingAgreed: false });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // 리다이렉트 직후 쿠키 세션으로 사용자 정보를 확보한다.
  useEffect(() => { void refreshProfile(); }, [refreshProfile]);

  const allAgreed = agreements.privacyAgreed && agreements.imageUploadAgreed && agreements.shippingAgreed;
  const toggleAll = (checked: boolean) => setAgreements({ privacyAgreed: checked, imageUploadAgreed: checked, shippingAgreed: checked });

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!allAgreed) { setError("필수 약관에 모두 동의해 주세요."); return; }
    setSubmitting(true);
    try {
      await api.agreeTerms(agreements);
      await refreshProfile();
      navigate("/", { replace: true });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "약관 동의 처리에 실패했습니다. 다시 시도해 주세요.");
      setSubmitting(false);
    }
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">{t("약관 동의")}</h1>
        <p className="auth__lead">
          {language === "en"
            ? `Please agree to the terms below to use our service.${user?.name ? ` Welcome, ${user.name}.` : ""}`
            : `서비스 이용을 위해 아래 약관에 동의해 주세요.${user?.name ? ` ${user.name}님, 환영합니다.` : ""}`}
        </p>

        <form className="auth__form" onSubmit={submit}>
          <label className="check" style={{ fontWeight: 600 }}>
            <input type="checkbox" checked={allAgreed} onChange={(e) => toggleAll(e.target.checked)} />
            <span>{t("전체 동의")}</span>
          </label>

          <TermsItem
            title="개인정보 수집 및 이용 동의 (필수)"
            checked={agreements.privacyAgreed}
            onChange={(v) => setAgreements((a) => ({ ...a, privacyAgreed: v }))}
          />
          <TermsItem
            title="이미지 업로드 및 활용 동의 (필수)"
            checked={agreements.imageUploadAgreed}
            onChange={(v) => setAgreements((a) => ({ ...a, imageUploadAgreed: v }))}
          />
          <TermsItem
            title="배송 안내 수신 동의 (필수)"
            checked={agreements.shippingAgreed}
            onChange={(v) => setAgreements((a) => ({ ...a, shippingAgreed: v }))}
          />

          {error && <p className="field-error" role="alert">{t(error)}</p>}

          <Button type="submit" block disabled={!allAgreed || submitting}>{submitting ? t("처리 중…") : t("동의하고 시작하기")}</Button>
        </form>
      </div>
    </section>
  );
}

function TermsItem({ title, checked, onChange }: { title: string; checked: boolean; onChange: (value: boolean) => void }) {
  const { t } = useLanguage();
  return (
    <div className="field" style={{ gap: 8 }}>
      <label className="check">
        <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
        <span>{t(title)}</span>
      </label>
      <div style={{ maxHeight: 120, overflow: "auto", padding: 12, border: "1px solid #e5ded2", background: "#faf8f4", fontSize: 13, color: "#4b5563", borderRadius: 8 }}>
        {t("[TBD] 약관 본문은 정책 확정 후 반영됩니다.")}
      </div>
    </div>
  );
}
