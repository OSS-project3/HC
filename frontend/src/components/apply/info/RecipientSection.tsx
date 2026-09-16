import { useRef } from "react";
import type { ApplicationDraft, RecipientInfo } from "../../../features/apply/types";
import { useInfoValidation } from "../../../features/apply/useInfoValidation";
import { useLanguage } from "../../../features/i18n/LanguageContext";
import { openPostcodeSearch } from "../../../lib/postcode";
import { showToast } from "../../ui/toast";
export function RecipientSection({ draft, setRecipient, validation }: {
  draft: ApplicationDraft;
  setRecipient: (patch: Partial<RecipientInfo>) => void;
  validation: ReturnType<typeof useInfoValidation>;
}) {
  const { t } = useLanguage();
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";
  const orgLabel = isStudent ? "학교명" : "법인·단체명";
  const addressDetailRef = useRef<HTMLInputElement | null>(null);
  const { registerField, inputCls, showPhoneFormat, fieldRefs } = validation;
  const toggleSame = (checked: boolean) => {
    if (checked) {
      // Copy applicant → recipient (only the shared identity fields).
      setRecipient({
        sameAsApplicant: true,
        name: isOrg ? draft.applicant.name : draft.applicant.englishName ?? "",
        phone: draft.applicant.phone,
        organizationName: isOrg ? draft.applicant.organizationName : "",
        department: isOrg ? draft.applicant.department : "",
      });
    } else {
      setRecipient({ sameAsApplicant: false });
    }
  };

 return (
          <section className="info-col">
            <div className="info-col__head">
              <h3 className="info-col__title">{t("수령인 정보")}</h3>
              <label className="check">
                <input
                  type="checkbox"
                  checked={draft.recipient.sameAsApplicant}
                  onChange={(e) => toggleSame(e.target.checked)}
                />
                <span>{t("신청인과 동일합니다")}</span>
              </label>
            </div>
            <label className="field" ref={registerField("recipient.name")}>
              <span className="field__label">
                {t("이름")}<span className="req">*</span>
              </span>
              <input
                className={inputCls("recipient.name")}
                value={draft.recipient.name}
                onChange={(e) => setRecipient({ name: e.target.value })}
              />
            </label>
            {isOrg && (
              <div className="field-row">
                <label className="field">
                  <span className="field__label">{t(orgLabel)}</span>
                  <input
                    className="field__input"
                    value={draft.recipient.organizationName ?? ""}
                    onChange={(e) => setRecipient({ organizationName: e.target.value })}
                    placeholder={t(isStudent ? "학교명 (선택)" : "법인·단체명 (선택)")}
                  />
                </label>
                <label className="field">
                  <span className="field__label">{t("부서")}</span>
                  <input
                    className="field__input"
                    value={draft.recipient.department ?? ""}
                    onChange={(e) => setRecipient({ department: e.target.value })}
                    placeholder={t("부서 (선택)")}
                  />
                </label>
              </div>
            )}
            <label className="field" ref={registerField("recipient.phone")}>
              <span className="field__label">
                {t("연락처")}<span className="req">*</span>
              </span>
              <input
                className={inputCls("recipient.phone")}
                inputMode="tel"
                value={draft.recipient.phone}
                onChange={(e) => setRecipient({ phone: e.target.value })}
                placeholder="010-1234-5678"
              />
              {showPhoneFormat(draft.recipient.phone) && (
                <span className="field-error">{t("올바른 연락처 형식으로 입력해 주세요. (예: 010-1234-5678)")}</span>
              )}
            </label>
            <div className="field">
              <span className="field__label">
                {t("배송지 주소")}<span className="req">*</span>
              </span>
              <div className="field__with-btn">
                <input
                  ref={registerField("recipient.postalCode")}
                  className={inputCls("recipient.postalCode")}
                  value={draft.recipient.postalCode}
                  onChange={(e) => setRecipient({ postalCode: e.target.value })}
                  placeholder={t("우편번호")}
                />
                <button type="button" className="postal-btn" onClick={async () => {
                  try {
                    await openPostcodeSearch((postalCode, address) => {
                      setRecipient({ postalCode, address });
                      requestAnimationFrame(() => addressDetailRef.current?.focus());
                    });
                  } catch {
                    showToast("주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
                  }
                }}>
                  {t("우편번호 찾기")}
                </button>
              </div>
              <input
                ref={(el) => {
                  addressDetailRef.current = el;
                  fieldRefs.current["recipient.address"] = el;
                }}
                className={inputCls("recipient.address")}
                value={draft.recipient.address}
                onChange={(e) => setRecipient({ address: e.target.value })}
                placeholder={t("기본 주소")}
              />
              <input
                className="field__input"
                value={draft.recipient.addressDetail}
                onChange={(e) => setRecipient({ addressDetail: e.target.value })}
                placeholder={t("상세 주소를 입력해 주세요")}
              />
            </div>
            <label className="field">
              <span className="field__label">{t("배송 요청사항")}</span>
              <input
                className="field__input"
                value={draft.recipient.deliveryRequest ?? ""}
                onChange={(e) => setRecipient({ deliveryRequest: e.target.value })}
              />
            </label>
          </section>
 );
}
