// Admin page: application records table (admin-only, redirects otherwise).
import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { adminStatusLabels, loadApplications, saveApplications, type AdminApplication, type AdminStatus } from "../../data/adminMock";
import { loadInquiries, saveInquiries, type InquiryRecord } from "../../data/inquiries";
import "./AdminPage.css";

const statusClass: Record<string, string> = {
  SUBMITTED: "is-submitted",
  CONSULTING: "is-consulting",
  PAYMENT_PENDING: "is-pending",
  IN_PRODUCTION: "is-production",
  COMPLETED: "is-completed",
  CANCELLED: "is-cancelled",
};

export function AdminPage() {
  const { isAdmin } = useAuth();
  const [applications, setApplications] = useState<AdminApplication[]>(loadApplications);
  const [inquiries, setInquiries] = useState(loadInquiries);
  const [openInquiryId, setOpenInquiryId] = useState<string | null>(null);
  const [openStatusMenu, setOpenStatusMenu] = useState<string | null>(null);
  useEffect(() => {
    if (!openStatusMenu) return;
    const close = () => setOpenStatusMenu(null);
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === "Escape") close(); };
    document.addEventListener("click", close);
    document.addEventListener("keydown", closeOnEscape);
    return () => { document.removeEventListener("click", close); document.removeEventListener("keydown", closeOnEscape); };
  }, [openStatusMenu]);
  const stats = [
    { label: "전체 신청", value: applications.length },
    { label: "제작 중", value: applications.filter((a) => a.status === "IN_PRODUCTION").length },
    { label: "입금 대기", value: applications.filter((a) => a.status === "PAYMENT_PENDING").length },
    { label: "발급 완료", value: applications.filter((a) => a.status === "COMPLETED").length },
  ];

  const changeStatus = (applicationNumber: string, status: AdminStatus) => {
    setApplications((items) => {
      const updated = items.map((item) => item.applicationNumber === applicationNumber ? { ...item, status } : item);
      saveApplications(updated);
      return updated;
    });
  };

  const changeInquiryStatus = (id: string, status: InquiryRecord["status"]) => {
    setInquiries((items) => {
      const updated = items.map((item) => item.id === id ? { ...item, status } : item);
      saveInquiries(updated);
      return updated;
    });
  };

  const [answerDraft, setAnswerDraft] = useState<Record<string, string>>({});
  const answerInquiry = (id: string) => {
    const text = (answerDraft[id] ?? "").trim();
    setInquiries((items) => {
      const updated = items.map((item) =>
        item.id === id
          ? { ...item, answer: text, answeredAt: new Date().toISOString(), status: (text ? "COMPLETED" : item.status) as InquiryRecord["status"] }
          : item,
      );
      saveInquiries(updated);
      return updated;
    });
  };

  // Front-end guard only — the server must also enforce admin access.
  if (!isAdmin) return <Navigate to="/login?returnTo=%2Fadmin" replace />;

  return (
    <section className="admin page-container">
      <header className="admin__head subpage-hero">
        <p className="eyebrow">관리</p>
        <h1 className="admin__title subpage-hero__title">신청 관리</h1>
        <p className="section-lead">제작 신청 현황을 확인하고 상태를 관리합니다. (데모 데이터)</p>
      </header>

      <div className="admin__stats">
        {stats.map((s) => (
          <div className="admin__stat" key={s.label}>
            <span className="admin__stat-value">{s.value}</span>
            <span className="admin__stat-label">{s.label}</span>
          </div>
        ))}
      </div>

      <div className="admin__table-wrap">
        <table className="admin__table">
          <thead>
            <tr>
              <th>신청번호</th>
              <th>구분</th>
              <th>카드 종류</th>
              <th>신청인</th>
              <th>연락처</th>
              <th>수량</th>
              <th>상태</th>
              <th>접수일</th>
            </tr>
          </thead>
          <tbody>
            {applications.map((a) => (
              <tr key={a.applicationNumber}>
                <td className="admin__mono">{a.applicationNumber}</td>
                <td>{a.applicantType}</td>
                <td>{a.cardType}</td>
                <td>{a.applicantName}</td>
                <td className="admin__mono">{a.phone}</td>
                <td>{a.quantity}매</td>
                <td>
                  <StatusMenu id={`application-${a.applicationNumber}`} value={a.status} options={(Object.keys(adminStatusLabels) as AdminStatus[]).map((status) => ({ value: status, label: adminStatusLabels[status], className: statusClass[status] }))} open={openStatusMenu === `application-${a.applicationNumber}`} onToggle={() => setOpenStatusMenu(openStatusMenu === `application-${a.applicationNumber}` ? null : `application-${a.applicationNumber}`)} onChange={(status) => { changeStatus(a.applicationNumber, status as AdminStatus); setOpenStatusMenu(null); }} />
                </td>
                <td>{a.submittedAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <section className="admin__inquiries">
        <div className="admin__section-head">
          <div><p className="eyebrow">고객지원</p><h2>1:1 문의 내역</h2></div>
          <strong>총 {inquiries.length}건</strong>
        </div>
        <div className="admin__table-wrap admin__inquiry-table-wrap">
          <table className="admin__table admin__inquiry-table">
            <thead><tr><th>문의 유형</th><th>제목</th><th>문의자</th><th>이메일</th><th>연락처</th><th>접수일</th><th>처리 상태</th></tr></thead>
            <tbody>
              {inquiries.map((inquiry) => (
                <tr key={inquiry.id} className={openInquiryId === inquiry.id ? "is-open" : undefined}>
                  <td><span className="admin__badge is-inquiry">{inquiry.category}</span></td>
                  <td><button className="admin__inquiry-title" onClick={() => setOpenInquiryId(openInquiryId === inquiry.id ? null : inquiry.id)} aria-expanded={openInquiryId === inquiry.id}>{inquiry.title}</button>{openInquiryId === inquiry.id && <div className="admin__inquiry-content"><b>문의 내용</b><p>{inquiry.content}</p><b>답변</b><textarea className="admin__inquiry-answer" rows={4} placeholder="답변 내용을 입력하세요." value={answerDraft[inquiry.id] ?? inquiry.answer ?? ""} onChange={(e) => setAnswerDraft((draft) => ({ ...draft, [inquiry.id]: e.target.value }))} /><button type="button" className="admin__inquiry-answer-save" onClick={() => answerInquiry(inquiry.id)}>답변 저장</button></div>}</td>
                  <td>{inquiry.name}</td><td>{inquiry.email}</td><td className="admin__mono">{inquiry.phone}</td><td>{new Date(inquiry.createdAt).toLocaleDateString("ko-KR")}</td>
                  <td><StatusMenu id={`inquiry-${inquiry.id}`} value={inquiry.status} options={[{ value: "PENDING", label: "답변 대기", className: "is-waiting" }, { value: "COMPLETED", label: "문의 완료", className: "is-completed" }]} open={openStatusMenu === `inquiry-${inquiry.id}`} onToggle={() => setOpenStatusMenu(openStatusMenu === `inquiry-${inquiry.id}` ? null : `inquiry-${inquiry.id}`)} onChange={(status) => { changeInquiryStatus(inquiry.id, status as InquiryRecord["status"]); setOpenStatusMenu(null); }} /></td>
                </tr>
              ))}
              {inquiries.length === 0 && <tr><td className="admin__empty" colSpan={7}>접수된 문의가 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

interface StatusOption { value: string; label: string; className: string }

function StatusMenu({ id, value, options, open, onToggle, onChange }: { id: string; value: string; options: StatusOption[]; open: boolean; onToggle: () => void; onChange: (value: string) => void }) {
  const selected = options.find((option) => option.value === value) ?? options[0];
  return (
    <div className={`admin-status-menu${open ? " is-open" : ""}`} onClick={(event) => event.stopPropagation()}>
      <button className={`admin-status-menu__trigger ${selected.className}`} type="button" onClick={onToggle} aria-expanded={open} aria-controls={`${id}-menu`}><i aria-hidden="true" /><span>{selected.label}</span><b aria-hidden="true">⌄</b></button>
      {open && <div className="admin-status-menu__options" id={`${id}-menu`} role="listbox" aria-label="처리 상태 선택">
        {options.map((option) => <button key={option.value} type="button" role="option" aria-selected={option.value === value} className={option.value === value ? "is-selected" : undefined} onClick={() => onChange(option.value)}><i className={option.className} aria-hidden="true" /><span>{option.label}</span>{option.value === value && <b aria-hidden="true">✓</b>}</button>)}
      </div>}
    </div>
  );
}
