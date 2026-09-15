import { useRef, useState } from "react";
import type { ApplicationDraft } from "./types";

export function useInfoValidation(draft: ApplicationDraft, onNext: () => void) {
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";
  const [showErrors, setShowErrors] = useState(false);
  const fieldRefs = useRef<Record<string, HTMLElement | null>>({});
  const registerField = (key: string) => (el: HTMLElement | null) => {
    fieldRefs.current[key] = el;
  };

  const filled = (value?: string) => (value ?? "").trim().length > 0;
  // Korean phone (mobile/landline, hyphens optional) and a basic email shape.
  const phonePattern = /^0\d{1,2}-?\d{3,4}-?\d{4}$/;
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isValidPhone = (value?: string) => phonePattern.test((value ?? "").replace(/\s/g, ""));
  const isValidEmail = (value?: string) => emailPattern.test((value ?? "").trim());

  const missingKeys: string[] = [];
  if (isOrg) {
    if (!filled(draft.applicant.name)) missingKeys.push("name");
    if (!filled(draft.applicant.organizationName)) missingKeys.push("organizationName");
    if (!isValidPhone(draft.applicant.phone)) missingKeys.push("phone");
    if (!isValidEmail(draft.applicant.email)) missingKeys.push("email");
  } else {
    if (!filled(draft.applicant.englishName)) missingKeys.push("englishName");
    if (!filled(draft.applicant.nationality)) missingKeys.push("nationality");
    if (!filled(draft.applicant.birthPlace)) missingKeys.push("birthPlace");
    if (!filled(draft.applicant.birthDate)) missingKeys.push("birthDate");
    if (!filled(draft.applicant.gender)) missingKeys.push("gender");
    if (isStudent) {
      const isUniversity = (draft.applicant.schoolLevel ?? "university") === "university";
      if (!filled(draft.applicant.schoolName)) missingKeys.push("schoolName");
      if (isUniversity) {
        if (!filled(draft.applicant.studentNumber)) missingKeys.push("studentNumber");
        if (!filled(draft.applicant.department)) missingKeys.push("department");
      }
    }
    // 카드 표기용 주소 — 학생증은 카드에 주소를 표시하지 않으므로 받지 않고, 그 외 카드종류는
    // 개인 신청도 필수다(백엔드 ApplicationService.validateCardAddress와 동일한 정책). 단체(엑셀
    // 업로드) 신청은 이 폼이 아니라 엑셀의 "주소" 컬럼으로 받으므로 여기서는 검증하지 않는다.
    if (!isStudent) {
      if (!filled(draft.applicant.address)) missingKeys.push("address");
    }
    if (!filled(draft.applicant.koreaEntryDate)) missingKeys.push("koreaEntryDate");
    if (!isValidPhone(draft.applicant.phone)) missingKeys.push("phone");
    if (!isValidEmail(draft.applicant.email)) missingKeys.push("email");
  }
  if (isPhysical) {
    if (!filled(draft.recipient.name)) missingKeys.push("recipient.name");
    if (!isValidPhone(draft.recipient.phone)) missingKeys.push("recipient.phone");
    if (!filled(draft.recipient.postalCode)) missingKeys.push("recipient.postalCode");
    if (!filled(draft.recipient.address)) missingKeys.push("recipient.address");
  }

  const isComplete = missingKeys.length === 0;
  const hasError = (key: string) => showErrors && missingKeys.includes(key);
  const inputCls = (key: string) => `field__input${hasError(key) ? " field__input--invalid" : ""}`;
  // Format hints appear only when a field has content but the shape is wrong — an
  // empty required field is already conveyed by the red outline alone.
  const showPhoneFormat = (value?: string) => showErrors && filled(value) && !isValidPhone(value);
  const showEmailFormat = (value?: string) => showErrors && filled(value) && !isValidEmail(value);

  const handleNext = () => {
    if (!isComplete) {
      setShowErrors(true);
      const el = fieldRefs.current[missingKeys[0]];
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "center" });
        const focusTarget = el.querySelector<HTMLElement>("input, select, textarea, button") ?? el;
        focusTarget.focus?.({ preventScroll: true });
      }
      return;
    }
    onNext();
  };

  return { fieldRefs, registerField, inputCls, hasError, showPhoneFormat, showEmailFormat, isComplete, handleNext };
}
