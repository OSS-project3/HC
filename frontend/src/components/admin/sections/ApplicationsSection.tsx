// 제작신청 관리 — 목록/상세는 실제 API(/api/admin/applications) 연결.
// 만세력·이름추천·이름확정(+1)·엑셀출력은 백엔드 미구현이라 UI만(mock). 설계: docs/specs/admin-dashboard/DESIGN.md
import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError, type AdminApplicationDetail, type AdminApplicationListItem, type ApplicationStatus, type ApplicationType } from "../../../services/api";
import { showToast } from "../../ui/toast";
import { getSelectionCounts, incrementSelection, mockRecommendations, mockSaju, type RecommendedName } from "../../../data/adminNamingMock";

const statusLabels: Record<ApplicationStatus, string> = {
  SUBMITTED: "접수", REVIEWING: "검토중", PHOTO_REJECTED: "사진반려", NAME_EDITING: "작명중",
  PRODUCTION_READY: "제작대기", PRODUCING: "제작중", COMPLETED: "발급완료", CANCELLED: "취소",
};

// 오행 아이콘용 — 전통 오행 색(목=청/화=적/토=황/금=백금속/수=흑청)을 CSS 클래스로 매핑.
const EL_KEY: Record<string, string> = { 목: "mok", 화: "hwa", 토: "to", 금: "geum", 수: "su" };
const EL_HANJA: Record<string, string> = { 목: "木", 화: "火", 토: "土", 금: "金", 수: "水" };

export function ApplicationsSection() {
  const [tab, setTab] = useState<ApplicationType>("INDIVIDUAL");
  const [all, setAll] = useState<AdminApplicationListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await api.listAdminApplications({ size: 100 });
      setAll(page.content);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "신청 목록을 불러오지 못했습니다. 관리자 권한(서버 인증)이 필요합니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const rows = useMemo(() => all.filter((a) => a.applicationType === tab), [all, tab]);
  const switchTab = (next: ApplicationType) => { setTab(next); setOpenId(null); setSelected(new Set()); };

  const toggleSelect = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const allSelected = rows.length > 0 && rows.every((r) => selected.has(r.applicationId));
  const toggleAll = () => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (rows.every((r) => next.has(r.applicationId))) {
        rows.forEach((r) => next.delete(r.applicationId));
      } else {
        rows.forEach((r) => next.add(r.applicationId));
      }
      return next;
    });
  };

  const exportExcel = () => {
    const ids = tab === "INDIVIDUAL" ? [...selected] : rows.map((r) => r.applicationId);
    if (ids.length === 0) { showToast("내보낼 신청을 선택해 주세요."); return; }
    // 🟡 API 미구현: POST /api/admin/applications/export (설계문서 §2.4). 지금은 안내만 노출.
    showToast(`엑셀 내보내기(${ids.length}건)는 백엔드 미구현입니다. 설계문서(DESIGN.md §2.4)를 참고하세요.`);
  };

  return (
    <div className="admin-panel admin-apps">
      <div className="admin-panel__head">
        <div><p className="eyebrow">제작신청</p><h2 className="admin-panel__title">제작신청 관리</h2></div>
      </div>

      <div className="admin-tabs">
        <button className={`admin-tabs__tab${tab === "INDIVIDUAL" ? " is-active" : ""}`} onClick={() => switchTab("INDIVIDUAL")}>
          개인 신청 <span className="admin-tabs__count">{all.filter((a) => a.applicationType === "INDIVIDUAL").length}</span>
        </button>
        <button className={`admin-tabs__tab${tab === "GROUP" ? " is-active" : ""}`} onClick={() => switchTab("GROUP")}>
          단체 신청 <span className="admin-tabs__count">{all.filter((a) => a.applicationType === "GROUP").length}</span>
        </button>
      </div>

      <div className="admin-panel__toolbar">
        <p className="admin__muted">
          {tab === "INDIVIDUAL" ? "행을 펼쳐 만세력·추천 이름을 확인하고 이름을 확정합니다. 여러 건을 선택해 한 엑셀로 내보낼 수 있습니다." : "단체 엑셀 값을 읽어 행마다 이름을 추천합니다."}
        </p>
        <button type="button" className="admin__btn admin__btn--primary" onClick={exportExcel}>
          {tab === "INDIVIDUAL" ? `선택 ${selected.size}건 엑셀 내보내기` : "전체 엑셀 내보내기"}
        </button>
      </div>

      {loading && <p className="admin-panel__note">불러오는 중…</p>}
      {error && <p className="admin-panel__note admin-panel__note--error">{error}</p>}

      {!loading && !error && (
        <div className="admin__table-wrap">
          <table className="admin__table">
            <thead>
              <tr>
                {tab === "INDIVIDUAL" && (
                  <th className="admin-apps__check">
                    <label className="admin-apps__checkbox">
                      <input type="checkbox" checked={allSelected} onChange={toggleAll} aria-label="전체 선택" />
                    </label>
                  </th>
                )}
                <th>신청번호</th><th>카드 종류</th><th>수량</th><th>상태</th><th>결제</th><th>접수일</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((a) => (
                <Fragment key={a.applicationId}>
                  <tr className={openId === a.applicationId ? "is-open" : undefined}>
                    {tab === "INDIVIDUAL" && (
                      <td className="admin-apps__check">
                        <label className="admin-apps__checkbox">
                          <input type="checkbox" checked={selected.has(a.applicationId)} onChange={() => toggleSelect(a.applicationId)} aria-label="엑셀 대상 선택" />
                        </label>
                      </td>
                    )}
                    <td className="admin__mono"><button className="admin__linklike" onClick={() => setOpenId(openId === a.applicationId ? null : a.applicationId)} aria-expanded={openId === a.applicationId}>{a.applicationNumber}</button></td>
                    <td>{a.cardTypeName}</td>
                    <td>{a.totalQuantity}매</td>
                    <td><span className="admin__badge">{statusLabels[a.status]}</span></td>
                    <td>{a.paymentStatus === "CONFIRMED" ? "완료" : "대기"}</td>
                    <td>{new Date(a.createdAt).toLocaleDateString("ko-KR")}</td>
                  </tr>
                  {openId === a.applicationId && (
                    <tr className="admin__detail-row">
                      <td colSpan={tab === "INDIVIDUAL" ? 7 : 6}><ApplicationNaming app={a} /></td>
                    </tr>
                  )}
                </Fragment>
              ))}
              {rows.length === 0 && <tr><td className="admin__empty" colSpan={tab === "INDIVIDUAL" ? 7 : 6}>{tab === "INDIVIDUAL" ? "개인" : "단체"} 신청 내역이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !error && rows.length === 0 && (
        <div className="admin-naming" style={{ marginTop: 14 }}>
          <p className="admin-naming__mock-banner">
            실제 {tab === "INDIVIDUAL" ? "개인" : "단체"} 신청 데이터가 없어 작명 플로우를 <b>예시(mock)</b>로 미리 봅니다.
            실제 신청이 들어오면 위 목록의 행을 펼쳐 동일하게 동작합니다.
          </p>
          {tab === "INDIVIDUAL"
            ? <NamingCard label="예시 신청인" seed="preview-individual" />
            : ["예시 멤버 1", "예시 멤버 2", "예시 멤버 3"].map((m, i) => <NamingCard key={m} label={m} seed={`preview-group-${i}`} />)}
        </div>
      )}
    </div>
  );
}

// 신청 상세 + 작명 플로우. 상세는 실제 API, 작명은 mock.
function ApplicationNaming({ app }: { app: AdminApplicationListItem }) {
  const [detail, setDetail] = useState<AdminApplicationDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    api.getAdminApplication(app.applicationId)
      .then((d) => { if (alive) setDetail(d); })
      .catch((e) => { if (alive) setError(e instanceof ApiError ? e.message : "상세를 불러오지 못했습니다."); });
    return () => { alive = false; };
  }, [app.applicationId]);

  if (error) return <p className="admin-panel__note admin-panel__note--error">{error}</p>;
  if (!detail) return <p className="admin-panel__note">상세 불러오는 중…</p>;

  const isGroup = detail.applicationType === "GROUP";
  // 단체는 멤버 목록 API가 없어(설계문서 §4) 시연용 mock 멤버로 대체한다.
  const memberSeeds = isGroup
    ? Array.from({ length: Math.min(detail.memberCount || 0, 5) }, (_, i) => ({ label: `멤버 ${i + 1}`, seed: `${detail.applicationNumber}-m${i + 1}` }))
    : [{ label: detail.applicant.name || "신청인", seed: detail.applicationNumber }];

  return (
    <div className="admin-naming">
      <div className="admin-naming__info">
        <b className="admin-naming__subtitle">신청 정보</b>
        <dl className="admin__detail-grid">
          <Item label="신청 유형" value={isGroup ? "단체" : "개인"} />
          <Item label="카드 종류" value={detail.cardTypeName} />
          <Item label="발급 방식" value={detail.issueType === "MOBILE_AND_PHYSICAL" ? "모바일+실물" : "모바일"} />
          <Item label={isGroup ? "담당자" : "신청인"} value={detail.applicant.name} />
          <Item label="이메일" value={detail.applicant.email} />
          <Item label="연락처" value={detail.applicant.phone} />
          {isGroup && <Item label="인원" value={`${detail.memberCount}명`} />}
        </dl>
      </div>

      {isGroup && (
        <p className="admin-naming__mock-banner">
          ⚠️ 단체 멤버(엑셀 행) 목록 조회 API가 아직 없어, 아래 멤버·만세력·추천 이름은 <b>시연용 mock</b>입니다.
          실제 연동 설계는 DESIGN.md(§2·§4)를 참고하세요.
        </p>
      )}

      {memberSeeds.map((m) => <NamingCard key={m.seed} label={m.label} seed={m.seed} />)}
    </div>
  );
}

function NamingCard({ label, seed }: { label: string; seed: string }) {
  const saju = useMemo(() => mockSaju(seed), [seed]);
  const recs = useMemo(() => mockRecommendations(seed, saju), [seed, saju]);
  const [chosen, setChosen] = useState<string | null>(null);
  const [counts, setCounts] = useState<Record<string, number>>(() => getSelectionCounts());

  const choose = (name: RecommendedName) => {
    const next = incrementSelection(name.id);
    setCounts((prev) => ({ ...prev, [name.id]: next }));
    setChosen(name.id);
    showToast(`"${name.name}(${name.hanja})" 이름을 확정했습니다. (선택 이력 +1 · mock)`);
  };

  return (
    <div className="admin-naming__card">
      <div className="admin-naming__card-head">
        <b>{label}</b>
        <span className="admin__muted">만세력 · 추천 이름 <em className="admin-naming__mock-tag">mock</em></span>
      </div>

      <div className="admin-naming__saju">
        <table className="admin-naming__pillars">
          <thead><tr><th></th><th>시주</th><th>일주</th><th>월주</th><th>년주</th></tr></thead>
          <tbody>
            <tr><th>천간</th><td>{saju.pillars.hour.stem}</td><td>{saju.pillars.day.stem}</td><td>{saju.pillars.month.stem}</td><td>{saju.pillars.year.stem}</td></tr>
            <tr><th>지지</th><td>{saju.pillars.hour.branch}</td><td>{saju.pillars.day.branch}</td><td>{saju.pillars.month.branch}</td><td>{saju.pillars.year.branch}</td></tr>
          </tbody>
        </table>
        <div className="admin-naming__elements">
          {(["목", "화", "토", "금", "수"] as const).map((el) => (
            <span
              key={el}
              className={`admin-naming__el el-${EL_KEY[el]}${saju.missing.includes(el) ? " is-missing" : ""}`}
              title={`${el}(${EL_HANJA[el]}) ${saju.elementCounts[el]}개`}
            >
              <i className="admin-naming__el-icon" aria-hidden="true">{el}</i>
              <b className="admin-naming__el-count">{saju.elementCounts[el]}</b>
            </span>
          ))}
          {saju.missing.length > 0 && <span className="admin-naming__missing-note">결핍: {saju.missing.join("·")} → 보완 이름 우선 추천</span>}
        </div>
      </div>

      <ul className="admin-naming__recs">
        {recs.map((n) => (
          <li key={n.id} className={`admin-naming__rec${chosen === n.id ? " is-chosen" : ""}`}>
            <div className="admin-naming__rec-main">
              <b>{n.name}</b> <span className="admin__muted">{n.hanja}</span>
              <span className="admin-naming__rec-el">{n.elements.join("·")}</span>
            </div>
            <div className="admin-naming__rec-sub">{n.reading} — {n.meaning}</div>
            <div className="admin-naming__rec-foot">
              <span className="admin__muted">선택 이력 {counts[n.id] ?? 0}회</span>
              <button type="button" className={`admin__btn ${chosen === n.id ? "admin__btn--chosen" : "admin__btn--primary"}`} onClick={() => choose(n)}>
                {chosen === n.id ? "✓ 확정됨" : "이 이름 선택"}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Item({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return <div className="admin__detail-item"><dt>{label}</dt><dd>{value}</dd></div>;
}
