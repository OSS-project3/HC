// Admin page: application records table (admin-only, redirects otherwise).
import { Navigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { adminApplications, adminStats, adminStatusLabels } from "../data/adminMock";
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

  // Front-end guard only — the server must also enforce admin access.
  if (!isAdmin) return <Navigate to="/login" replace />;

  return (
    <section className="admin page-container">
      <header className="admin__head">
        <p className="eyebrow">관리</p>
        <h1 className="admin__title">신청 관리</h1>
        <p className="section-lead">제작 신청 현황을 확인하고 상태를 관리합니다. (데모 데이터)</p>
      </header>

      <div className="admin__stats">
        {adminStats.map((s) => (
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
            {adminApplications.map((a) => (
              <tr key={a.applicationNumber}>
                <td className="admin__mono">{a.applicationNumber}</td>
                <td>{a.applicantType}</td>
                <td>{a.cardType}</td>
                <td>{a.applicantName}</td>
                <td className="admin__mono">{a.phone}</td>
                <td>{a.quantity}매</td>
                <td>
                  <span className={`admin__badge ${statusClass[a.status]}`}>{adminStatusLabels[a.status]}</span>
                </td>
                <td>{a.submittedAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
