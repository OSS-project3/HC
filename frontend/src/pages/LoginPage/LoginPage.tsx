// Login page.
import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { useAuth } from "../../features/auth/AuthContext";
import { api } from "../../services/api";
import "./LoginPage.css";

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = searchParams.get("returnTo");
  const safeReturnTo = returnTo?.startsWith("/") && !returnTo.startsWith("//") ? returnTo : "/";
  const { login, refreshProfile } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: typeof errors = {};
    if (!email.trim()) nextErrors.email = "이메일을 입력해 주세요.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) nextErrors.email = "올바른 이메일 형식으로 입력해 주세요.";
    if (!password) nextErrors.password = "비밀번호를 입력해 주세요.";
    if (Object.keys(nextErrors).length) { setErrors(nextErrors); return; }
    setErrors({});
    // ⚠️ TEMP: 임시 관리자 로그인. 백엔드에 시드된 admin 계정으로 실제 로그인(ADMIN 토큰)을 시도하고,
    //    실패 시(오프라인·미시드) 클라이언트 admin 상태로 폴백해 UI만이라도 열리게 한다.
    //    운영 배포 전 제거. 문서: docs/TEMP_ADMIN_LOGIN.md
    if (email.trim() === "admin@test.com" && password === "admin1234!") {
      // 1) 클라이언트 admin 상태를 먼저 세팅해 /admin 가드를 즉시 통과시킨다(렌더 레이스 방지).
      login({ name: "관리자", email: "admin@test.com", role: "admin" });
      // 2) 이어서 실제 백엔드 로그인으로 ADMIN 세션 쿠키까지 확보(성공 시 refreshProfile이 서버 기준으로 갱신).
      //    실패(오프라인·미시드)해도 클라이언트 admin 상태가 남아 UI는 열린다(admin API는 401).
      try {
        await api.loginWithPassword("admin@test.com", "admin1234!");
        await refreshProfile();
      } catch {
        /* keep client-only admin */
      }
      navigate(safeReturnTo === "/" ? "/admin" : safeReturnTo);
      return;
    }
    // Mock: sign in as a regular user with the entered email.
    login({ name: email.split("@")[0], email, role: "user" });
    navigate(safeReturnTo);
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
          <Button type="submit" block>
            로그인
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
