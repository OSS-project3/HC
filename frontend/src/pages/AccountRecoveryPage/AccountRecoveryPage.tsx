import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { api, ApiError } from "../../services/api";
import { useLanguage } from "../../features/i18n/LanguageContext";
import "../LoginPage/LoginPage.css";

type RecoveryType = "id" | "password";
type Phase = "request" | "confirm" | "done";

export function AccountRecoveryPage() {
  const { t, language } = useLanguage();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialType: RecoveryType = searchParams.get("type") === "password" ? "password" : "id";
  const [type, setType] = useState<RecoveryType>(initialType);
  const [phase, setPhase] = useState<Phase>("request");

  // request 단계 입력
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  // confirm 단계 입력
  const [requestId, setRequestId] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");

  const [error, setError] = useState("");
  const [result, setResult] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const reset = () => {
    setPhase("request"); setRequestId(""); setCode("");
    setNewPassword(""); setNewPasswordConfirm(""); setError(""); setResult("");
  };
  const changeType = (next: RecoveryType) => {
    setType(next);
    setName(""); setPhone(""); setEmail("");
    reset();
    setSearchParams({ type: next });
  };

  const message = (e: unknown, fallback: string) =>
    e instanceof ApiError ? e.message : e instanceof Error ? e.message : fallback;

  const submitRequest = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;
    setError(""); setResult(""); setSubmitting(true);
    try {
      const challenge = type === "id"
        ? await api.requestIdRecovery({ name: name.trim(), phone: phone.trim() })
        : await api.requestPasswordRecovery({ email: email.trim() });
      setRequestId(challenge.requestId);
      setPhase("confirm");
      setResult(type === "id"
        ? "가입 시 등록한 이메일로 인증 코드를 보냈습니다. 코드를 입력해 주세요."
        : "입력하신 이메일로 인증 코드를 보냈습니다. 코드와 새 비밀번호를 입력해 주세요.");
    } catch (e) {
      setError(message(e, "요청에 실패했습니다. 입력 정보를 확인해 주세요."));
    } finally {
      setSubmitting(false);
    }
  };

  const submitConfirm = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;
    setError("");
    if (type === "password") {
      if (newPassword.length < 8) { setError("비밀번호는 8자 이상이어야 합니다."); return; }
      if (newPassword !== newPasswordConfirm) { setError("비밀번호가 일치하지 않습니다."); return; }
    }
    setSubmitting(true);
    try {
      if (type === "id") {
        const { maskedEmail } = await api.confirmIdRecovery({ requestId, code: code.trim() });
        setResult(language === "en" ? `Your ID (email) is ${maskedEmail}.` : `회원님의 아이디(이메일)는 ${maskedEmail} 입니다.`);
      } else {
        await api.confirmPasswordRecovery({ requestId, code: code.trim(), newPassword });
        setResult("비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해 주세요.");
      }
      setPhase("done");
    } catch (e) {
      setError(message(e, "인증에 실패했습니다. 코드를 확인해 주세요."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">{t("계정 찾기")}</h1>
        <p className="auth__lead">{t("가입할 때 입력한 정보로 계정을 확인해 주세요.")}</p>

        <div className="auth__tabs" role="tablist" aria-label={t("계정 찾기 유형")}>
          <button type="button" role="tab" aria-selected={type === "id"} className={`auth__tab${type === "id" ? " auth__tab--active" : ""}`} onClick={() => changeType("id")}>{t("아이디 찾기")}</button>
          <button type="button" role="tab" aria-selected={type === "password"} className={`auth__tab${type === "password" ? " auth__tab--active" : ""}`} onClick={() => changeType("password")}>{t("비밀번호 찾기")}</button>
        </div>

        {phase === "request" && (
          <form className="auth__form" onSubmit={submitRequest}>
            {type === "id" ? (
              <>
                <label className="field">
                  <span className="field__label">{t("이름")}</span>
                  <input className="field__input" autoComplete="name" placeholder={t("이름")} value={name} onChange={(e) => setName(e.target.value)} required />
                </label>
                <label className="field">
                  <span className="field__label">{t("전화번호")}</span>
                  <input className="field__input" type="tel" autoComplete="tel" placeholder="010-1234-5678" value={phone} onChange={(e) => setPhone(e.target.value)} required />
                </label>
              </>
            ) : (
              <label className="field">
                <span className="field__label">{t("아이디(이메일)")}</span>
                <input className="field__input" type="email" autoComplete="username" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </label>
            )}
            <Button type="submit" block disabled={submitting}>{submitting ? t("처리 중…") : t("인증 코드 받기")}</Button>
          </form>
        )}

        {phase === "confirm" && (
          <form className="auth__form" onSubmit={submitConfirm}>
            <label className="field">
              <span className="field__label">{t("인증 코드")}</span>
              <input className="field__input" inputMode="numeric" autoComplete="one-time-code" placeholder={t("이메일로 받은 코드")} value={code} onChange={(e) => setCode(e.target.value)} required />
            </label>
            {type === "password" && (
              <>
                <label className="field">
                  <span className="field__label">{t("새 비밀번호")}</span>
                  <input className="field__input" type="password" autoComplete="new-password" placeholder={t("8자 이상")} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
                </label>
                <label className="field">
                  <span className="field__label">{t("새 비밀번호 확인")}</span>
                  <input className="field__input" type="password" autoComplete="new-password" placeholder={t("다시 입력")} value={newPasswordConfirm} onChange={(e) => setNewPasswordConfirm(e.target.value)} required />
                </label>
              </>
            )}
            <Button type="submit" block disabled={submitting}>{submitting ? t("처리 중…") : type === "id" ? t("아이디 확인") : t("비밀번호 재설정")}</Button>
            <button type="button" className="auth__link-button" onClick={reset}>{t("처음부터 다시")}</button>
          </form>
        )}

        {error && <p className="field-error" role="alert">{t(error)}</p>}
        {result && <p className="auth__result" role="status">{t(result)}</p>}
        {phase === "done" && (
          <p className="auth__switch"><Link to="/login">{t("로그인하러 가기")}</Link></p>
        )}
        {phase !== "done" && (
          <p className="auth__switch">{t("계정이 기억나셨나요?")} <Link to="/login">{t("로그인")}</Link></p>
        )}
      </div>
    </section>
  );
}
