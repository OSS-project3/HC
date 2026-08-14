import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import "../LoginPage/LoginPage.css";

type RecoveryType = "id" | "password";

export function AccountRecoveryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialType: RecoveryType = searchParams.get("type") === "password" ? "password" : "id";
  const [type, setType] = useState<RecoveryType>(initialType);
  const [result, setResult] = useState("");

  const changeType = (next: RecoveryType) => {
    setType(next);
    setResult("");
    setSearchParams({ type: next });
  };

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setResult(
      type === "id"
        ? "입력하신 정보와 일치하는 아이디를 확인했습니다. 가입 이메일을 안내해 드립니다. (데모)"
        : "입력하신 연락처로 비밀번호 재설정 안내를 발송했습니다. (데모)",
    );
  };

  return (
    <section className="auth page-container">
      <div className="auth__card">
        <h1 className="auth__title">계정 찾기</h1>
        <p className="auth__lead">가입할 때 입력한 정보로 계정을 확인해 주세요.</p>

        <div className="auth__tabs" role="tablist" aria-label="계정 찾기 유형">
          <button
            type="button"
            role="tab"
            aria-selected={type === "id"}
            className={`auth__tab${type === "id" ? " auth__tab--active" : ""}`}
            onClick={() => changeType("id")}
          >
            아이디 찾기
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={type === "password"}
            className={`auth__tab${type === "password" ? " auth__tab--active" : ""}`}
            onClick={() => changeType("password")}
          >
            비밀번호 찾기
          </button>
        </div>

        <form className="auth__form" onSubmit={submit}>
          {type === "id" ? (
            <label className="field">
              <span className="field__label">이름</span>
              <input className="field__input" autoComplete="name" placeholder="이름" required />
            </label>
          ) : (
            <label className="field">
              <span className="field__label">아이디(이메일)</span>
              <input
                className="field__input"
                type="email"
                autoComplete="username"
                placeholder="you@example.com"
                required
              />
            </label>
          )}

          <label className="field">
            <span className="field__label">전화번호</span>
            <input
              className="field__input"
              type="tel"
              autoComplete="tel"
              placeholder="010-1234-5678"
              required
            />
          </label>

          <Button type="submit" block>
            {type === "id" ? "아이디 확인" : "재설정 안내 받기"}
          </Button>
        </form>

        {result && <p className="auth__result" role="status">{result}</p>}

        <p className="auth__switch">
          계정이 기억나셨나요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </section>
  );
}
