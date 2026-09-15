// 순수 로직 테스트 — 브라우저·백엔드 없이 실행된다(docker 스택 불필요).
// §1.19 이름 추천 결정성(admin-saju.md: 무작위 금지, score DESC → 사전 index ASC, 상위 5개)과
// 1-E-2 만세력 복원 adapter(손상 데이터 거절, 미확정 주 마스킹) 검증.
import { test, expect } from "@playwright/test";
import { recommendNames, type SajuSnapshot } from "../src/lib/namingRecommendations";
import { fromActiveManseryeokResult } from "../src/lib/saju";
import type { ManseryeokActiveResult } from "../src/services/api";

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
