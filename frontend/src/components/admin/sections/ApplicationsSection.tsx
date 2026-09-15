import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import {
  api,
  ApiError,
  type AdminApplicationListItem,
  type ApplicationType
} from "../../../services/api";
import { showToast } from "../../ui/toast";
import { AdminPager } from "../AdminPager";

import { ApplicationDetail } from "../applications/ApplicationDetail";
import { NamingCard } from "../applications/NamingCard";
import { downloadApplicationsExcel, statusLabels } from "../applications/applicationUtils";
export function ApplicationsSection() {
  const [tab, setTab] = useState<ApplicationType>("INDIVIDUAL");
  const [all, setAll] = useState<AdminApplicationListItem[]>([]);
  // 서버 페이지네이션(§1.20) — 목록 API에 신청유형 필터가 없어 페이지 안에서 탭(개인/단체)으로 나눠 보여준다.
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [exporting, setExporting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.listAdminApplications({ page, size: 50 });
      setAll(result.content);
      setTotalPages(Math.max(1, result.totalPages));
      setTotalElements(result.totalElements);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "신청 목록을 불러오지 못했습니다. 관리자 권한(서버 인증)이 필요합니다.");
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { void load(); }, [load]);

  const rows = useMemo(() => all.filter((a) => a.applicationType === tab), [all, tab]);
  const switchTab = (next: ApplicationType) => { setTab(next); setOpenId(null); setSelected(new Set()); };
  const changePage = (next: number) => { setPage(next); setOpenId(null); setSelected(new Set()); };

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

  // 개인 신청: 선택한 여러 건을 한 엑셀로 내보낸다(단체는 원본 서식 보존을 위해 상세에서 1건씩 — ApplicationDetail).
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
        <span className="admin__muted">전체 {totalElements}건 · 탭 숫자는 현재 페이지 기준</span>
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
                      <td colSpan={tab === "INDIVIDUAL" ? 7 : 6}><ApplicationDetail app={a} onChanged={load} /></td>
                    </tr>
                  )}
                </Fragment>
              ))}
              {rows.length === 0 && <tr><td className="admin__empty" colSpan={tab === "INDIVIDUAL" ? 7 : 6}>{tab === "INDIVIDUAL" ? "개인" : "단체"} 신청 내역이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
      {!error && <AdminPager page={page} totalPages={totalPages} disabled={loading} onChange={changePage} />}

      {!loading && !error && rows.length === 0 && (
        <div className="admin-naming" style={{ marginTop: 14 }}>
          <p className="admin-naming__preview-banner">
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
