import { useCallback, useEffect, useState } from "react";
import { useManseryeokResults } from "../../../features/admin/useManseryeokResults";
import {
  api,
  ApiError,
  type AdminApplicationDetail,
  type AdminApplicationListItem,
  type AdminApplicationMember,
  type ApplicationStatus
} from "../../../services/api";
import { showToast } from "../../ui/toast";
import { genderLabel } from "./applicationUtils";

import { NamingCard } from "./NamingCard";
import { ZodiacDesignSelector } from "./ZodiacDesignSelector";
import { downloadApplicationsExcel, downloadBlob, statusLabels } from "./applicationUtils";

// 전체 파이프라인 조회용(2026-09-24, 읽기 전용) — 실제 전이 규칙은 백엔드
// ApplicationStatus.canTransitionTo와 동일: 주 경로는 선형이고, PHOTO_REJECTED/CANCELLED는
// 이탈 경로(각각 REVIEWING에서 갈렸다가 되돌아올 수 있음 / 여러 단계에서 취소로 빠질 수 있음)다.
const PIPELINE_MAIN = ["SUBMITTED", "REVIEWING", "NAME_EDITING", "PRODUCTION_READY", "PRODUCING", "COMPLETED"] as const;
const PIPELINE_BRANCHES = ["PHOTO_REJECTED", "CANCELLED"] as const;

// 신청 상세 + 구성원 작명 플로우 + 상태 전이(모두 실제 API), 만세력은 실제 계산.
export function ApplicationDetail({ app, onChanged }: { app: AdminApplicationListItem; onChanged?: () => void | Promise<void> }) {
  const [detail, setDetail] = useState<AdminApplicationDetail | null>(null);
  const [members, setMembers] = useState<AdminApplicationMember[] | null>(null);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [statusBusy, setStatusBusy] = useState(false);
  const [pipelineOpen, setPipelineOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelMemo, setCancelMemo] = useState("");
  const [groupBusy, setGroupBusy] = useState(false);
  const [cardBatchOpen, setCardBatchOpen] = useState(false);
  const [cardBatchText, setCardBatchText] = useState("");
  const [zodiacDesignSet, setZodiacDesignSet] = useState("");
  const [zodiacBusy, setZodiacBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { manseryeok, loadManseryeok } = useManseryeokResults(app.applicationId);

  const reloadMembers = useCallback(async () => {
    setMembers(await api.getAdminApplicationMembers(app.applicationId));
  }, [app.applicationId]);

  // 카드 생성 성공 시 확정되는 학생증 텍스트 색상(studentFrontTextColor/studentBackTextColor)을
  // 갱신하기 위해 상세도 함께 다시 불러온다 — onSaved가 이미 여러 저장 동작(작명·카드번호·카드생성)
  // 공용 콜백이라 여기 추가하는 게 가장 자연스럽다.
  const reloadDetail = useCallback(async () => {
    setDetail(await api.getAdminApplication(app.applicationId));
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

  // 십이간지 디자인 세트는 잠금이 없어 언제든 바뀔 수 있다 — 상세가 다시 로드될 때마다 선택값을 맞춘다.
  useEffect(() => {
    if (detail?.zodiacDesignSet) setZodiacDesignSet(String(detail.zodiacDesignSet));
  }, [detail?.zodiacDesignSet]);

  if (error) return <p className="admin-panel__note admin-panel__note--error">{error}</p>;
  if (!detail || !members) return <p className="admin-panel__note">상세 불러오는 중…</p>;

  const isGroup = detail.applicationType === "GROUP";
  const first = members[0];
  const onSaved = async () => { await Promise.all([reloadMembers(), reloadStats(), reloadDetail()]); };

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

  // 십이간지 캐릭터 디자인 세트(1~5, 신청 전체 1개, 카드종류 무관) — 잠금 없이 언제든 재저장 가능.
  // 값이 없으면 카드 미리보기·생성이 ZODIAC_DESIGN_NOT_SELECTED로 거절된다.
  const saveZodiacDesignSet = async () => {
    const value = Number(zodiacDesignSet);
    if (!value) { showToast("디자인 세트를 선택해 주세요."); return; }
    setZodiacBusy(true);
    try {
      await api.assignZodiacDesignSet(app.applicationId, value);
      showToast("십이간지 디자인 세트를 저장했습니다.");
      await reloadDetail();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "십이간지 디자인 세트 저장에 실패했습니다.");
    } finally {
      setZodiacBusy(false);
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
  // 관리자 강제 취소(2026-09-25) — COMPLETED/CANCELLED를 제외한 6개 상태에서 허용. 필수 메모는
  // "카드번호 일괄 입력"과 같은 기존 토글형 인라인 패널 패턴을 재사용(별도 모달 라이브러리 도입 없음).
  // refundRequired는 결제 상태 자체를 서버가 건드리지 않는다는 뜻이라 확정 후 토스트로 수동 환불을 상기시킨다.
  const trimmedCancelMemo = cancelMemo.trim();
  const cancelMemoValid = trimmedCancelMemo.length >= 1 && trimmedCancelMemo.length <= 500;
  const runCancel = async () => {
    if (!cancelMemoValid) return;
    setStatusBusy(true);
    try {
      const res = await api.cancelApplicationByAdmin(app.applicationId, trimmedCancelMemo);
      showToast(res.refundRequired ? "취소 완료 — 상태: 취소됨 (결제 확인됨 — 환불은 수동으로 처리하세요)" : "취소 완료 — 상태: 취소됨");
      setCancelOpen(false);
      setCancelMemo("");
      setDetail(await api.getAdminApplication(app.applicationId));
      await onChanged?.();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "취소에 실패했습니다.");
    } finally {
      setStatusBusy(false);
    }
  };
  const canCancel = (["SUBMITTED", "REVIEWING", "PHOTO_REJECTED", "NAME_EDITING", "PRODUCTION_READY", "PRODUCING"] as ApplicationStatus[]).includes(s);
  // "작명 완료 처리"/"제작 시작"은 목록에서 제거됐다(2026-09-24 확정) — 마지막 구성원의
  // 이름 확정/카드 생성 시점에 백엔드가 자동으로 전이시킨다. 아래 엔드포인트는 자동 전이가
  // 어떤 이유로든 안 걸렸을 때의 수동 복구 경로로 백엔드에 계속 남아있지만, 평소 업무 흐름에는
  // 노출하지 않는다.
  const statusActions: { label: string; danger?: boolean; call: () => Promise<{ status: ApplicationStatus }> | null }[] = [];
  // 접수 직후 앞단 흐름: 결제 확인 → 검토 시작 → 작명 승인.
  if (s === "SUBMITTED" && detail.paymentStatus !== "CONFIRMED") statusActions.push({ label: "결제 확인", call: () => api.confirmApplicationPayment(app.applicationId) });
  if (s === "SUBMITTED" && detail.paymentStatus === "CONFIRMED") statusActions.push({ label: "검토 시작", call: () => api.startApplicationReview(app.applicationId) });
  if (s === "REVIEWING") statusActions.push({ label: "작명 승인(작명중으로)", call: () => api.approveApplicationNaming(app.applicationId) });
  if (s === "REVIEWING") statusActions.push({ label: "사진 반려", danger: true, call: () => { const r = window.prompt("사진 반려 사유를 입력하세요."); return r && r.trim() ? api.rejectApplicationPhoto(app.applicationId, r.trim()) : null; } });
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
          {s === "CANCELLED" && <Item label="취소 메모" value={detail.cancellationMemo ?? undefined} />}
        </dl>
      </div>

      <div className="admin-naming__status">
        <span className="admin-naming__subtitle">상태 관리</span>
        <span className="admin__badge">{statusLabels[s]}</span>
        {detail.physicalDispatchedAt && <span className="admin__muted">발송됨</span>}
        <div className="admin-naming__status-actions">
          {statusActions.map((a) => (
            <button
              key={a.label}
              type="button"
              className={`admin__btn${a.danger ? " admin__btn--danger" : ""}`}
              disabled={statusBusy}
              onClick={() => void runStatus(a.label, a.call)}
            >
              {a.label}
            </button>
          ))}
          {statusActions.length === 0 && !canCancel && <span className="admin__muted">가능한 수동 전이 없음</span>}
          {canCancel && (
            <button
              type="button"
              className="admin__btn admin__btn--danger"
              aria-expanded={cancelOpen}
              disabled={statusBusy}
              onClick={() => setCancelOpen((v) => !v)}
            >
              신청 취소
            </button>
          )}
        </div>
        {canCancel && cancelOpen && (
          <div className="admin-naming__cancel-panel">
            <p className="admin__muted">
              완료 전 신청을 취소하며 생성된 사진·카드 파일이 삭제됩니다.
              {detail.paymentStatus === "CONFIRMED" && " 결제가 확인된 신청이므로 별도 운영 절차로 전액 환불해야 합니다."}
            </p>
            <textarea
              className="field__input"
              rows={4}
              maxLength={500}
              value={cancelMemo}
              onChange={(e) => setCancelMemo(e.target.value)}
              placeholder="취소 사유(메모)를 입력하세요. 공백 제외 1~500자, 필수입니다."
              disabled={statusBusy}
            />
            <div className="admin-naming__cancel-panel-actions">
              <span className="admin__muted">{trimmedCancelMemo.length}/500자</span>
              <button type="button" className="admin__btn" disabled={statusBusy} onClick={() => { setCancelOpen(false); setCancelMemo(""); }}>
                닫기
              </button>
              <button type="button" className="admin__btn admin__btn--danger" disabled={statusBusy || !cancelMemoValid} onClick={() => void runCancel()}>
                취소 확정
              </button>
            </div>
          </div>
        )}
        <button
          type="button"
          className="admin__btn admin-naming__pipeline-toggle"
          aria-expanded={pipelineOpen}
          onClick={() => setPipelineOpen((v) => !v)}
        >
          전체 진행 단계 {pipelineOpen ? "숨기기" : "보기"}
        </button>
      </div>

      {pipelineOpen && (
        <div className="admin-naming__pipeline">
          <ol className="admin-naming__pipeline-main">
            {PIPELINE_MAIN.map((st, i) => {
              const currentIdx = PIPELINE_MAIN.indexOf(s as typeof PIPELINE_MAIN[number]);
              const stepState = s === st ? "current" : currentIdx >= 0 && i < currentIdx ? "done" : "pending";
              return (
                <li key={st} className={`admin-naming__pipeline-step is-${stepState}`}>
                  {i > 0 && <span className="admin-naming__pipeline-arrow" aria-hidden="true">→ </span>}
                  {statusLabels[st]}
                </li>
              );
            })}
          </ol>
          <p className="admin__muted admin-naming__pipeline-branches">
            이탈 경로 —{" "}
            {PIPELINE_BRANCHES.map((st, i) => (
              <span key={st}>
                {i > 0 && " · "}
                {statusLabels[st]}{s === st ? "(현재)" : ""}
              </span>
            ))}
          </p>
        </div>
      )}

      {s === "CANCELLED" ? (
        <p className="admin-panel__note">취소된 신청입니다 — 생성됐던 사진·카드 파일이 이미 삭제되어 작명·카드 관련 작업을 진행할 수 없습니다.</p>
      ) : (
        <>
          <div className="admin-naming__zodiac">
            <span className="admin-naming__subtitle">십이간지 디자인</span>
            <ZodiacDesignSelector value={zodiacDesignSet} onChange={setZodiacDesignSet} disabled={zodiacBusy} />
            <button type="button" className="admin__btn" disabled={zodiacBusy || !zodiacDesignSet} onClick={saveZodiacDesignSet}>저장</button>
            <span className="admin__muted">
              {detail.zodiacDesignSet != null ? `현재 확정: 세트 ${detail.zodiacDesignSet}` : "미선택 — 카드 미리보기·생성이 거절됩니다"} · 카드 생성 전후 언제든 변경 가능, 저장 후 "미리보기"로 실제 카드에서 확인
            </span>
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
              <span className="admin__muted">사주 프로그램이 채운 이름 엑셀을 업로드하면 구성원 한글이름이 일괄 반영됩니다. “성씨” 열(선택, 한글 1~2자)을 추가하면 성씨도 함께 저장됩니다.</span>
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
              applicationStatus={detail.status}
              confirmedCardDesignId={detail.cardDesignId}
              confirmedCardIssueDate={detail.cardIssueDate}
              confirmedStudentFrontTextColor={detail.studentFrontTextColor}
              confirmedStudentBackTextColor={detail.studentBackTextColor}
              index={i}
              member={m}
              isGroup={isGroup}
              counts={counts}
              onSaved={onSaved}
              manseryeok={{
                status: manseryeok.status,
                result: manseryeok.results?.get(m.memberId) ?? null,
                loaded: manseryeok.results !== null,
              }}
              onManseryeokChanged={loadManseryeok}
            />
          ))}
          {members.length === 0 && <p className="admin-panel__note">구성원 정보가 없습니다.</p>}
        </>
      )}
    </div>
  );
}

function Item({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return <div className="admin__detail-item"><dt>{label}</dt><dd>{value}</dd></div>;
}
