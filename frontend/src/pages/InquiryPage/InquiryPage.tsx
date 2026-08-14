import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { showToast } from "../../components/ui/toast";
import { useAuth } from "../../features/auth/AuthContext";
import { loadInquiries, saveInquiries, type InquiryRecord } from "../../data/inquiries";
import "./InquiryPage.css";

export function InquiryPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [agreed, setAgreed] = useState(false);

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const record: InquiryRecord = {
      id: crypto.randomUUID(),
      category: String(data.get("category")),
      name: String(data.get("name")),
      email: String(data.get("email")),
      phone: String(data.get("phone")),
      title: String(data.get("title")),
      content: String(data.get("content")),
      createdAt: new Date().toISOString(),
      status: "PENDING",
    };
    saveInquiries([record, ...loadInquiries()]);
    showToast("문의가 정상적으로 접수되었습니다.");
    form.reset();
    setAgreed(false);
    window.setTimeout(() => navigate("/support#contact"), 700);
  };

  return (
    <main className="inquiry-page">
      <header className="subpage-hero page-container inquiry-page__hero">
        <p className="eyebrow">고객지원 · 상담 문의</p>
        <h1 className="subpage-hero__title">1:1 문의하기</h1>
        <p className="section-lead">문의 내용을 남겨주시면 영업일 기준 1~2일 내 답변드리겠습니다.</p>
      </header>
      <section className="inquiry-form-wrap page-container">
        <form className="inquiry-form" onSubmit={submit}>
          <div className="inquiry-form__grid">
            <label className="field"><span className="field__label">문의 유형 <i className="req">*</i></span><select className="field__select" name="category" required defaultValue=""><option value="" disabled>문의 유형을 선택해 주세요</option><option>제작 신청</option><option>결제 및 배송</option><option>카드 발급</option><option>행사·단체 협업</option><option>기타</option></select></label>
            <label className="field"><span className="field__label">이름 <i className="req">*</i></span><input className="field__input" name="name" required defaultValue={user?.name ?? ""} placeholder="이름을 입력해 주세요" /></label>
            <label className="field"><span className="field__label">이메일 <i className="req">*</i></span><input className="field__input" name="email" type="email" required defaultValue={user?.email ?? ""} placeholder="답변받을 이메일" /></label>
            <label className="field"><span className="field__label">연락처 <i className="req">*</i></span><input className="field__input" name="phone" type="tel" required placeholder="010-0000-0000" /></label>
          </div>
          <label className="field"><span className="field__label">제목 <i className="req">*</i></span><input className="field__input" name="title" required maxLength={80} placeholder="문의 제목을 입력해 주세요" /></label>
          <label className="field"><span className="field__label">문의 내용 <i className="req">*</i></span><textarea className="field__textarea inquiry-form__textarea" name="content" required maxLength={2000} placeholder="문의하실 내용을 자세히 입력해 주세요" /></label>
          <label className="check inquiry-form__privacy"><input type="checkbox" required checked={agreed} onChange={(e) => setAgreed(e.target.checked)} /><span><b>개인정보 수집 및 이용에 동의합니다.</b><small>문의 답변을 위해 이름, 이메일, 연락처를 수집하며 답변 완료 후 관련 기준에 따라 보관·파기합니다.</small></span></label>
          <div className="inquiry-form__actions"><Button type="button" variant="ghost" onClick={() => navigate(-1)}>취소</Button><Button type="submit" disabled={!agreed}>문의 접수</Button></div>
        </form>
      </section>
    </main>
  );
}
