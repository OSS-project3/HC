// 제작신청 관리 — 목록/상세/구성원은 실제 API. 만세력은 실제 계산(manseryeok).
// 이름 확정·선택이력은 **백엔드 저장**(프론트 localStorage 미사용). 추천 이름 데이터만 프론트 번들.
import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError, type AdminApplicationDetail, type AdminApplicationListItem, type AdminApplicationMember, type ApplicationStatus, type ApplicationType } from "../../../services/api";
import { showToast } from "../../ui/toast";
import { mockRecommendations, mockSaju, type MockSaju, type RecommendedName } from "../../../data/adminNamingMock";
import { computeMemberSaju } from "../../../lib/saju";

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
                      <td colSpan={tab === "INDIVIDUAL" ? 7 : 6}><ApplicationNaming app={a} onChanged={load} /></td>
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
            실제 {tab === "INDIVIDUAL" ? "개인" : "단체"} 신청 데이터가 없어 작명 플로우를 <b>예시</b>로 미리 봅니다.
            실제 신청이 들어오면 위 목록의 행을 펼쳐 동일하게 동작합니다.
          </p>
          {(tab === "INDIVIDUAL" ? [0] : [0, 1, 2]).map((i) => (
            <NamingCard
              key={i}
              appId={0}
              index={i}
              isGroup={tab === "GROUP"}
              counts={{}}
              onSaved={async () => {}}
              member={{
                memberId: -1 - i,
                englishName: tab === "INDIVIDUAL" ? "예시 신청인" : `예시 멤버 ${i + 1}`,
                nationality: ["미국", "일본", "베트남"][i] ?? "미국",
                gender: i % 2 === 0 ? "MALE" : "FEMALE",
                birthDate: ["1994-03-15", "1999-08-22", "1987-12-05"][i] ?? "1996-05-20",
                birthTime: ["08:10", "14:40", "21:05"][i] ?? "12:00",
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function genderLabel(g?: "MALE" | "FEMALE"): string | undefined {
  return g === "MALE" ? "남성" : g === "FEMALE" ? "여성" : undefined;
}

// 신청 상세 + 구성원 작명 플로우 + 상태 전이(모두 실제 API), 만세력은 실제 계산.
function ApplicationNaming({ app, onChanged }: { app: AdminApplicationListItem; onChanged?: () => void | Promise<void> }) {
  const [detail, setDetail] = useState<AdminApplicationDetail | null>(null);
  const [members, setMembers] = useState<AdminApplicationMember[] | null>(null);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [statusBusy, setStatusBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reloadMembers = useCallback(async () => {
    setMembers(await api.getAdminApplicationMembers(app.applicationId));
  }, [app.applicationId]);

  const reloadStats = useCallback(async () => {
    try {
      const stats = await api.getNameSelectionStats();
      setCounts(Object.fromEntries(stats.map((s) => [`${s.name}|${s.hanja}`, s.count])));
    } catch { /* 통계 실패는 카운트 0으로 표시 */ }
  }, []);

  useEffect(() => {
    let alive = true;
    Promise.all([api.getAdminApplication(app.applicationId), api.getAdminApplicationMembers(app.applicationId)])
      .then(([d, m]) => { if (alive) { setDetail(d); setMembers(m); } })
      .catch((e) => { if (alive) setError(e instanceof ApiError ? e.message : "상세를 불러오지 못했습니다."); });
    void reloadStats();
    return () => { alive = false; };
  }, [app.applicationId, reloadStats]);

  if (error) return <p className="admin-panel__note admin-panel__note--error">{error}</p>;
  if (!detail || !members) return <p className="admin-panel__note">상세 불러오는 중…</p>;

  const isGroup = detail.applicationType === "GROUP";
  const first = members[0];
  const onSaved = async () => { await Promise.all([reloadMembers(), reloadStats()]); };

  // 백엔드에 존재하는 상태 전이 API를 현재 상태에 맞춰 노출한다. call()이 null이면(입력 취소) 건너뛴다.
  const runStatus = async (label: string, call: () => Promise<{ status: ApplicationStatus }> | null) => {
    const p = call();
    if (!p) return;
    setStatusBusy(true);
    try {
      const res = await p;
      showToast(`${label} 완료 — 상태: ${statusLabels[res.status]}`);
      setDetail(await api.getAdminApplication(app.applicationId));
      await onChanged?.();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : `${label}에 실패했습니다.`);
    } finally {
      setStatusBusy(false);
    }
  };

  const s = detail.status;
  const statusActions: { label: string; danger?: boolean; call: () => Promise<{ status: ApplicationStatus }> | null }[] = [];
  if (s === "REVIEWING") statusActions.push({ label: "사진 반려", danger: true, call: () => { const r = window.prompt("사진 반려 사유를 입력하세요."); return r && r.trim() ? api.rejectApplicationPhoto(app.applicationId, r.trim()) : null; } });
  if (s === "NAME_EDITING") statusActions.push({ label: "작명 완료 처리", call: () => api.completeNaming(app.applicationId) });
  if (s === "PRODUCTION_READY") statusActions.push({ label: "제작 시작", call: () => api.startProducing(app.applicationId) });
  if (s === "PRODUCING" && !detail.cardReadyAt) statusActions.push({ label: "카드 발급 완료", call: () => api.markCardReady(app.applicationId) });
  if (s === "PRODUCING" && detail.cardReadyAt && detail.issueType === "MOBILE_AND_PHYSICAL" && !detail.physicalDispatchedAt) statusActions.push({ label: "배송 발송(운송장 등록)", call: () => { const t = window.prompt("운송장 번호를 입력하세요."); return t && t.trim() ? api.dispatchApplication(app.applicationId, t.trim()) : null; } });

  return (
    <div className="admin-naming">
      <div className="admin-naming__info">
        <b className="admin-naming__subtitle">신청 정보</b>
        <dl className="admin__detail-grid">
          <Item label="신청 유형" value={isGroup ? "단체" : "개인"} />
          <Item label="카드 종류" value={detail.cardTypeName} />
          <Item label="발급 방식" value={detail.issueType === "MOBILE_AND_PHYSICAL" ? "모바일+실물" : "모바일"} />
          <Item label={isGroup ? "담당자" : "신청인"} value={detail.applicant.name} />
          {!isGroup && <Item label="출신 국가" value={first?.nationality} />}
          {!isGroup && <Item label="성별" value={genderLabel(first?.gender)} />}
          {!isGroup && <Item label="생년월일" value={first?.birthDate} />}
          <Item label="이메일" value={detail.applicant.email} />
          <Item label="연락처" value={detail.applicant.phone} />
          {isGroup && <Item label="인원" value={`${detail.memberCount}명`} />}
        </dl>
      </div>

      <div className="admin-naming__status">
        <span className="admin-naming__subtitle">상태 관리</span>
        <span className="admin__badge">{statusLabels[s]}</span>
        {detail.physicalDispatchedAt && <span className="admin__muted">발송됨</span>}
        <select
          className="field__select admin-naming__status-select"
          aria-label="상태 변경"
          value=""
          disabled={statusBusy || statusActions.length === 0}
          onChange={(e) => { const a = statusActions.find((x) => x.label === e.target.value); if (a) void runStatus(a.label, a.call); }}
        >
          <option value="" disabled>{statusActions.length ? "상태 변경 선택…" : "가능한 전이 없음"}</option>
          {statusActions.map((a) => <option key={a.label} value={a.label}>{a.label}</option>)}
        </select>
      </div>

      {members.map((m, i) => (
        <NamingCard key={m.memberId} appId={app.applicationId} index={i} member={m} isGroup={isGroup} counts={counts} onSaved={onSaved} />
      ))}
      {members.length === 0 && <p className="admin-panel__note">구성원 정보가 없습니다.</p>}
    </div>
  );
}

function NamingCard({ appId, index, member, isGroup, counts, onSaved }: {
  appId: number; index: number; member: AdminApplicationMember; isGroup: boolean;
  counts: Record<string, number>; onSaved: () => Promise<void>;
}) {
  const memberKey = `${appId}#${member.memberId}`;
  const label = member.englishName || (isGroup ? `멤버 ${index + 1}` : "신청인");
  // 실제 만세력(생년월일/시간). 계산 불가 시 mock로 폴백.
  const realSaju = useMemo(() => computeMemberSaju(member.birthDate, member.birthTime), [member.birthDate, member.birthTime]);
  const saju: MockSaju = useMemo(() => realSaju ?? mockSaju(memberKey), [realSaju, memberKey]);
  const [tick, setTick] = useState(0); // 새로고침 버튼: 증가 시 추천을 다시 뽑는다.
  const recs = useMemo(() => mockRecommendations(memberKey, saju), [memberKey, saju, tick]);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  // 확정 이름은 서버(member.assignedName)가 소스. 예시(preview) 멤버(음수 id)는 저장하지 않는다.
  const chosen = member.assignedName ? { name: member.assignedName, hanja: member.assignedHanja ?? "" } : null;
  const isPreview = member.memberId < 0;

  const choose = async (name: RecommendedName) => {
    if (isPreview) { showToast("예시 카드입니다. 실제 신청에서 서버에 저장됩니다."); return; }
    setSaving(true);
    try {
      await api.saveMemberName(appId, member.memberId, { name: name.name, hanja: name.hanja, reading: name.reading, meaning: name.meaning });
      showToast(`"${name.name}(${name.hanja})" 이름을 확정했습니다. (서버 저장 · 선택이력 +1)`);
      setEditing(false);
      await onSaved();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const metaLine = [member.nationality, genderLabel(member.gender), member.birthDate].filter(Boolean).join(" · ");

  // 이름 확정 후: 상태 '작명 완료' + 창을 접어(compact) 노출한다.
  if (chosen && !editing) {
    return (
      <div className="admin-naming__card is-done">
        <div className="admin-naming__card-head">
          <div className="admin-naming__head-left"><b>{label}</b><span className="admin__status-pill is-completed">작명 완료</span></div>
          <button type="button" className="admin__btn" onClick={() => setEditing(true)}>다시 선택</button>
        </div>
        <div className="admin-naming__done">
          <span className="admin-naming__done-name">{chosen.name}{chosen.hanja && <em>{chosen.hanja}</em>}</span>
          <span className="admin__muted">확정된 이름 (서버 저장)</span>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-naming__card">
      <div className="admin-naming__card-head">
        <div className="admin-naming__head-left">
          <b>{label}</b>
          <span className={`admin__status-pill ${chosen ? "is-completed" : "is-waiting"}`}>{chosen ? "작명 완료" : "접수"}</span>
        </div>
        <span className="admin__muted">{metaLine}{realSaju ? "" : (metaLine ? " · " : "") + "만세력 mock"}</span>
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

      <div className="admin-naming__recs-head">
        <b className="admin-naming__subtitle">추천 이름 {recs.length}</b>
        <button type="button" className="admin__btn admin-naming__refresh" onClick={() => setTick((t) => t + 1)}>
          <span aria-hidden="true">↻</span> 다른 이름 추천
        </button>
      </div>
      <ul className="admin-naming__recs">
        {recs.map((n) => (
          <li key={n.id} className="admin-naming__rec">
            <div className="admin-naming__rec-main">
              <b>{n.name}</b> <span className="admin__muted">{n.hanja}</span>
              <span className="admin-naming__rec-el">{n.elements.join("·")}</span>
            </div>
            <div className="admin-naming__rec-sub">{n.reading} — {n.meaning}</div>
            <div className="admin-naming__rec-foot">
              <span className="admin__muted">선택 이력 {counts[`${n.name}|${n.hanja}`] ?? 0}회</span>
              <button type="button" className="admin__btn admin__btn--primary" disabled={saving} onClick={() => choose(n)}>이 이름 선택</button>
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
