import { useEffect, useMemo, useState } from "react";
import { recommendNames, type RecommendedName, type SajuSnapshot } from "../../../lib/namingRecommendations";
import { computeMemberSaju, fromActiveManseryeokResult } from "../../../lib/saju";
import {
  api,
  ApiError,
  type AdminApplicationMember,
  type ManseryeokActiveResult,
  type StudentTextColor
} from "../../../services/api";
import { showToast } from "../../ui/toast";
import { genderLabel } from "./applicationUtils";

import { CardNumberField } from "./CardNumberField";
import { CardProductionPanel } from "./CardProductionPanel";
import { ManseryeokPanel } from "./ManseryeokPanel";
// 오행 아이콘용 — 전통 오행 색(목=청/화=적/토=황/금=백금속/수=흑청)을 CSS 클래스로 매핑.
const EL_KEY: Record<string, string> = { 목: "mok", 화: "hwa", 토: "to", 금: "geum", 수: "su" };
const EL_HANJA: Record<string, string> = { 목: "木", 화: "火", 토: "土", 금: "金", 수: "水" };

export function NamingCard({ appId, cardTypeId, confirmedStudentFrontTextColor, confirmedStudentBackTextColor, index, member, isGroup, counts, onSaved, manseryeok, onManseryeokChanged }: {
  appId: number; cardTypeId?: number; index: number; member: AdminApplicationMember; isGroup: boolean;
  // 학생증(STUDENT) 전용 — 신청 단위로 이미 확정된 카드 텍스트 색상(카드 생성 성공 시 확정됨).
  confirmedStudentFrontTextColor?: StudentTextColor; confirmedStudentBackTextColor?: StudentTextColor;
  counts: Record<string, number>; onSaved: () => Promise<void>;
  // 활성 확정 만세력(1-E) — 예시(preview) 카드는 전달하지 않는다. result=null이면 미확정.
  manseryeok?: { status: "LOADING" | "READY" | "ERROR"; result: ManseryeokActiveResult | null; loaded: boolean };
  onManseryeokChanged?: () => Promise<void>;
}) {
  const label = member.englishName || (isGroup ? `멤버 ${index + 1}` : "신청인");
  const isPreview = member.memberId < 0;
  // 로컬 계산 사주 — (a) 예시 카드 표시용 (b) 출생시간 미상(UNKNOWN_TIME) 확정 payload 계산용.
  // 실제 멤버의 화면 표시·이름 추천에는 쓰지 않는다(활성 확정 결과만 사용).
  const localSaju = useMemo(() => computeMemberSaju(member.birthDate, member.birthTime), [member.birthDate, member.birthTime]);
  // 저장된 활성 결과 → 화면 모델. null이면서 result가 있으면 저장 데이터 손상(임의 보정 금지).
  const restored = useMemo(
    () => (manseryeok?.result ? fromActiveManseryeokResult(manseryeok.result) : null),
    [manseryeok?.result],
  );
  const restoreBroken = Boolean(manseryeok?.result) && !restored;
  const saju: SajuSnapshot | null = isPreview ? localSaju : restored;
  // 결정적 추천(§1.19) — 확정 만세력이 있을 때만 상위 5개(score DESC, 사전 index ASC).
  const recs = useMemo(() => (saju ? recommendNames(saju) : []), [saju]);
  const uncertainPillars = manseryeok?.result?.uncertainPillars ?? [];
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [surname, setSurname] = useState(member.surname ?? "");

  useEffect(() => { setSurname(member.surname ?? ""); }, [member.surname]);

  // 확정 이름은 서버(member.assignedName)가 소스. 예시(preview) 멤버(음수 id)는 저장하지 않는다.
  const chosen = member.surname && member.assignedName ? { surname: member.surname, name: member.assignedName, hanja: member.assignedHanja ?? "" } : null;

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
  // 만세력 표시 상태(1-E-1): LOADING / CONFIRMED(restored) / NOT_CONFIRMED / ERROR / 데이터 손상.
  const manseryeokLoading = Boolean(manseryeok && manseryeok.status === "LOADING" && !manseryeok.loaded);
  const manseryeokError = Boolean(manseryeok && manseryeok.status === "ERROR" && !manseryeok.loaded);
  const manseryeokStaleWarn = Boolean(manseryeok && manseryeok.status === "ERROR" && manseryeok.loaded);
  const sajuLabel = isPreview ? " · 예시(로컬 계산)" : restored ? " · 확정 만세력" : "";

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
        {!isPreview && cardTypeId && (
          <CardProductionPanel
            appId={appId}
            memberId={member.memberId}
            cardTypeId={cardTypeId}
            confirmedFrontTextColor={confirmedStudentFrontTextColor}
            confirmedBackTextColor={confirmedStudentBackTextColor}
            onGenerated={onSaved}
          />
        )}
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
        <ManseryeokPanel
          appId={appId}
          member={member}
          unknownTimeSaju={localSaju}
          onConfirmed={onManseryeokChanged ?? (async () => {})}
        />
      )}

      {/* 만세력 표시(1-E-1): 조회 중·미확정·조회 실패·데이터 손상을 명확히 구분하고, 어떤 경우에도
          로컬 계산값을 서버 확정 결과처럼 표시하지 않는다. */}
      {!isPreview && manseryeokLoading && <p className="admin-panel__note">확정 만세력 조회 중…</p>}
      {!isPreview && manseryeokError && (
        <p className="admin-panel__note admin-panel__note--error">
          만세력 확정 결과 조회에 실패했습니다. 네트워크 확인 후 다시 시도해 주세요.{" "}
          <button type="button" className="admin__btn" onClick={() => void onManseryeokChanged?.()}>다시 시도</button>
        </p>
      )}
      {!isPreview && manseryeokStaleWarn && (
        <p className="admin-panel__note admin-panel__note--error">최신 만세력 재조회에 실패했습니다 — 마지막으로 조회된 결과를 표시 중입니다.</p>
      )}
      {!isPreview && restoreBroken && (
        <p className="admin-panel__note admin-panel__note--error">저장된 만세력 데이터가 손상되었습니다. 출생지역 검색으로 다시 확정해 주세요.</p>
      )}
      {!isPreview && !manseryeokLoading && !manseryeokError && !restoreBroken && !restored && (
        <p className="admin-panel__note">확정된 만세력이 없습니다 — 출생지역을 검색해 만세력을 확정하면 이름 추천이 활성화됩니다.</p>
      )}

      {saju && (
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
          {!isPreview && uncertainPillars.length > 0 && (
            <p className="admin__muted">미확정 주: {uncertainPillars.join("·")} — 확정값이 아니므로 “—”로 표시됩니다.</p>
          )}
          {!isPreview && manseryeok?.result && (
            <p className="admin__muted">
              정확도 {manseryeok.result.timeAccuracy} · 계산 {new Date(manseryeok.result.calculatedAt).toLocaleString("ko-KR")}
              {manseryeok.result.calculationEngineVersion ? ` · ${manseryeok.result.calculationEngineVersion}` : ""}
            </p>
          )}
        </div>
      )}

      {saju && (
        <>
          <div className="admin-naming__recs-head">
            <b className="admin-naming__subtitle">추천 이름 {recs.length}</b>
            <label className="admin-naming__surname">
              <span>성씨</span>
              <input className="field__input" value={surname} onChange={(e) => setSurname(e.target.value)} placeholder="김" maxLength={2} />
            </label>
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
        </>
      )}
      {!isPreview && <CardNumberField appId={appId} memberId={member.memberId} current={member.cardNumber} onSaved={onSaved} />}
      {!isPreview && cardTypeId && (
        <CardProductionPanel
          appId={appId}
          memberId={member.memberId}
          cardTypeId={cardTypeId}
          confirmedFrontTextColor={confirmedStudentFrontTextColor}
          confirmedBackTextColor={confirmedStudentBackTextColor}
          onGenerated={onSaved}
        />
      )}
    </div>
  );
}

