// Login page.
import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { useAuth } from "../../features/auth/AuthContext";
import { api, ApiError } from "../../services/api";
import "./LoginPage.css";

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = searchParams.get("returnTo");
  const safeReturnTo = returnTo?.startsWith("/") && !returnTo.startsWith("//") ? returnTo : "/";
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    const nextErrors: typeof errors = {};
    if (!email.trim()) nextErrors.email = "이메일을 입력해 주세요.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) nextErrors.email = "올바른 이메일 형식으로 입력해 주세요.";
    if (!password) nextErrors.password = "비밀번호를 입력해 주세요.";
    if (Object.keys(nextErrors).length) { setErrors(nextErrors); return; }
    setErrors({});
    setSubmitting(true);
    try {
      // 실제 백엔드 로그인 — 성공하면 서버 세션(HttpOnly 쿠키) 확보. 로그인 상태로 홈(또는 returnTo)으로 이동.
      const me = await api.loginWithPassword(email.trim(), password);
      const role = me.role === "ADMIN" ? "admin" : "user";
      login({ name: me.name, email: me.email, role, source: "api", phone: me.phone, address: me.address });
      const dest = role === "admin" && safeReturnTo === "/" ? "/admin" : safeReturnTo;
      navigate(dest, { replace: true });
    } catch (err) {
      // 실패 시 mock 로그인/이동 없이 안내 문구만 표시하고 로그인 화면에 머무른다.
      if (err instanceof ApiError && (err.status === 401 || err.status === 400 || err.status === 404)) {
        setFormError("아이디/비밀번호가 틀리거나 존재하지 않는 계정입니다.");
      } else {
        setFormError("로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">로그인</h1>

        <form className="auth__form" onSubmit={submit}>
          <label className="field">
            <span className="field__label">이메일 <span className="req">*</span></span>
            <input
              className="field__input"
              type="email"
              autoComplete="username"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => { setEmail(e.target.value); if (errors.email) setErrors((current) => ({ ...current, email: undefined })); }}
              required
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? "login-email-error" : undefined}
            />
            {errors.email && <span id="login-email-error" className="field-error" role="alert">{errors.email}</span>}
          </label>
          <label className="field">
            <span className="field__label">비밀번호 <span className="req">*</span></span>
            <input className="field__input" type="password" autoComplete="current-password" placeholder="비밀번호" value={password} onChange={(e) => { setPassword(e.target.value); if (errors.password) setErrors((current) => ({ ...current, password: undefined })); }} required aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? "login-password-error" : undefined} />
            {errors.password && <span id="login-password-error" className="field-error" role="alert">{errors.password}</span>}
          </label>
          {formError && <p className="auth__form-error" role="alert">{formError}</p>}
          <Button type="submit" block disabled={submitting}>
            {submitting ? "로그인 중…" : "로그인"}
          </Button>
        </form>

        <div className="auth__recovery-links" aria-label="계정 찾기">
          <Link to="/account-recovery?type=id">아이디 찾기</Link>
          <i aria-hidden="true" />
          <Link to="/account-recovery?type=password">비밀번호 찾기</Link>
        </div>

        <div className="auth__divider">
          <span>소셜 계정으로 간편 로그인</span>
        </div>

        <div className="auth__social">
          <button className="auth__social-button auth__social-button--google" type="button" onClick={() => { window.location.href = api.oauthUrl("google"); }}>
            <span className="auth__social-logo auth__social-logo--google" aria-hidden="true"><GoogleLogo /></span>
            <span>Google로 계속하기</span>
          </button>
          <button className="auth__social-button auth__social-button--naver" type="button" onClick={() => { window.location.href = api.oauthUrl("naver"); }}>
            <span className="auth__social-logo" aria-hidden="true"><NaverLogo /></span>
            <span>네이버로 계속하기</span>
          </button>
        </div>

        <p className="auth__switch">
          아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
      </div>
    </section>
  );
}

function GoogleLogo() {
  return <svg viewBox="0 0 24 24" role="img"><path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.4-.18-2.06H12v3.9h5.38a4.6 4.6 0 0 1-2 3.02v2.53h3.24c1.9-1.75 2.98-4.33 2.98-7.39Z"/><path fill="#34A853" d="M12 22c2.7 0 4.96-.9 6.62-2.38l-3.24-2.53c-.9.6-2.05.96-3.38.96-2.6 0-4.8-1.76-5.59-4.12H3.07v2.61A10 10 0 0 0 12 22Z"/><path fill="#FBBC05" d="M6.41 13.93A6.02 6.02 0 0 1 6.1 12c0-.67.12-1.32.31-1.93V7.46H3.07A10 10 0 0 0 2 12c0 1.61.39 3.14 1.07 4.54l3.34-2.61Z"/><path fill="#EA4335" d="M12 5.95c1.47 0 2.79.5 3.82 1.5l2.87-2.87A9.62 9.62 0 0 0 12 2a10 10 0 0 0-8.93 5.46l3.34 2.61C7.2 7.71 9.4 5.95 12 5.95Z"/></svg>;
}

function NaverLogo() {
  return <svg viewBox="0 0 24 24" role="img"><path fill="currentColor" d="M5 4h5.2l3.6 5.3V4H19v16h-5.2l-3.6-5.3V20H5V4Z"/></svg>;
}
