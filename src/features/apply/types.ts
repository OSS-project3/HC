import type { CardType } from "../../data/cards";

export type ApplicantType = "personal" | "organization";
export type IssuanceMethod = "mobile" | "mobile_and_physical";

export interface ApplicantInfo {
  name: string;
  organizationName?: string;
  department?: string;
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
}

export interface ApplicationDraft {
  applicantType: ApplicantType;
  designId?: string;
  cardType?: CardType;

  issuanceMethod: IssuanceMethod;
  quantity: number;

  applicant: ApplicantInfo;
  recipient: RecipientInfo;

  logoFile?: UploadFileInfo;
  sealFile?: UploadFileInfo;
  archiveFile?: UploadFileInfo;

  consultationConfirmed: boolean;
  depositorName: string;
}

export const emptyApplicant: ApplicantInfo = {
  name: "",
  organizationName: "",
  department: "",
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
    depositorName: "",
  };
}

export const STEP_LABELS = ["유형 선택", "정보 입력", "사진 / 파일 등록", "최종 확인", "신청 완료"] as const;
