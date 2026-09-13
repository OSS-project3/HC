// 관리자 "이름 추천/만세력" 플로우 데이터.
// - 추천 이름: 실제 데이터(sajuNames.json — saju 레포 names.json + 사주 이름 결과.xlsx를 병합한 700개,
//   자원오행/발음오행/뜻 포함)를 오행 결핍 기반으로 점수화해 매번 무작위 8개를 추천한다(새로고침 시 새 조합).
// - 만세력(四柱)은 아직 실제 계산 백엔드가 없어 mock이다(신청별 결정적 생성). 실제 연동 설계: docs/specs/admin-dashboard/DESIGN.md
import sajuNamesRaw from "./sajuNames.json";

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
export interface MockSaju {
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

const STEMS = ["갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"];
const BRANCHES = ["자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"];
const ELEMENTS: FiveElement[] = ["목", "화", "토", "금", "수"];
const GENERATES: Record<FiveElement, FiveElement> = { 목: "화", 화: "토", 토: "금", 금: "수", 수: "목" }; // 상생
const CONTROLS: Record<FiveElement, FiveElement> = { 목: "토", 토: "수", 수: "화", 화: "금", 금: "목" }; // 상극

// 문자열 seed → 안정적인 해시(만세력은 신청별로 항상 동일하게).
function hash(seed: string): number {
  let h = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

// 시연용 가짜 만세력. seed(예: 신청번호+멤버)로 결정적으로 생성한다.
export function mockSaju(seed: string): MockSaju {
  const h = hash(seed);
  const pick = (n: number, mod: number) => ((h >> n) % mod + mod) % mod;
  const pillar = (n: number): SajuPillar => ({ stem: STEMS[pick(n, 10)], branch: BRANCHES[pick(n + 3, 12)] });
  const counts: Record<FiveElement, number> = { 목: 0, 화: 0, 토: 0, 금: 0, 수: 0 };
  for (let i = 0; i < 8; i++) counts[ELEMENTS[(h >> (i * 2)) % 5]] += 1;
  const missing = ELEMENTS.filter((e) => counts[e] === 0);
  return {
    pillars: { year: pillar(0), month: pillar(6), day: pillar(12), hour: pillar(18) },
    elementCounts: counts,
    missing,
  };
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

// 오행 결핍을 보완하는 이름을 상위 풀로 뽑고, 그 안에서 무작위 8개를 추천한다.
// 무작위라 새로고침(컴포넌트 재마운트)마다 새로운 조합이 나온다.
export function mockRecommendations(_seed: string, saju: MockSaju, limit = 8): RecommendedName[] {
  const scored = SAJU_NAMES.map((n) => ({ n, score: scoreName(n, saju.elementCounts) }));
  scored.sort((a, b) => b.score - a.score);
  const pool = scored.slice(0, Math.max(limit * 6, 48)); // 상위 관련 이름 풀
  // Fisher-Yates 셔플(Math.random) — 매 호출마다 다른 조합.
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, limit).map(({ n }) => ({
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
