// 화면 문자열 ↔ 백엔드 API enum 변환 공통 매퍼 (FRONTEND_API_INTEGRATION_SPEC §2.5).
// 카드 종류(slug↔cardTypeId) 매핑은 data/cards.ts의 cardTypeIds/cardTypeById를 사용한다.
import type { ApplicantType, IssuanceMethod } from "./types";

export type ApplicationTypeApi = "INDIVIDUAL" | "GROUP";
export type IssueTypeApi = "MOBILE" | "MOBILE_AND_PHYSICAL";
export type GenderApi = "MALE" | "FEMALE";
export type OrientationApi = "LANDSCAPE" | "PORTRAIT";
export type SchoolTypeApi = "UNIVERSITY" | "HIGH_SCHOOL";

export const toApplicationType = (t: ApplicantType): ApplicationTypeApi =>
  t === "organization" ? "GROUP" : "INDIVIDUAL";

export const toIssueType = (m: IssuanceMethod): IssueTypeApi =>
  m === "mobile_and_physical" ? "MOBILE_AND_PHYSICAL" : "MOBILE";

export const toGender = (g?: "male" | "female" | ""): GenderApi | undefined =>
  g === "male" ? "MALE" : g === "female" ? "FEMALE" : undefined;

export const toOrientation = (o?: "landscape" | "portrait"): OrientationApi =>
  o === "portrait" ? "PORTRAIT" : "LANDSCAPE";

export const toSchoolType = (l?: "university" | "highschool"): SchoolTypeApi =>
  l === "highschool" ? "HIGH_SCHOOL" : "UNIVERSITY";
