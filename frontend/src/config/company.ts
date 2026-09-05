/**
 * Company / contact information.
 *
 * NOTE: the values below are taken from the mock-ups and may be placeholders.
 * Confirm the final values with the client before production deployment.
 */
const currentHostname = window.location.hostname.toLowerCase();

export const isAssociationSite = currentHostname === "hanse.kr";

export const companyInfo = {
  nameKo: "(주)한글과 세종",
  nameEn: "HANGUL & SEJONG",
  tagline: "한글 오행 기반 한국 이름 추천",
  address: "전북특별자치도 전주시 완산구 최명희길 11",
  phone: "010-7538-2383",
  fax: "063-123-4567",
  representative: "유철호",
  representativeTitle: "(주)이사장",
  registrationNumber: "123-45-67890",
  patentNumber: "제10-2026-0073719호",
  businessHours: "평일 09:30 - 17:00",
  businessHoursNote: "(주말·공휴일 휴무)",
  lunchHours: "점심 12:00 - 13:00",
  email: "chy0051@naver.com",
  copyright: "© 2026 한글과 세종 ALL RIGHTS RESERVED",
} as const;

const associationNameKo =
  companyInfo.nameKo[0] + "사" + companyInfo.nameKo.slice(2);
export const siteNameKo = [companyInfo.nameKo, associationNameKo][+isAssociationSite];

/** 사단법인 페이지(hanse.kr)에서만 다르게 표시하는 값 — 푸터 전용. */
const associationAddress = "전주시 완산구 기린대로 192 (예원빌딩 9층)";
const associationRepresentativeTitle = "이사장";
export const footerAddress = [companyInfo.address, associationAddress][+isAssociationSite];
export const footerRepresentativeTitle =
  [companyInfo.representativeTitle, associationRepresentativeTitle][+isAssociationSite];

/** Bank / deposit details shown on the application completion step. */
export const bankInfo = {
  bankName: "농협은행",
  accountNumber: "352-7538-2383-83",
  accountHolder: "유철호",
} as const;
