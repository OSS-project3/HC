// 관리자 "이름 추천/만세력" 플로우의 **임시 mock 데이터**.
// ⚠️ 실제 만세력(四柱) 계산과 이름 추천은 백엔드가 없어 아직 미구현이다. 이 파일의 값은 UI 시연용
//    가짜 데이터이며, github.com/05solar/saju 의 데이터/코드를 복사한 것이 아니다(해당 레포는
//    라이선스가 없어 재배포 불가). 실제 연동 설계는 docs/specs/admin-dashboard/DESIGN.md 참고.

export type FiveElement = "목" | "화" | "토" | "금" | "수";

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
  name: string; // 한글 이름
  hanja: string;
  reading: string; // 한자 훈음
  meaning: string;
  elements: FiveElement[]; // 자원오행(글자별)
}

const STEMS = ["갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"];
const BRANCHES = ["자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"];
const ELEMENTS: FiveElement[] = ["목", "화", "토", "금", "수"];

// 문자열 seed → 안정적인 해시(같은 신청은 항상 같은 mock 값이 나오도록).
function hash(seed: string): number {
  let h = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

// 시연용 가짜 만세력. seed(예: 신청번호+이름)로 결정적으로 생성한다.
export function mockSaju(seed: string): MockSaju {
  const h = hash(seed);
  const pick = (n: number, mod: number) => ((h >> n) % mod + mod) % mod;
  const pillar = (n: number): SajuPillar => ({ stem: STEMS[pick(n, 10)], branch: BRANCHES[pick(n + 3, 12)] });
  const counts: Record<FiveElement, number> = { 목: 0, 화: 0, 토: 0, 금: 0, 수: 0 };
  // 8글자(4주 × 천간/지지)를 오행에 분배.
  for (let i = 0; i < 8; i++) counts[ELEMENTS[(h >> (i * 2)) % 5]] += 1;
  const missing = ELEMENTS.filter((e) => counts[e] === 0);
  return {
    pillars: { year: pillar(0), month: pillar(6), day: pillar(12), hour: pillar(18) },
    elementCounts: counts,
    missing,
  };
}

// 시연용 추천 이름 풀(자체 제작 소량 mock — 실제 추천 데이터 아님).
const NAME_POOL: RecommendedName[] = [
  { id: "n-jiho", name: "지호", hanja: "智浩", reading: "지혜 지(智) 넓을 호(浩)", meaning: "지혜가 바다처럼 넓은 사람", elements: ["화", "수"] },
  { id: "n-seoyeon", name: "서연", hanja: "瑞蓮", reading: "상서로울 서(瑞) 연꽃 연(蓮)", meaning: "상서로운 기운을 지닌 맑은 사람", elements: ["금", "목"] },
  { id: "n-doyun", name: "도윤", hanja: "度潤", reading: "법도 도(度) 윤택할 윤(潤)", meaning: "품이 넓고 윤택하게 베푸는 사람", elements: ["토", "수"] },
  { id: "n-hana", name: "하나", hanja: "河娜", reading: "물 하(河) 아름다울 나(娜)", meaning: "강물처럼 유연하고 아름다운 사람", elements: ["수", "화"] },
  { id: "n-minjun", name: "민준", hanja: "旻峻", reading: "하늘 민(旻) 높을 준(峻)", meaning: "하늘처럼 높고 곧은 사람", elements: ["화", "토"] },
  { id: "n-yerin", name: "예린", hanja: "叡潾", reading: "밝을 예(叡) 맑을 린(潾)", meaning: "밝고 맑게 빛나는 사람", elements: ["금", "수"] },
  { id: "n-gunwoo", name: "건우", hanja: "健宇", reading: "굳셀 건(健) 집 우(宇)", meaning: "굳세고 큰 그릇을 지닌 사람", elements: ["목", "토"] },
  { id: "n-chaeun", name: "채은", hanja: "彩銀", reading: "채색 채(彩) 은 은(銀)", meaning: "빛나는 재능과 단단함을 지닌 사람", elements: ["화", "금"] },
  { id: "n-sian", name: "시안", hanja: "時安", reading: "때 시(時) 편안 안(安)", meaning: "때를 아는 편안한 사람", elements: ["금", "토"] },
  { id: "n-yul", name: "이율", hanja: "利潤", reading: "이로울 이(利) 윤택할 율(潤)", meaning: "이롭고 윤택한 삶을 이루는 사람", elements: ["금", "수"] },
];

// 결핍 오행을 보완하는 이름을 우선 노출하도록 seed 기반으로 정렬/선별(시연용 근사).
export function mockRecommendations(seed: string, saju: MockSaju, limit = 6): RecommendedName[] {
  const scored = NAME_POOL.map((n) => {
    const supply = n.elements.filter((e) => saju.missing.includes(e)).length;
    return { n, score: supply * 10 + (hash(seed + n.id) % 5) };
  });
  scored.sort((a, b) => b.score - a.score);
  return scored.slice(0, limit).map((s) => s.n);
}

// ── 이름 선택 이력(+1) — 실제 API가 없어 localStorage로 시연 ──────────────
const SELECTION_KEY = "admin:name-selection-counts";

export function getSelectionCounts(): Record<string, number> {
  try {
    return JSON.parse(localStorage.getItem(SELECTION_KEY) || "{}") as Record<string, number>;
  } catch {
    return {};
  }
}

export function incrementSelection(nameId: string): number {
  const counts = getSelectionCounts();
  counts[nameId] = (counts[nameId] ?? 0) + 1;
  localStorage.setItem(SELECTION_KEY, JSON.stringify(counts));
  return counts[nameId];
}
