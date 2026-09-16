import { useState } from "react";
import { type SajuSnapshot } from "../../../lib/namingRecommendations";
import { computeMemberSajuFromResolved, makeSajuInputHash, toConfirmedPillars } from "../../../lib/saju";
import {
  api,
  ApiError,
  type AdminApplicationMember,
  type BirthRegionCandidate,
  type ManseryeokResolveResponse,
  type OffsetCandidate
} from "../../../services/api";
import { showToast } from "../../ui/toast";

// 만세력 확정 흐름(검색→resolve→confirm). 복원(재진입)은 부모의 일괄 GET이 담당하며 여기서는 confirm
// 성공 시 onConfirmed()로 활성 결과 재조회만 트리거한다 — 복원 목적으로 confirm을 재호출하지 않는다.
export function ManseryeokPanel({ appId, member, unknownTimeSaju, onConfirmed }: {
  appId: number;
  member: AdminApplicationMember;
  // 출생시간 미상(UNKNOWN_TIME) 확정 payload 계산용 로컬 사주 — 화면 표시용이 아니다.
  unknownTimeSaju: SajuSnapshot | null;
  onConfirmed: () => Promise<void>;
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
          // 방금 전송한 값을 임시 확정으로 쓰지 않고 서버 저장값(활성 결과)을 재조회해 화면을 갱신한다(1-E-2).
          await onConfirmed();
        }
      } else if (data.status === "UNKNOWN_TIME") {
        if (!unknownTimeSaju) {
          showToast("생년월일 정보가 없어 만세력을 계산할 수 없습니다.");
          return;
        }
        await api.confirmManseryeokResult(appId, member.memberId, {
          timezoneId: data.timezoneId ?? "UNKNOWN",
          longitude: data.longitude ?? target.longitude,
          timeAccuracy: "UNKNOWN",
          confirmedPillars: toConfirmedPillars(unknownTimeSaju),
          uncertainPillars: ["hour"],
          elementCounts: unknownTimeSaju.elementCounts,
          calculationEngineVersion: "manseryeok@2.0.0",
          inputHash: makeSajuInputHash([member.birthDate, member.birthTime, data.timezoneId, target.longitude, "UNKNOWN"]),
        });
        showToast("출생시간 미상으로 만세력 결과를 저장했습니다.");
        await onConfirmed();
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

