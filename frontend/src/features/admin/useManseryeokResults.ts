import { useCallback, useEffect, useRef, useState } from "react";
import { api, type ManseryeokActiveResult } from "../../services/api";
// 재진입 복원(1-E) 상태 — results는 마지막으로 성공 조회한 Map을 유지한다(재조회 실패 시에도 표시 유지).
interface ManseryeokRestoreState {
  status: "LOADING" | "READY" | "ERROR";
  results: Map<number, ManseryeokActiveResult> | null;
}

export function useManseryeokResults(applicationId: number) {
  // 활성 만세력 결과 일괄 복원(1-E-3) — Application 단위 GET 1회, 각 NamingCard에 Map으로 내려준다.
  const [manseryeok, setManseryeok] = useState<ManseryeokRestoreState>({ status: "LOADING", results: null });
  const manseryeokGen = useRef(0);

  const loadManseryeok = useCallback(async () => {
    const gen = ++manseryeokGen.current;
    setManseryeok((prev) => ({ status: "LOADING", results: prev.results }));
    try {
      const rows = await api.listManseryeokResults(applicationId);
      if (gen !== manseryeokGen.current) return; // 늦게 도착한 이전 요청이 새 상태를 덮지 않게
      setManseryeok({ status: "READY", results: new Map(rows.map((r) => [r.memberId, r.result])) });
    } catch {
      if (gen !== manseryeokGen.current) return;
      // 조회 실패 — 마지막 정상값은 유지하고 상태만 오류로 표시한다.
      setManseryeok((prev) => ({ status: "ERROR", results: prev.results }));
    }
  }, [applicationId]);

  useEffect(() => {
    void loadManseryeok();
    return () => { manseryeokGen.current += 1; };
  }, [loadManseryeok]);

  return { manseryeok, loadManseryeok };
}
