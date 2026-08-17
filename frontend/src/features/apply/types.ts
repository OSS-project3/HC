// Type definitions for the multi-step application draft.
import type { CardType } from "../../data/cards";

export type ApplicantType = "personal" | "organization";
export type IssuanceMethod = "mobile" | "mobile_and_physical";

export interface ApplicantInfo {
  name: string;
  englishName?: string;
  nationality?: string;
  birthPlace?: string;
  birthDate?: string;
  birthTime?: string;
  birthTimeUnknown?: boolean;
  gender?: "male" | "female" | "";
  koreaEntryDate?: string;
  organizationName?: string;
  department?: string;
  studentNumber?: string;
  /** 학생증 개인 신청: 대학교 / 고등학교 구분. */
  schoolLevel?: "university" | "highschool";
  /** 학생증 개인 신청: 대학교명 · 고등학교명. */
  schoolName?: string;
  phone: string;
  email: string;
}

export interface RecipientInfo {
  sameAsApplicant: boolean;
  name: string;
  organizationName?: string;
  department?: string;
  phone: string;
  postalCode: string;
  address: string;
  addressDetail: string;
  deliveryRequest?: string;
}

export interface UploadFileInfo {
  name: string;
  size: number;
  /** Object URL for preview (images only). */
  previewUrl?: string;
  /** Browser File used for the multipart API call. It is never persisted. */
  file?: File;
}

export interface ApplicationDraft {
  applicantType: ApplicantType;
  designId?: string;
  cardType?: CardType;
  /** 견본 이미지 방향(가로/세로). 미설정 시 디자인의 기본 방향을 따른다. */
  cardOrientation?: "landscape" | "portrait";

  issuanceMethod: IssuanceMethod;
  quantity: number;

  applicant: ApplicantInfo;
  recipient: RecipientInfo;

  logoFile?: UploadFileInfo;
  sealFile?: UploadFileInfo;
  archiveFile?: UploadFileInfo;
  faceFile?: UploadFileInfo;

  consultationConfirmed: boolean;
  disclaimerConfirmed: boolean;
  depositorName: string;
}

export const emptyApplicant: ApplicantInfo = {
  name: "",
  englishName: "",
  nationality: "",
  birthPlace: "",
  birthDate: "",
  birthTime: "",
  birthTimeUnknown: false,
  gender: "",
  koreaEntryDate: "",
  organizationName: "",
  department: "",
  studentNumber: "",
  schoolLevel: "university",
  schoolName: "",
  phone: "",
  email: "",
};

export const emptyRecipient: RecipientInfo = {
  sameAsApplicant: false,
  name: "",
  organizationName: "",
  department: "",
  phone: "",
  postalCode: "",
  address: "",
  addressDetail: "",
  deliveryRequest: "",
};

export function createEmptyDraft(): ApplicationDraft {
  return {
    applicantType: "personal",
    issuanceMethod: "mobile",
    quantity: 1,
    applicant: { ...emptyApplicant },
    recipient: { ...emptyRecipient },
    consultationConfirmed: false,
    disclaimerConfirmed: false,
    depositorName: "",
  };
}

export const STEP_LABELS = ["유형 선택", "정보 입력", "사진 / 파일 등록", "최종 확인", "신청 완료"] as const;
