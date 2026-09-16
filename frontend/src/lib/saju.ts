// 실제 만세력(四柱) 계산 — saju 레포와 동일하게 npm `manseryeok` 라이브러리를 브라우저에서 사용한다.
// 멤버의 생년월일(+출생시간)로 4주를 계산하고, 8자(천간4+지지4)의 오행 분포를 낸다.
import { calculateFourPillars } from "manseryeok";
import type { FiveElement, SajuSnapshot } from "../lib/namingRecommendations";
import type { ManseryeokActiveResult } from "../services/api";

const ELEMENTS: FiveElement[] = ["목", "화", "토", "금", "수"];

// 생년월일/출생시간 → 실제 만세력. 값이 없거나 계산 불가(잘못된 날짜)면 null. 예시 표시·시간 미상 확정에만 사용한다.
export function computeMemberSaju(birthDate?: string, birthTime?: string): SajuSnapshot | null {
  if (!birthDate) return null;
  const [y, m, d] = birthDate.split("-").map(Number);
  if (!y || !m || !d) return null;
  const t = (birthTime ?? "").trim();
  const known = t.length > 0;
  const [hh, mm] = known ? t.split(":").map(Number) : [12, 0];

  let detail;
  try {
    detail = calculateFourPillars({
      year: y, month: m, day: d,
      hour: known ? hh : 12, minute: known ? mm : 0,
      isLunar: false, dayBoundary: "midnight",
    });
  } catch {
    return null;
  }

  const counts: Record<FiveElement, number> = { 목: 0, 화: 0, 토: 0, 금: 0, 수: 0 };
  for (const pair of [detail.yearElement, detail.monthElement, detail.dayElement, detail.hourElement]) {
    counts[pair.stem as FiveElement] += 1;
    counts[pair.branch as FiveElement] += 1;
  }
  const missing = ELEMENTS.filter((e) => counts[e] === 0);
  return {
    pillars: {
      year: { stem: detail.year.heavenlyStem, branch: detail.year.earthlyBranch },
      month: { stem: detail.month.heavenlyStem, branch: detail.month.earthlyBranch },
      day: { stem: detail.day.heavenlyStem, branch: detail.day.earthlyBranch },
      hour: { stem: detail.hour.heavenlyStem, branch: detail.hour.earthlyBranch },
    },
    elementCounts: counts,
    missing,
  };
}

export function computeMemberSajuFromResolved(utcInstant: string, longitude: number): SajuSnapshot | null {
  const instant = new Date(utcInstant);
  if (Number.isNaN(instant.getTime())) return null;

  const kst = new Date(instant.getTime() + 9 * 60 * 60 * 1000);
  let detail;
  try {
    detail = calculateFourPillars({
      year: kst.getUTCFullYear(),
      month: kst.getUTCMonth() + 1,
      day: kst.getUTCDate(),
      hour: kst.getUTCHours(),
      minute: kst.getUTCMinutes(),
      isLunar: false,
      dayBoundary: "midnight",
      trueSolarTime: { longitude, applyHistoricalDst: false },
    } as Parameters<typeof calculateFourPillars>[0] & { trueSolarTime: { longitude: number; applyHistoricalDst: boolean } });
  } catch {
    return null;
  }

  const counts: Record<FiveElement, number> = { 목: 0, 화: 0, 토: 0, 금: 0, 수: 0 };
  for (const pair of [detail.yearElement, detail.monthElement, detail.dayElement, detail.hourElement]) {
    counts[pair.stem as FiveElement] += 1;
    counts[pair.branch as FiveElement] += 1;
  }
  const missing = ELEMENTS.filter((e) => counts[e] === 0);
  return {
    pillars: {
      year: { stem: detail.year.heavenlyStem, branch: detail.year.earthlyBranch },
      month: { stem: detail.month.heavenlyStem, branch: detail.month.earthlyBranch },
      day: { stem: detail.day.heavenlyStem, branch: detail.day.earthlyBranch },
      hour: { stem: detail.hour.heavenlyStem, branch: detail.hour.earthlyBranch },
    },
    elementCounts: counts,
    missing,
  };
}

// 재진입 복원(1-E-2) — 저장된 활성 결과를 화면 모델(SajuSnapshot 호환)로 변환하는 순수 adapter.
// 필수 pillar·오행 값이 손상·누락됐으면 임의 보정하지 않고 null을 반환한다(호출부가 데이터 오류로 표시).
// uncertainPillars에 포함된 주는 confirmedPillars에 없어도 되며 "—"로 표시한다(확정값처럼 보이지 않게).
export function fromActiveManseryeokResult(result: ManseryeokActiveResult): SajuSnapshot | null {
  const counts = result.elementCounts;
  if (!counts) return null;
  const normalized: Record<FiveElement, number> = { 목: 0, 화: 0, 토: 0, 금: 0, 수: 0 };
  for (const el of ELEMENTS) {
    const value = counts[el];
    if (typeof value !== "number" || !Number.isFinite(value) || value < 0) return null;
    normalized[el] = value;
  }

  const uncertain = new Set(result.uncertainPillars ?? []);
  const pillars: Partial<SajuSnapshot["pillars"]> = {};
  for (const key of ["year", "month", "day", "hour"] as const) {
    if (uncertain.has(key)) {
      // 미확정 주는 저장값이 있더라도 확정값처럼 표시하지 않는다.
      pillars[key] = { stem: "—", branch: "—" };
      continue;
    }
    const stored = result.confirmedPillars?.[key];
    if (stored && typeof stored.stem === "string" && stored.stem && typeof stored.branch === "string" && stored.branch) {
      pillars[key] = { stem: stored.stem, branch: stored.branch };
    } else {
      return null;
    }
  }

  return {
    pillars: pillars as SajuSnapshot["pillars"],
    elementCounts: normalized,
    missing: ELEMENTS.filter((e) => normalized[e] === 0),
  };
}

export function toConfirmedPillars(saju: SajuSnapshot): Record<string, { stem: string; branch: string }> {
  return {
    year: saju.pillars.year,
    month: saju.pillars.month,
    day: saju.pillars.day,
    hour: saju.pillars.hour,
  };
}

export function makeSajuInputHash(parts: Array<string | number | undefined | null>): string {
  const input = parts.map((part) => part ?? "").join("|");
  let hash = 0x811c9dc5;
  for (let i = 0; i < input.length; i += 1) {
    hash ^= input.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return (hash >>> 0).toString(16).padStart(8, "0");
}
