import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { useAuth } from "../features/auth/AuthContext";
import "./LoginPage.css";

export function SignupPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState<string | null>(null);

  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.email.trim()) {
      setError("이름과 이메일을 입력해 주세요.");
      return;
    }
    if (form.password !== form.confirm) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }
    // Mock: create the account and sign in as a regular user.
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
            <span className="field__label">이름</span>
            <input
              className="field__input"
              value={form.name}
              onChange={(e) => set({ name: e.target.value })}
              placeholder="이름"
            />
          </label>
          <label className="field">
            <span className="field__label">이메일</span>
            <input
              className="field__input"
              type="email"
              autoComplete="username"
              value={form.email}
              onChange={(e) => set({ email: e.target.value })}
              placeholder="you@example.com"
            />
          </label>
          <label className="field">
            <span className="field__label">비밀번호</span>
            <input
              className="field__input"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(e) => set({ password: e.target.value })}
              placeholder="비밀번호"
            />
          </label>
          <label className="field">
            <span className="field__label">비밀번호 확인</span>
            <input
              className="field__input"
              type="password"
              autoComplete="new-password"
              value={form.confirm}
              onChange={(e) => set({ confirm: e.target.value })}
              placeholder="비밀번호 확인"
            />
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
