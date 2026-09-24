// 순수 로직 테스트 — 브라우저·백엔드 없이 실행된다(docker 스택 불필요).
// §1.19 이름 추천 결정성(admin-saju.md: 무작위 금지, score DESC → 사전 index ASC, 상위 5개)과
// 1-E-2 만세력 복원 adapter(손상 데이터 거절, 미확정 주 마스킹) 검증.
import { test, expect } from "@playwright/test";
import { recommendNames, type SajuSnapshot } from "../src/lib/namingRecommendations";
import { fromActiveManseryeokResult } from "../src/lib/saju";
import type { ManseryeokActiveResult } from "../src/services/api";
import sajuNamesRaw from "../src/data/sajuNames.json" with { type: "json" };

const SAMPLE_SAJU: SajuSnapshot = {
  pillars: {
    year: { stem: "을", branch: "해" },
    month: { stem: "임", branch: "오" },
    day: { stem: "정", branch: "축" },
    hour: { stem: "정", branch: "미" },
  },
  elementCounts: { 목: 1, 화: 3, 토: 2, 금: 0, 수: 2 },
  missing: ["금"],
};

test.describe("recommendNames — 결정적 추천(§1.19)", () => {
  test("같은 입력이면 항상 같은 상위 5개가 같은 순서로 나온다", () => {
    const first = recommendNames(SAMPLE_SAJU);
    const second = recommendNames(SAMPLE_SAJU);
    const third = recommendNames({ ...SAMPLE_SAJU, elementCounts: { ...SAMPLE_SAJU.elementCounts } });

    expect(first).toHaveLength(5);
    expect(second).toEqual(first);
    expect(third).toEqual(first);
  });

  test("중복 후보가 없다", () => {
    const recs = recommendNames(SAMPLE_SAJU);
    expect(new Set(recs.map((r) => r.id)).size).toBe(recs.length);
  });

  test("오행 입력이 다르면 다른 추천이 나올 수 있고, 각각은 여전히 결정적이다", () => {
    const other: SajuSnapshot = {
      ...SAMPLE_SAJU,
      elementCounts: { 목: 0, 화: 2, 토: 2, 금: 2, 수: 2 },
      missing: ["목"],
    };
    expect(recommendNames(other)).toEqual(recommendNames(other));
  });
});

// 추천 후보 필터(2026-09-24 확정) — 사전에는 1글자·4글자 이름도 보존돼 있지만(원본 삭제 안 함),
// 백엔드 저장 검증은 2~3글자만 허용하므로 추천 후보에서만 제외한다. 필터는 점수·순위와 무관하게
// 항상 적용돼야 하므로 여러 오행 입력으로 넓게(limit=700, 사전 전체) 확인한다.
test.describe("recommendNames — 추천 후보 길이 필터(2026-09-24)", () => {
  const INPUTS: SajuSnapshot[] = [
    SAMPLE_SAJU,
    { ...SAMPLE_SAJU, elementCounts: { 목: 0, 화: 2, 토: 2, 금: 2, 수: 2 }, missing: ["목"] },
    { ...SAMPLE_SAJU, elementCounts: { 목: 2, 화: 0, 토: 0, 금: 3, 수: 3 }, missing: ["화", "토"] },
  ];

  test("1글자·4글자 이름은 어떤 오행 입력에서도 추천되지 않는다", () => {
    for (const saju of INPUTS) {
      const all = recommendNames(saju, 700);
      for (const rec of all) {
        const length = Array.from(rec.name).length;
        expect(length === 2 || length === 3).toBe(true);
      }
    }
  });

  test("추천 가능 후보는 정확히 682건(700 - 1글자 14건 - 4글자 4건)이다", () => {
    const all = recommendNames(SAMPLE_SAJU, 700);
    expect(all).toHaveLength(682);
  });

  test("기본 호출은 필터 적용 후에도 항상 5건을 반환한다", () => {
    for (const saju of INPUTS) {
      expect(recommendNames(saju)).toHaveLength(5);
    }
  });
});

// 태산/현산 한자 오류 수정(2026-09-24) — 사전 원본에 한글 이름과 한자 글자 수가 어긋난 손상
// 데이터가 있었다(예: "태산"/"兌示산" — 2글자 이름에 3글자 한자). 이미 사전에 존재하는 단독 이름
// "산"(한자 "祘")을 근거로 "兌祘"/"鉉祘"로 바로잡았다 — 추천 가능 조건(2~3글자)과 무관하게 데이터
// 자체의 정합성 회귀를 잡기 위한 테스트.
test.describe("사전 데이터 — 태산/현산 한자 정합성(2026-09-24)", () => {
  interface SajuNameJson { name: string; hanja: string; reading: string }
  const SAJU_NAMES = sajuNamesRaw as SajuNameJson[];

  test("태산의 한자는 이름과 글자 수가 같은 兌祘이다", () => {
    const entry = SAJU_NAMES.find((n) => n.name === "태산");
    expect(entry?.hanja).toBe("兌祘");
    expect(entry?.reading).toBe("기쁠 태(兌) 셈 산(祘)");
  });

  test("현산의 한자는 이름과 글자 수가 같은 鉉祘이다", () => {
    const entry = SAJU_NAMES.find((n) => n.name === "현산");
    expect(entry?.hanja).toBe("鉉祘");
    expect(entry?.reading).toBe("솥귀 현(鉉) 셈 산(祘)");
  });

  test("사전 전체에 이름·한자 글자 수가 어긋난 항목이 더 이상 없다", () => {
    const mismatched = SAJU_NAMES.filter((n) => n.hanja && Array.from(n.hanja).length !== Array.from(n.name).length);
    expect(mismatched).toEqual([]);
  });
});

function activeResult(overrides: Partial<ManseryeokActiveResult> = {}): ManseryeokActiveResult {
  return {
    timezoneId: "America/New_York",
    longitude: -74.006,
    selectedOffset: "-04:00",
    utcInstant: "1995-06-15T19:00:00Z",
    timeAccuracy: "EXACT",
    confirmedPillars: {
      year: { stem: "을", branch: "해" },
      month: { stem: "임", branch: "오" },
      day: { stem: "정", branch: "축" },
      hour: { stem: "정", branch: "미" },
    },
    uncertainPillars: [],
    elementCounts: { 목: 1, 화: 3, 토: 2, 금: 0, 수: 2 },
    calculatedAt: "2026-09-14T00:00:00",
    ...overrides,
  };
}

test.describe("fromActiveManseryeokResult — 복원 adapter(1-E-2)", () => {
  test("정상 결과를 화면 모델로 변환한다(오행 결핍 포함)", () => {
    const saju = fromActiveManseryeokResult(activeResult());
    expect(saju).not.toBeNull();
    expect(saju!.pillars.year).toEqual({ stem: "을", branch: "해" });
    expect(saju!.missing).toEqual(["금"]);
  });

  test("uncertainPillars의 주는 저장값이 있어도 '—'로 마스킹한다", () => {
    const saju = fromActiveManseryeokResult(activeResult({ timeAccuracy: "UNKNOWN", uncertainPillars: ["hour"] }));
    expect(saju).not.toBeNull();
    expect(saju!.pillars.hour).toEqual({ stem: "—", branch: "—" });
    expect(saju!.pillars.day).toEqual({ stem: "정", branch: "축" });
  });

  test("필수 pillar 누락은 임의 보정하지 않고 null(데이터 오류)", () => {
    const broken = activeResult();
    delete broken.confirmedPillars!.year;
    expect(fromActiveManseryeokResult(broken)).toBeNull();
  });

  test("elementCounts 누락/손상은 null(데이터 오류)", () => {
    expect(fromActiveManseryeokResult(activeResult({ elementCounts: undefined }))).toBeNull();
    expect(fromActiveManseryeokResult(activeResult({ elementCounts: { 목: 1 } }))).toBeNull();
  });
});
