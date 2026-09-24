// 관리자 "이름 추천/만세력" 플로우 데이터.
// - 추천 이름: 실제 데이터(sajuNames.json — saju 레포 names.json + 사주 이름 결과.xlsx를 병합한 700개,
//   자원오행/발음오행/뜻 포함)를 오행 결핍 기반으로 점수화한다.
// - 추천은 결정적이다(admin-saju.md): 같은 만세력·같은 이름 사전이면 항상 같은 상위 5개가 같은 순서로
//   나온다. score DESC, 동점이면 이름 사전 index(고정 식별자) ASC. Math.random() 사용 금지.
// Node ESM(순수 로직 테스트)에서도 로드되도록 JSON import attribute를 명시한다(Vite/Rollup도 지원).
import sajuNamesRaw from "../data/sajuNames.json" with { type: "json" };

export type FiveElement = "목" | "화" | "토" | "금" | "수";

interface SajuNameEntry {
  name: string;
  hanja: string;
  roman: string;
  jawon: FiveElement[]; // 자원오행(글자별)
  eum: FiveElement[]; // 발음오행(초성별)
  reading: string;
  meaning: string;
}
const SAJU_NAMES = sajuNamesRaw as SajuNameEntry[];

export interface SajuPillar {
  stem: string; // 천간
  branch: string; // 지지
}
export interface SajuSnapshot {
  pillars: { year: SajuPillar; month: SajuPillar; day: SajuPillar; hour: SajuPillar };
  elementCounts: Record<FiveElement, number>;
  missing: FiveElement[];
}

export interface RecommendedName {
  id: string;
  name: string;
  hanja: string;
  reading: string;
  meaning: string;
  elements: FiveElement[]; // 자원오행(표시용)
}

const GENERATES: Record<FiveElement, FiveElement> = { 목: "화", 화: "토", 토: "금", 금: "수", 수: "목" }; // 상생
const CONTROLS: Record<FiveElement, FiveElement> = { 목: "토", 토: "수", 수: "화", 화: "금", 금: "목" }; // 상극

// 추천 후보 필터(2026-09-24 확정) — 사전에는 1글자·4글자 이름도 보존돼 있지만(외자·특수 케이스),
// 백엔드 저장 검증(ApplicationMember.validateNameFormat)은 성씨 제외 2~3글자만 허용한다. 추천했다가
// 저장 시 거절되는 걸 막기 위해 추천 후보에서만 제외한다 — 사전 원본·백엔드 검증 자체는 바꾸지 않는다.
function isRecommendable(entry: SajuNameEntry): boolean {
  const length = Array.from(entry.name).length;
  return length === 2 || length === 3;
}

// 결핍 가중치(saju 레포 recommend.py 규칙): 없음(0)=3, 약함(1)=1, 그 외 0.
function needWeight(count: number): number {
  return count === 0 ? 3 : count === 1 ? 1 : 0;
}

// 이름 점수 = 자원오행평균*2 + 발음오행평균*1 + 상생/상극 보정 + 결핍 커버 보너스(recommend.py 이식).
function scoreName(entry: SajuNameEntry, counts: Record<FiveElement, number>): number {
  const avg = (els: FiveElement[]) => els.reduce((s, e) => s + needWeight(counts[e]), 0) / (els.length || 1);
  let score = avg(entry.jawon) * 2 + avg(entry.eum) * 1;
  if (entry.jawon.length === 2) {
    const [a, b] = entry.jawon;
    if (GENERATES[a] === b || GENERATES[b] === a) score += 0.3;
    if (CONTROLS[a] === b || CONTROLS[b] === a) score -= 0.3;
  }
  const covered = new Set(entry.jawon.filter((e) => counts[e] <= 1));
  if (covered.size > 1) score += 0.5 * (covered.size - 1);
  return score;
}

// 결정적 이름 추천 — 추천 불가 후보(1글자·4글자)를 먼저 제외한 뒤 점수화해 score DESC, 동점이면
// 사전 index(원본 사전에서의 고정 위치) ASC로 상위 limit개. 필터는 점수 계산·정렬·상위 limit개
// 절단보다 먼저 적용한다 — 상위 limit개를 먼저 뽑은 뒤 걸러내면 결과가 limit개보다 적어질 수 있다.
// 같은 만세력 입력·같은 사전 버전이면 항상 같은 후보가 같은 순서로 반환된다(무작위 없음).
export function recommendNames(saju: SajuSnapshot, limit = 5): RecommendedName[] {
  const scored = SAJU_NAMES
    .map((n, index) => ({ n, index }))
    .filter(({ n }) => isRecommendable(n))
    .map(({ n, index }) => ({ n, index, score: scoreName(n, saju.elementCounts) }));
  scored.sort((a, b) => b.score - a.score || a.index - b.index);
  return scored.slice(0, limit).map(({ n }) => ({
    id: `${n.name}|${n.hanja}`,
    name: n.name,
    hanja: n.hanja,
    reading: n.reading,
    meaning: n.meaning,
    elements: n.jawon,
  }));
}

// 확정 이름과 선택 이력은 이제 **백엔드에 저장**한다(프론트 localStorage 미사용, 데이터 유출 방지):
//  - 확정: POST /api/admin/applications/{id}/members/{mid}/name → application_members.name/chinese_name
//  - 선택이력: 위 호출이 name_selection_stats +1, GET /api/admin/name-selection-stats로 조회
// (기존 localStorage helper getSelectionCounts/incrementSelection/getChosen/setChosen/clearChosen 제거)
