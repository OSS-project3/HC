// 관리자 대시보드. 좌측 사이드바로 섹션을 전환한다. API가 있는 영역은 실제 연결,
// 없는 영역(제작신청 작명/만세력/엑셀)은 UI만(mock) — docs/specs/admin-dashboard/DESIGN.md 참고.
// 클라이언트 가드일 뿐이며 서버가 /api/admin/**를 ADMIN으로 재검증한다.
import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { api, ApiError } from "../../services/api";
import { EventAdminPanel } from "../../components/admin/EventAdminPanel";
import { ApplicationsSection } from "../../components/admin/sections/ApplicationsSection";
import { BoardsSection } from "../../components/admin/sections/BoardsSection";
import { ReviewsSection } from "../../components/admin/sections/ReviewsSection";
import { InquiriesSection } from "../../components/admin/sections/InquiriesSection";
import { SchoolTemplateSection } from "../../components/admin/sections/SchoolTemplateSection";
import "./AdminPage.css";

type SectionId = "overview" | "applications" | "templates" | "notice" | "faq" | "events" | "reviews" | "inquiries";

const SECTIONS: { id: SectionId; label: string; group: string }[] = [
  { id: "overview", label: "개요", group: "" },
  { id: "applications", label: "제작신청 관리", group: "신청" },
  { id: "templates", label: "학생증 템플릿", group: "신청" },
  { id: "notice", label: "공지사항", group: "콘텐츠" },
  { id: "faq", label: "FAQ", group: "콘텐츠" },
  { id: "events", label: "행사사업", group: "콘텐츠" },
  { id: "reviews", label: "후기", group: "콘텐츠" },
  { id: "inquiries", label: "고객지원(문의)", group: "콘텐츠" },
];

export function AdminPage() {
  const { isAdmin } = useAuth();
  const [section, setSection] = useState<SectionId>(() => {
    const hash = window.location.hash.replace("#", "") as SectionId;
    return SECTIONS.some((s) => s.id === hash) ? hash : "overview";
  });

  useEffect(() => { window.location.hash = section; }, [section]);

  // Front-end guard only — the server must also enforce admin access.
  if (!isAdmin) return <Navigate to="/login?returnTo=%2Fadmin" replace />;

  const groups = Array.from(new Set(SECTIONS.map((s) => s.group)));

  return (
    <div className="admin-dash page-container">
      <aside className="admin-dash__side">
        <div className="admin-dash__brand"><span className="admin-dash__brand-badge">관리</span><b>대시보드</b></div>
        <nav className="admin-dash__nav">
          {groups.map((g) => (
            <div key={g || "_"} className="admin-dash__nav-group">
              {g && <p className="admin-dash__nav-label">{g}</p>}
              {SECTIONS.filter((s) => s.group === g).map((s) => (
                <button key={s.id} className={`admin-dash__nav-item${section === s.id ? " is-active" : ""}`} onClick={() => setSection(s.id)}>
                  {s.label}
                </button>
              ))}
            </div>
          ))}
        </nav>
      </aside>

      <main className="admin-dash__main">
        {section === "overview" && <OverviewSection onGo={setSection} />}
        {section === "applications" && <ApplicationsSection />}
        {section === "templates" && <SchoolTemplateSection />}
        {section === "notice" && <BoardsSection boardType="NOTICE" />}
        {section === "faq" && <BoardsSection boardType="FAQ" />}
        {section === "events" && (
          <div className="admin-panel">
            <div className="admin-panel__head"><div><p className="eyebrow">행사사업</p><h2 className="admin-panel__title">부스 운영 · 법인단체 협업</h2></div></div>
            <EventAdminPanel label="부스 운영 게시글" eventType="BOOTH" />
            <EventAdminPanel label="법인·단체 협업 게시글" eventType="COLLABORATION" />
          </div>
        )}
        {section === "reviews" && <ReviewsSection />}
        {section === "inquiries" && <InquiriesSection />}
      </main>
    </div>
  );
}

// 개요: 실제 API에서 집계. 서버 관리자 세션이 없으면(하드코딩 로그인) 401이 나며 안내를 표시한다.
function OverviewSection({ onGo }: { onGo: (s: SectionId) => void }) {
  const [stats, setStats] = useState<{ applications: number | null; individual: number | null; group: number | null; inquiries: number | null; pendingInquiries: number | null; completedInquiries: number | null }>({
    applications: null, individual: null, group: null, inquiries: null, pendingInquiries: null, completedInquiries: null,
  });
  const [warn, setWarn] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const data = await api.getAdminStats();
        if (!alive) return;
        setStats({
          applications: data.totalApplications,
          individual: data.individualApplications,
          group: data.groupApplications,
          inquiries: data.totalInquiries,
          pendingInquiries: data.pendingInquiries,
          completedInquiries: data.completedInquiries,
        });
      } catch (e) {
        if (alive) setWarn(e instanceof ApiError && e.status === 401
          ? "서버 관리자 세션이 없어 실시간 집계를 불러오지 못했습니다. (임시 하드코딩 로그인은 서버 토큰이 없습니다 — TEMP_ADMIN_LOGIN.md 참고)"
          : "집계를 불러오지 못했습니다.");
      }
    })();
    return () => { alive = false; };
  }, []);

  const cards = [
    { label: "전체 신청", value: stats.applications, go: "applications" as const },
    { label: "개인 신청", value: stats.individual, go: "applications" as const },
    { label: "단체 신청", value: stats.group, go: "applications" as const },
    { label: "1:1 문의", value: stats.inquiries, go: "inquiries" as const },
  ];

  return (
    <div className="admin-panel">
      <div className="admin-panel__head"><div><p className="eyebrow">개요</p><h2 className="admin-panel__title">대시보드</h2></div></div>
      {warn && <p className="admin-panel__note admin-panel__note--warn">{warn}</p>}
      <div className="admin-dash__stats">
        {cards.map((c) => (
          <button key={c.label} className="admin-dash__stat" onClick={() => onGo(c.go)}>
            <span className="admin-dash__stat-value">{c.value ?? "—"}</span>
            <span className="admin-dash__stat-label">{c.label}</span>
          </button>
        ))}
      </div>
      <p className="admin__muted admin-dash__hint">
        좌측 메뉴에서 공지·FAQ·행사·후기·문의를 관리하고, 제작신청 관리에서 개인/단체 신청을 확인·작명합니다.
        작명·카드 제작·학생증 템플릿 업로드는 관리자 API와 연결되어 있으며, 일부 항목은 신청 상태와 생성된 카드 파일 상태에 따라 제한됩니다.
      </p>
    </div>
  );
}
