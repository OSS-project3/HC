import { Button } from "../components/ui/Button";
import "./LoginPage.css";

export function LoginPage() {
  return (
    <section className="login page-container">
      <div className="login__card">
        <h1 className="login__title">로그인</h1>
        <p className="login__lead">한글과 세종 관리자 · 신청 조회 로그인</p>
        <form className="login__form" onSubmit={(e) => e.preventDefault()}>
          <label className="field">
            <span className="field__label">아이디</span>
            <input className="field__input" type="text" autoComplete="username" placeholder="아이디" />
          </label>
          <label className="field">
            <span className="field__label">비밀번호</span>
            <input
              className="field__input"
              type="password"
              autoComplete="current-password"
              placeholder="비밀번호"
            />
          </label>
          <Button type="submit" block>
            로그인
          </Button>
        </form>
      </div>
    </section>
  );
}
