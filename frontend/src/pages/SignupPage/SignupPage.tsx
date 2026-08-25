// Sign-up page — 이메일 인증(인라인) 후 실제 백엔드 회원가입.
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { api, ApiError } from "../../services/api";
import { showToast } from "../../components/ui/toast";
import "../LoginPage/LoginPage.css";

export function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "", confirm: "" });
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Partial<Record<keyof typeof form, string>>>({});
  // 이메일 인증 상태
  const [code, setCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [signupToken, setSignupToken] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = (patch: Partial<typeof form>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((current) => { const next = { ...current }; Object.keys(patch).forEach((key) => delete next[key as keyof typeof form]); return next; });
  };
  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim());

  const requestCode = async () => {
    if (!emailValid) { setErrors((e) => ({ ...e, email: "올바른 이메일 형식으로 입력해 주세요." })); return; }
    setBusy(true); setError(null);
    try {
      // 인증코드 발송 전에 이메일 중복을 먼저 확인해 조기에 안내한다(§1.1-b).
      const { exists } = await api.checkEmail(form.email.trim());
      if (exists) { setErrors((e) => ({ ...e, email: "이미 가입된 이메일입니다. 로그인해 주세요." })); return; }
      await api.requestSignupEmailCode(form.email.trim());
      setCodeSent(true);
      showToast("인증 코드를 이메일로 발송했습니다.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "인증 코드 발송에 실패했습니다.");
    } finally { setBusy(false); }
  };

  const confirmCode = async () => {
    if (!code.trim()) { setError("인증 코드를 입력해 주세요."); return; }
    setBusy(true); setError(null);
    try {
      const res = await api.confirmSignupEmailCode(form.email.trim(), code.trim());
      setSignupToken(res.signupToken); setVerified(true);
      showToast("이메일 인증이 완료되었습니다.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "인증 코드가 올바르지 않거나 만료되었습니다.");
    } finally { setBusy(false); }
  };

  const validate = () => {
    const next: typeof errors = {};
    if (!form.name.trim()) next.name = "이름을 입력해 주세요.";
    else if (form.name.trim().length < 2) next.name = "이름은 2자 이상 입력해 주세요.";
    if (!emailValid) next.email = "올바른 이메일 형식으로 입력해 주세요.";
    if (!form.password) next.password = "비밀번호를 입력해 주세요.";
    else if (form.password.length < 8 || form.password.length > 72) next.password = "비밀번호는 8~72자로 입력해 주세요.";
    if (!form.confirm) next.confirm = "비밀번호 확인을 입력해 주세요.";
    else if (form.password !== form.confirm) next.confirm = "비밀번호가 일치하지 않습니다.";
    if (!form.phone.trim()) next.phone = "전화번호를 입력해 주세요.";
    else if (!/^01[016789]-?\d{3,4}-?\d{4}$/.test(form.phone.trim())) next.phone = "올바른 휴대전화 번호를 입력해 주세요. 예: 010-1234-5678";
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!verified || !signupToken) { setError("이메일 인증을 먼저 완료해 주세요."); return; }
    if (!validate()) return;
    setBusy(true);
    try {
      await api.signup({ email: form.email.trim(), signupToken, password: form.password, name: form.name.trim(), phone: form.phone.trim() });
      showToast("회원가입이 완료되었습니다. 로그인해 주세요.");
      navigate("/login");
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "회원가입에 실패했습니다.");
    } finally { setBusy(false); }
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">회원가입</h1>
        <p className="auth__lead">이메일 인증 후 가입할 수 있습니다.</p>

        <form className="auth__form" onSubmit={submit}>
          <label className="field">
            <span className="field__label">이메일 <span className="req">*</span></span>
            <div className="field__with-btn">
              <input
                className="field__input"
                type="email"
                autoComplete="username"
                value={form.email}
                onChange={(e) => set({ email: e.target.value })}
                placeholder="you@example.com"
                required
                disabled={verified}
                aria-invalid={Boolean(errors.email)}
              />
              <button type="button" className="postal-btn" onClick={requestCode} disabled={busy || verified || !emailValid}>
                {verified ? "인증완료" : codeSent ? "재발송" : "인증코드 받기"}
              </button>
            </div>
            {errors.email && <span className="field-error" role="alert">{errors.email}</span>}
          </label>

          {!verified && (
            <label className="field">
              <span className="field__label">인증 코드 <span className="req">*</span></span>
              <div className="field__with-btn">
                <input
                  className="field__input"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  placeholder={codeSent ? "이메일로 받은 6자리 코드" : "먼저 '인증코드 받기'를 눌러주세요"}
                  inputMode="numeric"
                  maxLength={6}
                  disabled={!codeSent}
                />
                <button type="button" className="postal-btn" onClick={confirmCode} disabled={busy || !codeSent}>확인</button>
              </div>
              <small className="auth__field-hint">
                {codeSent ? "이메일로 받은 6자리 코드를 10분 안에 입력해 주세요." : "이메일 입력 후 '인증코드 받기'를 누르면 인증 코드가 발송됩니다."}
              </small>
            </label>
          )}
          {verified && <p className="auth__field-hint auth__field-hint--ok">✓ 이메일 인증 완료</p>}

          <label className="field">
            <span className="field__label">이름 <span className="req">*</span></span>
            <input className="field__input" value={form.name} onChange={(e) => set({ name: e.target.value })} placeholder="이름" required aria-invalid={Boolean(errors.name)} />
            {errors.name && <span className="field-error" role="alert">{errors.name}</span>}
          </label>
          <label className="field">
            <span className="field__label">비밀번호 <span className="req">*</span></span>
            <input className="field__input" type="password" autoComplete="new-password" value={form.password} onChange={(e) => set({ password: e.target.value })} placeholder="비밀번호" required aria-invalid={Boolean(errors.password)} />
            <small className="auth__field-hint">8~72자</small>
            {errors.password && <span className="field-error" role="alert">{errors.password}</span>}
          </label>
          <label className="field">
            <span className="field__label">비밀번호 확인 <span className="req">*</span></span>
            <input className="field__input" type="password" autoComplete="new-password" value={form.confirm} onChange={(e) => set({ confirm: e.target.value })} placeholder="비밀번호 확인" required aria-invalid={Boolean(errors.confirm)} />
            {errors.confirm && <span className="field-error" role="alert">{errors.confirm}</span>}
          </label>
          <label className="field">
            <span className="field__label">전화번호 <span className="req">*</span></span>
            <input className="field__input" type="tel" autoComplete="tel" value={form.phone} onChange={(e) => set({ phone: e.target.value })} placeholder="010-1234-5678" required inputMode="numeric" aria-invalid={Boolean(errors.phone)} />
            {errors.phone && <span className="field-error" role="alert">{errors.phone}</span>}
          </label>

          {error && <p className="field-error">{error}</p>}

          <Button type="submit" block>회원가입</Button>
        </form>

        <p className="auth__switch">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </section>
  );
}
