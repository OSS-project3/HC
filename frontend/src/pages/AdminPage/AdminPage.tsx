// Admin page: application records table (admin-only, redirects otherwise).
import { Fragment, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { adminStatusLabels, loadApplications, saveApplications, type AdminApplication, type AdminApplicationDetail, type AdminStatus } from "../../data/adminMock";
import { loadInquiries, saveInquiries, type InquiryRecord } from "../../data/inquiries";
import { EventFeedAdminPanel } from "../../components/admin/EventFeedAdminPanel";
import { showToast } from "../../components/ui/toast";
import { boothPosts, collabPosts, loadFeedPosts, saveFeedPosts, type FeedPost } from "../../data/eventFeedPosts";
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
  const [managedBoothPosts, setManagedBoothPosts] = useState(() => loadFeedPosts("booth-posts", boothPosts));
  const [managedCollabPosts, setManagedCollabPosts] = useState(() => loadFeedPosts("collab-posts", collabPosts));
  const [openInquiryId, setOpenInquiryId] = useState<string | null>(null);
  const [openAppNumber, setOpenAppNumber] = useState<string | null>(null);
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
  const updateBoothPosts = (items: FeedPost[]) => { saveFeedPosts("booth-posts", items); setManagedBoothPosts(items); };
  const updateCollabPosts = (items: FeedPost[]) => { saveFeedPosts("collab-posts", items); setManagedCollabPosts(items); };

  const [answerDraft, setAnswerDraft] = useState<Record<string, string>>({});
  const answerInquiry = (id: string) => {
    const text = (answerDraft[id] ?? "").trim();
    if (!text) {
      showToast("답변 내용을 입력해 주세요.");
      return;
    }
    setInquiries((items) => {
      const updated = items.map((item) =>
        item.id === id
          ? { ...item, answer: text, answeredAt: new Date().toISOString(), status: "COMPLETED" as InquiryRecord["status"] }
          : item,
      );
      saveInquiries(updated);
      return updated;
    });
    showToast("답변이 저장되었습니다. 문의 상태가 완료로 변경됩니다.");
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
              <Fragment key={a.applicationNumber}>
                <tr className={openAppNumber === a.applicationNumber ? "is-open" : undefined}>
                  <td className="admin__mono">
                    <button className="admin__app-number" onClick={() => setOpenAppNumber(openAppNumber === a.applicationNumber ? null : a.applicationNumber)} aria-expanded={openAppNumber === a.applicationNumber}>{a.applicationNumber}</button>
                  </td>
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
                {openAppNumber === a.applicationNumber && (
                  <tr className="admin__detail-row">
                    <td colSpan={8}><ApplicationDetail application={a} /></td>
                  </tr>
                )}
              </Fragment>
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
                  <td><button className="admin__inquiry-title" onClick={() => setOpenInquiryId(openInquiryId === inquiry.id ? null : inquiry.id)} aria-expanded={openInquiryId === inquiry.id}>{inquiry.title}</button>{openInquiryId === inquiry.id && <div className="admin__inquiry-content"><b>문의 내용</b><p>{inquiry.content}</p><b>답변 작성</b><textarea className="admin__inquiry-answer" rows={4} placeholder="답변 내용을 입력하세요." value={answerDraft[inquiry.id] ?? inquiry.answer ?? ""} onChange={(e) => setAnswerDraft((draft) => ({ ...draft, [inquiry.id]: e.target.value }))} /><div className="admin__inquiry-answer-foot"><button type="button" className="admin__inquiry-answer-save" onClick={() => answerInquiry(inquiry.id)}>{inquiry.answeredAt ? "답변 수정" : "답변 저장"}</button>{inquiry.answeredAt && <span className="admin__inquiry-answered">답변 완료 · {new Date(inquiry.answeredAt).toLocaleString("ko-KR")}</span>}</div></div>}</td>
                  <td>{inquiry.name}</td><td>{inquiry.email}</td><td className="admin__mono">{inquiry.phone}</td><td>{new Date(inquiry.createdAt).toLocaleDateString("ko-KR")}</td>
                  <td><StatusMenu id={`inquiry-${inquiry.id}`} value={inquiry.status} options={[{ value: "PENDING", label: "답변 대기", className: "is-waiting" }, { value: "COMPLETED", label: "문의 완료", className: "is-completed" }]} open={openStatusMenu === `inquiry-${inquiry.id}`} onToggle={() => setOpenStatusMenu(openStatusMenu === `inquiry-${inquiry.id}` ? null : `inquiry-${inquiry.id}`)} onChange={(status) => { changeInquiryStatus(inquiry.id, status as InquiryRecord["status"]); setOpenStatusMenu(null); }} /></td>
                </tr>
              ))}
              {inquiries.length === 0 && <tr><td className="admin__empty" colSpan={7}>접수된 문의가 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      <section className="admin__event-posts">
        <div className="admin__section-head">
          <div><p className="eyebrow">행사사업</p><h2>부스 운영 · 법인단체 협업 게시글</h2></div>
          <strong>총 {managedBoothPosts.length + managedCollabPosts.length}건</strong>
        </div>
        <EventFeedAdminPanel label="부스 운영 게시글" items={managedBoothPosts} onChange={updateBoothPosts} compact />
        <EventFeedAdminPanel label="법인·단체 협업 게시글" items={managedCollabPosts} onChange={updateCollabPosts} showCompanyFields compact />
      </section>
    </section>
  );
}

function ApplicationDetail({ application }: { application: AdminApplication }) {
  const d: AdminApplicationDetail = application.detail ?? {};
  const isOrg = application.applicantType === "법인·단체";
  // Only render rows that actually have a value (demo records carry no detail).
  const rows: [string, string | undefined][] = [
    ["신청 유형", application.applicantType],
    ["카드 종류", application.cardType],
    ["발급 방식", d.issuanceMethod],
    [isOrg ? "담당자 이름" : "이름", application.applicantName],
    isOrg ? ["법인·단체/학교명", d.organizationName] : ["영문 이름", d.englishName],
    ["국적", d.nationality],
    ["출생지역", d.birthPlace],
    ["생년월일", d.birthDate],
    ["출생시간", d.birthTime],
    ["성별", d.gender],
    ["한국입국일", d.koreaEntryDate],
    ["학교 구분", d.schoolLevel],
    ["학교명", d.schoolName],
    ["학번", d.studentNumber],
    ["학과", d.department],
    ["연락처", application.phone],
    ["이메일", d.email ?? application.applicantEmail],
    ["수량", `${application.quantity}매`],
    ["수령인", d.recipientName],
    ["수령인 연락처", d.recipientPhone],
    ["배송지", d.recipientAddress],
    ["배송 요청사항", d.deliveryRequest],
    ["접수일", application.submittedAt],
  ];
  const visible = rows.filter(([, value]) => value != null && value !== "");
  return (
    <div className="admin__detail">
      <b className="admin__detail-title">신청 세부 내역</b>
      {!application.detail && <p className="admin__detail-note">이 신청은 세부 내역이 저장되기 전에 접수되어 요약 정보만 표시됩니다.</p>}
      <dl className="admin__detail-grid">
        {visible.map(([label, value]) => (
          <div className="admin__detail-item" key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
    </div>
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
