// Sign-up page.
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { useAuth } from "../../features/auth/AuthContext";
import { api } from "../../services/api";
import "../LoginPage/LoginPage.css";

export function SignupPage() {
  const navigate = useNavigate();
  const { login, user, refreshProfile } = useAuth();
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "", confirm: "" });
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Partial<Record<keyof typeof form, string>>>({});

  const set = (patch: Partial<typeof form>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((current) => { const next = { ...current }; Object.keys(patch).forEach((key) => delete next[key as keyof typeof form]); return next; });
  };

  const validate = () => {
    const next: typeof errors = {};
    if (!form.name.trim()) next.name = "이름을 입력해 주세요.";
    else if (form.name.trim().length < 2) next.name = "이름은 2자 이상 입력해 주세요.";
    if (!form.email.trim()) next.email = "이메일을 입력해 주세요.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) next.email = "올바른 이메일 형식으로 입력해 주세요.";
    if (!form.password) next.password = "비밀번호를 입력해 주세요.";
    else if (form.password.length < 8 || form.password.length > 64 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password) || !/[^A-Za-z0-9]/.test(form.password)) next.password = "비밀번호는 8~64자의 영문, 숫자, 특수문자를 모두 포함해야 합니다.";
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
    if (!validate()) return;
    if (user?.source === "api") {
      try {
        await api.agreeTerms({ privacyAgreed: true, imageUploadAgreed: true, shippingAgreed: true });
        await api.updateMe({ name: form.name, phone: form.phone });
        await refreshProfile();
        navigate("/");
      } catch (reason) { setError(reason instanceof Error ? reason.message : "회원 정보 저장에 실패했습니다."); }
      return;
    }
    // Email/password membership remains local until its backend API is implemented.
    login({ name: form.name, email: form.email, role: "user" });
    navigate("/");
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">회원가입</h1>
        <p className="auth__lead">간단한 정보만 입력하면 바로 시작할 수 있습니다.</p>

        <form className="auth__form" onSubmit={submit}>
          <label className="field">
            <span className="field__label">이름 <span className="req">*</span></span>
            <input
              className="field__input"
              value={form.name}
              onChange={(e) => set({ name: e.target.value })}
              placeholder="이름"
              required aria-invalid={Boolean(errors.name)}
            />
            {errors.name && <span className="field-error" role="alert">{errors.name}</span>}
          </label>
          <label className="field">
            <span className="field__label">이메일 <span className="req">*</span></span>
            <input
              className="field__input"
              type="email"
              autoComplete="username"
              value={form.email}
              onChange={(e) => set({ email: e.target.value })}
              placeholder="you@example.com"
              required aria-invalid={Boolean(errors.email)}
            />
            {errors.email && <span className="field-error" role="alert">{errors.email}</span>}
          </label>
          <label className="field">
            <span className="field__label">비밀번호 <span className="req">*</span></span>
            <input
              className="field__input"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(e) => set({ password: e.target.value })}
              placeholder="비밀번호"
              required aria-invalid={Boolean(errors.password)}
            />
            <small className="auth__field-hint">8~64자, 영문·숫자·특수문자 포함</small>
            {errors.password && <span className="field-error" role="alert">{errors.password}</span>}
          </label>
          <label className="field">
            <span className="field__label">비밀번호 확인 <span className="req">*</span></span>
            <input
              className="field__input"
              type="password"
              autoComplete="new-password"
              value={form.confirm}
              onChange={(e) => set({ confirm: e.target.value })}
              placeholder="비밀번호 확인"
              required aria-invalid={Boolean(errors.confirm)}
            />
            {errors.confirm && <span className="field-error" role="alert">{errors.confirm}</span>}
          </label>
          <label className="field">
            <span className="field__label">전화번호 <span className="req">*</span></span>
            <input
              className="field__input"
              type="tel"
              autoComplete="tel"
              value={form.phone}
              onChange={(e) => set({ phone: e.target.value })}
              placeholder="010-1234-5678"
              required
              inputMode="numeric" aria-invalid={Boolean(errors.phone)}
            />
            {errors.phone && <span className="field-error" role="alert">{errors.phone}</span>}
          </label>

          {error && <p className="field-error">{error}</p>}

          <Button type="submit" block>
            회원가입
          </Button>
        </form>

        <p className="auth__switch">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </section>
  );
}
