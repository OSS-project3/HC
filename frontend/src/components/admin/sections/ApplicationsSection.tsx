// 제작신청 관리 — 목록/상세/구성원은 실제 API. 만세력은 실제 계산(manseryeok).
// 이름 확정·선택이력은 **백엔드 저장**(프론트 localStorage 미사용). 추천 이름 데이터만 프론트 번들.
import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import {
  api,
  ApiError,
  type AdminApplicationDetail,
  type AdminApplicationListItem,
  type AdminApplicationMember,
  type ApplicationStatus,
  type ApplicationType,
  type BirthRegionCandidate,
  type CardDesignOption,
  type ManseryeokResolveResponse,
  type OffsetCandidate,
} from "../../../services/api";
import { showToast } from "../../ui/toast";
import { mockRecommendations, mockSaju, type MockSaju, type RecommendedName } from "../../../data/adminNamingMock";
import { computeMemberSaju, computeMemberSajuFromResolved, makeSajuInputHash, toConfirmedPillars } from "../../../lib/saju";

const statusLabels: Record<ApplicationStatus, string> = {
  SUBMITTED: "접수", REVIEWING: "검토중", PHOTO_REJECTED: "사진반려", NAME_EDITING: "작명중",
  PRODUCTION_READY: "제작대기", PRODUCING: "제작중", COMPLETED: "발급완료", CANCELLED: "취소",
};

// 오행 아이콘용 — 전통 오행 색(목=청/화=적/토=황/금=백금속/수=흑청)을 CSS 클래스로 매핑.
const EL_KEY: Record<string, string> = { 목: "mok", 화: "hwa", 토: "to", 금: "geum", 수: "su" };
const EL_HANJA: Record<string, string> = { 목: "木", 화: "火", 토: "土", 금: "金", 수: "水" };

// 신청 명단 엑셀(xlsx)을 실제 API로 받아 브라우저 다운로드를 트리거한다. POST /api/admin/applications/export.
async function downloadApplicationsExcel(ids: number[], type: ApplicationType) {
  const { blob, filename } = await api.exportApplications(ids, type);
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename.endsWith(".xlsx") ? filename : "applications-export.xlsx";
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function asDataUrl(base64: string) {
  return base64.startsWith("data:") ? base64 : `data:image/png;base64,${base64}`;
}

export function ApplicationsSection() {
  const [tab, setTab] = useState<ApplicationType>("INDIVIDUAL");
  const [all, setAll] = useState<AdminApplicationListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [exporting, setExporting] = useState(false);

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

  // 개인 신청: 선택한 여러 건을 한 엑셀로 내보낸다(단체는 원본 서식 보존을 위해 상세에서 1건씩 — ApplicationNaming).
  const exportSelected = async () => {
    const ids = [...selected];
    if (ids.length === 0) { showToast("내보낼 신청을 선택해 주세요."); return; }
    setExporting(true);
    try {
      await downloadApplicationsExcel(ids, "INDIVIDUAL");
      showToast(`엑셀 ${ids.length}건을 내보냈습니다.`);
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "엑셀 내보내기에 실패했습니다.");
    } finally {
      setExporting(false);
    }
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
          {tab === "INDIVIDUAL" ? "행을 펼쳐 만세력·추천 이름을 확인하고 이름을 확정합니다. 여러 건을 선택해 한 엑셀로 내보낼 수 있습니다." : "행을 펼쳐 엑셀 내보내기·작명 결과 업로드를 신청 단위로 진행합니다(단체는 원본 서식 보존을 위해 1건씩)."}
        </p>
        {tab === "INDIVIDUAL" && (
          <button type="button" className="admin__btn admin__btn--primary" onClick={exportSelected} disabled={exporting || selected.size === 0}>
            {exporting ? "내보내는 중…" : `선택 ${selected.size}건 엑셀 내보내기`}
          </button>
        )}
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
  const [groupBusy, setGroupBusy] = useState(false);
  const [cardBatchOpen, setCardBatchOpen] = useState(false);
  const [cardBatchText, setCardBatchText] = useState("");
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

  // 단체 신청: 원본 서식 엑셀 내보내기 + 사주 프로그램 결과 엑셀 업로드(구성원 이름 일괄 반영).
  const exportThisGroup = async () => {
    setGroupBusy(true);
    try {
      await downloadApplicationsExcel([app.applicationId], "GROUP");
      showToast("엑셀을 내보냈습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "엑셀 내보내기에 실패했습니다.");
    } finally {
      setGroupBusy(false);
    }
  };
  const downloadGroupCards = async () => {
    setGroupBusy(true);
    try {
      const { blob, filename } = await api.getAdminApplicationCardsZip(app.applicationId);
      downloadBlob(blob, filename.endsWith(".zip") ? filename : `application-${app.applicationId}-cards.zip`);
      showToast("카드 ZIP을 다운로드했습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 ZIP 다운로드에 실패했습니다.");
    } finally {
      setGroupBusy(false);
    }
  };
  const applyNamingResult = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setGroupBusy(true);
    try {
      const res = await api.applyNamingResult(app.applicationId, file);
      showToast(`작명 결과 반영 완료 — ${res.updatedCount}명 이름 저장`);
      await Promise.all([reloadMembers(), reloadStats()]);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "작명 결과 업로드에 실패했습니다.");
    } finally {
      setGroupBusy(false);
    }
  };
  // 카드번호 일괄 입력: "사진번호<탭/공백>카드번호" 줄들을 파싱해 PUT .../card-numbers (applicationVersion 동시성).
  const submitCardNumbersBatch = async () => {
    const items = cardBatchText.split("\n").map((l) => l.trim()).filter(Boolean).map((l) => {
      const parts = l.split(/[\t,]+|\s+/).filter(Boolean);
      return { photoNumber: parts[0] ?? "", cardNumber: parts.slice(1).join(" ").trim() };
    }).filter((it) => it.photoNumber && it.cardNumber);
    if (items.length === 0) { showToast("‘사진번호 카드번호’ 형식으로 입력해 주세요."); return; }
    if (detail?.version == null) { showToast("신청 버전을 불러오지 못했습니다. 새로고침 후 다시 시도해 주세요."); return; }
    setGroupBusy(true);
    try {
      const res = await api.assignCardNumbersBatch(app.applicationId, detail.version, items);
      showToast(`카드번호 ${res.updatedCount}건을 저장했습니다.`);
      setDetail(await api.getAdminApplication(app.applicationId));
      await reloadMembers();
      setCardBatchOpen(false);
      setCardBatchText("");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드번호 일괄 저장에 실패했습니다.");
    } finally {
      setGroupBusy(false);
    }
  };

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
  // 접수 직후 앞단 흐름: 결제 확인 → 검토 시작 → 작명 승인.
  if (s === "SUBMITTED" && detail.paymentStatus !== "CONFIRMED") statusActions.push({ label: "결제 확인", call: () => api.confirmApplicationPayment(app.applicationId) });
  if (s === "SUBMITTED" && detail.paymentStatus === "CONFIRMED") statusActions.push({ label: "검토 시작", call: () => api.startApplicationReview(app.applicationId) });
  if (s === "REVIEWING") statusActions.push({ label: "작명 승인(작명중으로)", call: () => api.approveApplicationNaming(app.applicationId) });
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

      {isGroup && (
        <div className="admin-naming__group-tools">
          <button type="button" className="admin__btn" disabled={groupBusy} onClick={exportThisGroup}>
            <span aria-hidden="true">⭳</span> 이 신청 엑셀 내보내기
          </button>
          <label className={`admin__btn admin-naming__upload${groupBusy ? " is-disabled" : ""}`}>
            <span aria-hidden="true">⭱</span> 작명 결과 엑셀 업로드
            <input type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" hidden disabled={groupBusy} onChange={applyNamingResult} />
          </label>
          <button type="button" className="admin__btn" disabled={groupBusy} onClick={() => setCardBatchOpen((v) => !v)}>
            <span aria-hidden="true">#</span> 카드번호 일괄 입력
          </button>
          <button type="button" className="admin__btn" disabled={groupBusy} onClick={downloadGroupCards}>
            <span aria-hidden="true">⭳</span> 전체 카드 ZIP
          </button>
          <span className="admin__muted">사주 프로그램이 채운 이름 엑셀을 업로드하면 구성원 한글이름이 일괄 반영됩니다.</span>
          {cardBatchOpen && (
            <div className="admin-naming__cardbatch">
              <textarea
                className="field__input"
                rows={5}
                value={cardBatchText}
                onChange={(e) => setCardBatchText(e.target.value)}
                placeholder={"사진번호와 카드번호를 한 줄에 하나씩 (탭/공백 구분, 카드번호 형식 ROK-#####-####)\n예)\n001\tROK-00001-0001\n002\tROK-00002-0002"}
              />
              <div className="admin-naming__cardbatch-actions">
                <button type="button" className="admin__btn admin__btn--primary" disabled={groupBusy} onClick={submitCardNumbersBatch}>일괄 저장</button>
                <span className="admin__muted">사진번호 기준 매칭 · 전부 성공해야 저장(all-or-nothing)</span>
              </div>
            </div>
          )}
        </div>
      )}

      {members.map((m, i) => (
        <NamingCard
          key={m.memberId}
          appId={app.applicationId}
          cardTypeId={detail.cardTypeId}
          index={i}
          member={m}
          isGroup={isGroup}
          counts={counts}
          onSaved={onSaved}
        />
      ))}
      {members.length === 0 && <p className="admin-panel__note">구성원 정보가 없습니다.</p>}
    </div>
  );
}

function NamingCard({ appId, cardTypeId, index, member, isGroup, counts, onSaved }: {
  appId: number; cardTypeId?: number; index: number; member: AdminApplicationMember; isGroup: boolean;
  counts: Record<string, number>; onSaved: () => Promise<void>;
}) {
  const memberKey = `${appId}#${member.memberId}`;
  const label = member.englishName || (isGroup ? `멤버 ${index + 1}` : "신청인");
  // 실제 만세력(생년월일/시간). 계산 불가 시 mock로 폴백.
  const fallbackSaju = useMemo(() => computeMemberSaju(member.birthDate, member.birthTime), [member.birthDate, member.birthTime]);
  const [resolvedSaju, setResolvedSaju] = useState<MockSaju | null>(null);
  const saju: MockSaju = useMemo(() => resolvedSaju ?? fallbackSaju ?? mockSaju(memberKey), [resolvedSaju, fallbackSaju, memberKey]);
  const [tick, setTick] = useState(0); // 새로고침 버튼: 증가 시 추천을 다시 뽑는다.
  const recs = useMemo(() => mockRecommendations(memberKey, saju), [memberKey, saju, tick]);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [surname, setSurname] = useState(member.surname ?? "");

  useEffect(() => { setSurname(member.surname ?? ""); }, [member.surname]);

  // 확정 이름은 서버(member.assignedName)가 소스. 예시(preview) 멤버(음수 id)는 저장하지 않는다.
  const chosen = member.surname && member.assignedName ? { surname: member.surname, name: member.assignedName, hanja: member.assignedHanja ?? "" } : null;
  const isPreview = member.memberId < 0;

  const choose = async (name: RecommendedName) => {
    if (isPreview) { showToast("예시 카드입니다. 실제 신청에서 서버에 저장됩니다."); return; }
    const cleanSurname = surname.trim();
    if (!/^[가-힣]{1,2}$/.test(cleanSurname)) {
      showToast("성씨는 한글 1~2자로 입력해 주세요.");
      return;
    }
    setSaving(true);
    try {
      await api.saveMemberName(appId, member.memberId, { surname: cleanSurname, name: name.name, hanja: name.hanja, reading: name.reading, meaning: name.meaning });
      showToast(`"${cleanSurname}${name.name}(${name.hanja})" 이름을 확정했습니다. (서버 저장 · 선택이력 +1)`);
      setEditing(false);
      await onSaved();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const metaLine = [member.nationality, genderLabel(member.gender), member.birthDate].filter(Boolean).join(" · ");
  const sajuLabel = resolvedSaju ? "" : fallbackSaju ? " · 임시 만세력" : " · 만세력 mock";

  // 이름 확정 후: 상태 '작명 완료' + 창을 접어(compact) 노출한다.
  if (chosen && !editing) {
    return (
      <div className="admin-naming__card is-done">
        <div className="admin-naming__card-head">
          <div className="admin-naming__head-left"><b>{label}</b><span className="admin__status-pill is-completed">작명 완료</span></div>
          <button type="button" className="admin__btn" onClick={() => setEditing(true)}>다시 선택</button>
        </div>
        <div className="admin-naming__done">
          <span className="admin-naming__done-name">{chosen.surname}{chosen.name}{chosen.hanja && <em>{chosen.hanja}</em>}</span>
          <span className="admin__muted">확정된 이름 (서버 저장)</span>
        </div>
        {!isPreview && <CardNumberField appId={appId} memberId={member.memberId} current={member.cardNumber} onSaved={onSaved} />}
        {!isPreview && cardTypeId && <CardProductionTools appId={appId} memberId={member.memberId} cardTypeId={cardTypeId} onGenerated={onSaved} />}
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
        <span className="admin__muted">{metaLine}{sajuLabel}</span>
      </div>

      {!isPreview && (
        <SajuResolvePanel
          appId={appId}
          member={member}
          fallbackSaju={saju}
          onResolved={setResolvedSaju}
        />
      )}

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
        <label className="admin-naming__surname">
          <span>성씨</span>
          <input className="field__input" value={surname} onChange={(e) => setSurname(e.target.value)} placeholder="김" maxLength={2} />
        </label>
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
      {!isPreview && <CardNumberField appId={appId} memberId={member.memberId} current={member.cardNumber} onSaved={onSaved} />}
      {!isPreview && cardTypeId && <CardProductionTools appId={appId} memberId={member.memberId} cardTypeId={cardTypeId} onGenerated={onSaved} />}
    </div>
  );
}

function SajuResolvePanel({ appId, member, fallbackSaju, onResolved }: {
  appId: number;
  member: AdminApplicationMember;
  fallbackSaju: MockSaju;
  onResolved: (saju: MockSaju | null) => void;
}) {
  const [query, setQuery] = useState(member.birthRegion ?? "");
  const [candidates, setCandidates] = useState<BirthRegionCandidate[]>([]);
  const [selected, setSelected] = useState("");
  const [resolved, setResolved] = useState<ManseryeokResolveResponse | null>(null);
  const [busy, setBusy] = useState(false);

  const search = async () => {
    const keyword = query.trim();
    if (!keyword) { showToast("출생지역을 입력해 주세요."); return; }
    setBusy(true);
    try {
      const rows = await api.searchBirthRegion(keyword);
      setCandidates(rows);
      setSelected(rows[0] ? `${rows[0].latitude},${rows[0].longitude}` : "");
      if (rows.length === 0) showToast("출생지역 검색 결과가 없습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "출생지역 검색에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const resolve = async (candidate?: BirthRegionCandidate, offset?: OffsetCandidate) => {
    const target = candidate ?? candidates.find((c) => `${c.latitude},${c.longitude}` === selected);
    if (!target) { showToast("출생지역 후보를 선택해 주세요."); return; }
    setBusy(true);
    try {
      const data = await api.resolveManseryeokBirthTime(appId, member.memberId, {
        latitude: target.latitude,
        longitude: target.longitude,
        timezoneId: resolved?.timezoneId,
        selectedOffset: offset?.offset,
      });
      setResolved(data);
      if (data.status === "EXACT" && data.utcInstant && data.longitude != null) {
        const saju = computeMemberSajuFromResolved(data.utcInstant, data.longitude);
        if (saju) {
          onResolved(saju);
          await api.confirmManseryeokResult(appId, member.memberId, {
            timezoneId: data.timezoneId ?? "",
            longitude: data.longitude,
            selectedOffset: data.selectedOffset,
            utcInstant: data.utcInstant,
            timeAccuracy: "EXACT",
            confirmedPillars: toConfirmedPillars(saju),
            uncertainPillars: [],
            elementCounts: saju.elementCounts,
            calculationEngineVersion: "manseryeok@2.0.0",
            inputHash: makeSajuInputHash([member.birthDate, member.birthTime, data.timezoneId, data.longitude, data.utcInstant]),
          });
          showToast("만세력 결과를 확정 저장했습니다.");
        }
      } else if (data.status === "UNKNOWN_TIME") {
        onResolved(fallbackSaju);
        await api.confirmManseryeokResult(appId, member.memberId, {
          timezoneId: data.timezoneId ?? "UNKNOWN",
          longitude: data.longitude ?? target.longitude,
          timeAccuracy: "UNKNOWN",
          confirmedPillars: toConfirmedPillars(fallbackSaju),
          uncertainPillars: ["hour"],
          elementCounts: fallbackSaju.elementCounts,
          calculationEngineVersion: "manseryeok@2.0.0",
          inputHash: makeSajuInputHash([member.birthDate, member.birthTime, data.timezoneId, target.longitude, "UNKNOWN"]),
        });
        showToast("출생시간 미상으로 만세력 결과를 저장했습니다.");
      } else if (data.status === "AMBIGUOUS_LOCAL_TIME") {
        showToast("중복되는 현지 시각입니다. 후보 offset 중 하나를 선택해 주세요.");
      } else {
        showToast("존재하지 않는 현지 시각입니다. 출생시간을 확인해 주세요.");
      }
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "만세력 확정에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="admin-naming__resolve">
      <div className="field__with-btn">
        <input className="field__input" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="출생지역 검색" />
        <button type="button" className="postal-btn" disabled={busy} onClick={search}>검색</button>
      </div>
      {candidates.length > 0 && (
        <div className="field__with-btn">
          <select className="field__select" value={selected} onChange={(e) => setSelected(e.target.value)}>
            {candidates.map((c) => <option key={`${c.displayName}-${c.latitude}-${c.longitude}`} value={`${c.latitude},${c.longitude}`}>{c.displayName}</option>)}
          </select>
          <button type="button" className="postal-btn" disabled={busy} onClick={() => void resolve()}>만세력 확정</button>
        </div>
      )}
      {resolved?.status === "AMBIGUOUS_LOCAL_TIME" && (
        <div className="admin-naming__offsets">
          {(resolved.candidates ?? []).map((c) => (
            <button key={`${c.offset}-${c.utcInstant}`} type="button" className="admin__btn" disabled={busy} onClick={() => void resolve(undefined, c)}>
              {c.offset} / {c.utcInstant}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function CardProductionTools({ appId, memberId, cardTypeId, onGenerated }: {
  appId: number;
  memberId: number;
  cardTypeId: number;
  onGenerated: () => Promise<void>;
}) {
  const [designs, setDesigns] = useState<CardDesignOption[]>([]);
  const [designId, setDesignId] = useState("");
  const [issueDate, setIssueDate] = useState(todayIso());
  const [preview, setPreview] = useState<{ front: string; back: string } | null>(null);
  const [busy, setBusy] = useState(false);

  const loadDesigns = async () => {
    setBusy(true);
    try {
      const rows = await api.listCardDesigns({ cardTypeId, active: true, applicationId: appId });
      setDesigns(rows);
      setDesignId((current) => current || String(rows.find((d) => d.isDefault)?.id ?? rows[0]?.id ?? ""));
      if (rows.length === 0) showToast("사용 가능한 카드 디자인이 없습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 디자인을 불러오지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const requestBody = () => {
    const id = Number(designId);
    if (!id) {
      showToast("카드 디자인을 선택해 주세요.");
      return null;
    }
    return { cardDesignId: id, issueDate };
  };

  const previewCard = async () => {
    const body = requestBody();
    if (!body) return;
    setBusy(true);
    try {
      const data = await api.getCardPreview(appId, memberId, body);
      setPreview({ front: asDataUrl(data.front), back: asDataUrl(data.back) });
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 미리보기에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const generate = async () => {
    const body = requestBody();
    if (!body) return;
    setBusy(true);
    try {
      await api.generateCard(appId, memberId, body);
      showToast("카드 이미지를 생성해 저장했습니다.");
      await onGenerated();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 생성에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const downloadMember = async () => {
    setBusy(true);
    try {
      const data = await api.getAdminMemberCardDownload(appId, memberId);
      window.open(data.cardFrontUrl, "_blank", "noopener,noreferrer");
      window.open(data.cardBackUrl, "_blank", "noopener,noreferrer");
      showToast("카드 다운로드 링크를 열었습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 다운로드에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="admin-card-tools">
      <div className="admin-card-tools__row">
        <button type="button" className="admin__btn" disabled={busy} onClick={loadDesigns}>디자인 불러오기</button>
        <select className="field__select" value={designId} onChange={(e) => setDesignId(e.target.value)} disabled={busy || designs.length === 0}>
          <option value="">디자인 선택</option>
          {designs.map((d) => <option key={d.id} value={d.id}>{d.name} #{d.designNumber}</option>)}
        </select>
        <input className="field__input admin-card-tools__date" type="date" value={issueDate} onChange={(e) => setIssueDate(e.target.value)} />
        <button type="button" className="admin__btn" disabled={busy} onClick={previewCard}>미리보기</button>
        <button type="button" className="admin__btn admin__btn--primary" disabled={busy} onClick={generate}>카드 생성</button>
        <button type="button" className="admin__btn" disabled={busy} onClick={downloadMember}>다운로드</button>
      </div>
      {preview && (
        <div className="admin-card-tools__preview">
          <img src={preview.front} alt="카드 앞면 미리보기" />
          <img src={preview.back} alt="카드 뒷면 미리보기" />
        </div>
      )}
    </div>
  );
}

// 개인/단일 멤버 카드번호 확정 — 관리자 직접 입력(PUT .../members/{memberId}/card-number).
function CardNumberField({ appId, memberId, current, onSaved }: { appId: number; memberId: number; current?: string; onSaved: () => Promise<void> }) {
  const [value, setValue] = useState(current ?? "");
  const [busy, setBusy] = useState(false);
  const save = async () => {
    if (!value.trim()) { showToast("카드번호를 입력해 주세요."); return; }
    setBusy(true);
    try {
      await api.assignCardNumber(appId, memberId, value.trim());
      showToast("카드번호를 저장했습니다.");
      await onSaved();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드번호 저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="admin-naming__cardnum">
      <span className="field__label">카드번호</span>
      <div className="field__with-btn">
        <input className="field__input" value={value} onChange={(e) => setValue(e.target.value)} placeholder="ROK-00000-0000" maxLength={30} />
        <button type="button" className="postal-btn" disabled={busy} onClick={save}>저장</button>
      </div>
    </div>
  );
}

function Item({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return <div className="admin__detail-item"><dt>{label}</dt><dd>{value}</dd></div>;
}
