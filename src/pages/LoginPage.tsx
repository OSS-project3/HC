import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { useAuth, demoUser } from "../features/auth/AuthContext";
import "./LoginPage.css";

export function LoginPage() {
  const navigate = useNavigate();
  const { login, loginAsUser, loginAsAdmin } = useAuth();
  const [email, setEmail] = useState("");

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    // Mock: sign in as a regular user with the entered email.
    login({ name: email.split("@")[0] || demoUser.name, email: email || demoUser.email, role: "user" });
    navigate("/");
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">로그인</h1>
        <p className="auth__lead">한글과 세종 · 신청 조회 및 관리 로그인</p>

        <form className="auth__form" onSubmit={submit}>
          <label className="field">
            <span className="field__label">이메일</span>
            <input
              className="field__input"
              type="email"
              autoComplete="username"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label className="field">
            <span className="field__label">비밀번호</span>
            <input className="field__input" type="password" autoComplete="current-password" placeholder="비밀번호" />
          </label>
          <Button type="submit" block>
            로그인
          </Button>
        </form>

        <div className="auth__divider">
          <span>또는 데모 계정으로 체험</span>
        </div>

        <div className="auth__demo">
          <Button
            variant="ghost"
            block
            onClick={() => {
              loginAsUser();
              navigate("/");
            }}
          >
            일반 사용자 데모 로그인
          </Button>
          <Button
            variant="outline"
            block
            onClick={() => {
              loginAsAdmin();
              navigate("/admin");
            }}
          >
            관리자 데모 로그인
          </Button>
        </div>

        <p className="auth__switch">
          아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
      </div>
    </section>
  );
}
