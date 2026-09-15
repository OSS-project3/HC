import type { ApplicantInfo, ApplicationDraft } from "../../../features/apply/types";
import { useInfoValidation } from "../../../features/apply/useInfoValidation";
import { useSchoolSearch } from "../../../features/apply/useSchoolSearch";
import { useLanguage } from "../../../features/i18n/LanguageContext";
import { SearchableSelectField } from "../../ui/SearchableSelectField";
interface InfoSectionProps {
  draft: ApplicationDraft;
  setApplicant: (patch: Partial<ApplicantInfo>) => void;
  validation: ReturnType<typeof useInfoValidation>;
}
export function OrganizationFields({ draft, setApplicant, validation }: InfoSectionProps) {
  const { t } = useLanguage();
  const isStudent = draft.cardType === "student";
  const orgLabel = isStudent ? "학교명" : "법인·단체명";
  const { registerField, inputCls, hasError, showPhoneFormat, showEmailFormat } = validation;

  const { manualSchoolInput, setManualSchoolInput, selectSchool, setSchoolQuery, schoolSelectOptions } = useSchoolSearch(draft, setApplicant);

  return (
            <>
              <label className="field" ref={registerField("name")}>
                <span className="field__label">
                  {t("이름")}<span className="req">*</span>
                </span>
                <input
                  className={inputCls("name")}
                  value={draft.applicant.name}
                  onChange={(e) => setApplicant({ name: e.target.value })}
                  placeholder={t("담당자 이름")}
                />
              </label>
              {isStudent && !manualSchoolInput ? (
                <>
                  <fieldset className="form-block">
                    <legend className="form-block__legend">
                      {t("학교 구분")}<span className="req">*</span>
                    </legend>
                    <div className="radio-row">
                      <label className="check">
                        <input
                          type="radio"
                          name="orgSchoolLevel"
                          checked={(draft.applicant.schoolLevel ?? "university") === "university"}
                          onChange={() =>
                            setApplicant({ schoolLevel: "university", schoolId: undefined, schoolName: "", organizationName: "" })
                          }
                        />
                        <span>{t("대학교")}</span>
                      </label>
                      <label className="check">
                        <input
                          type="radio"
                          name="orgSchoolLevel"
                          checked={draft.applicant.schoolLevel === "highschool"}
                          onChange={() =>
                            setApplicant({ schoolLevel: "highschool", schoolId: undefined, schoolName: "", organizationName: "" })
                          }
                        />
                        <span>{t("고등학교")}</span>
                      </label>
                    </div>
                  </fieldset>
                  <label className="field" ref={registerField("organizationName")}>
                    <span className="field__label">
                      {t(orgLabel)}<span className="req">*</span>
                    </span>
                    <SearchableSelectField
                      ariaLabel={t("학교 선택")}
                      placeholder={t("학교명을 검색해 주세요")}
                      searchPlaceholder={t("학교명을 입력해 주세요")}
                      value={draft.applicant.schoolId != null ? String(draft.applicant.schoolId) : ""}
                      onChange={selectSchool}
                      onQueryChange={setSchoolQuery}
                      triggerClassName={`field__select${hasError("organizationName") ? " field__select--invalid" : ""}`}
                      options={schoolSelectOptions}
                    />
                  </label>
                  <button type="button" className="field__toggle" onClick={() => setManualSchoolInput(true)}>
                    {t("찾는 학교가 없나요? 직접 입력")}
                  </button>
                </>
              ) : isStudent ? (
                <>
                  <fieldset className="form-block">
                    <legend className="form-block__legend">
                      {t("학교 구분")}<span className="req">*</span>
                    </legend>
                    <div className="radio-row">
                      <label className="check">
                        <input
                          type="radio"
                          name="orgSchoolLevel"
                          checked={(draft.applicant.schoolLevel ?? "university") === "university"}
                          onChange={() => setApplicant({ schoolLevel: "university", schoolId: undefined })}
                        />
                        <span>{t("대학교")}</span>
                      </label>
                      <label className="check">
                        <input
                          type="radio"
                          name="orgSchoolLevel"
                          checked={draft.applicant.schoolLevel === "highschool"}
                          onChange={() => setApplicant({ schoolLevel: "highschool", schoolId: undefined })}
                        />
                        <span>{t("고등학교")}</span>
                      </label>
                    </div>
                  </fieldset>
                  <label className="field" ref={registerField("organizationName")}>
                    <span className="field__label">
                      {t(orgLabel)}<span className="req">*</span>
                    </span>
                    <input
                      className={inputCls("organizationName")}
                      value={draft.applicant.organizationName ?? ""}
                      onChange={(e) => setApplicant({ organizationName: e.target.value, schoolName: e.target.value, schoolId: undefined })}
                      placeholder={t("학교명을 입력해 주세요")}
                    />
                  </label>
                  <button
                    type="button"
                    className="field__toggle"
                    onClick={() => {
                      setApplicant({ schoolId: undefined, schoolName: "", organizationName: "" });
                      setManualSchoolInput(false);
                    }}
                  >
                    {t("목록에서 학교 찾기")}
                  </button>
                </>
              ) : (
                <div className="field-row">
                  <label className="field" ref={registerField("organizationName")}>
                    <span className="field__label">
                      {t(orgLabel)}<span className="req">*</span>
                    </span>
                    <input
                      className={inputCls("organizationName")}
                      value={draft.applicant.organizationName ?? ""}
                      onChange={(e) => setApplicant({ organizationName: e.target.value })}
                      placeholder={t("법인·단체명을 입력해 주세요")}
                    />
                  </label>
                  <label className="field">
                    <span className="field__label">{t("부서")}</span>
                    <input
                      className="field__input"
                      value={draft.applicant.department ?? ""}
                      onChange={(e) => setApplicant({ department: e.target.value })}
                      placeholder={t("부서 (선택)")}
                    />
                  </label>
                </div>
              )}
              {isStudent && (
                <label className="field">
                  <span className="field__label">{t("부서")}</span>
                  <input
                    className="field__input"
                    value={draft.applicant.department ?? ""}
                    onChange={(e) => setApplicant({ department: e.target.value })}
                    placeholder={t("부서 (선택)")}
                  />
                </label>
              )}
              <label className="field" ref={registerField("phone")}>
                <span className="field__label">
                  {t("연락처")}<span className="req">*</span>
                </span>
                <input
                  className={inputCls("phone")}
                  inputMode="tel"
                  value={draft.applicant.phone}
                  onChange={(e) => setApplicant({ phone: e.target.value })}
                  placeholder="010-1234-5678"
                />
                {showPhoneFormat(draft.applicant.phone) && (
                  <span className="field-error">{t("올바른 연락처 형식으로 입력해 주세요. (예: 010-1234-5678)")}</span>
                )}
              </label>
              <label className="field" ref={registerField("email")}>
                <span className="field__label">
                  {t("이메일")}<span className="req">*</span>
                </span>
                <input
                  className={inputCls("email")}
                  type="email"
                  value={draft.applicant.email}
                  onChange={(e) => setApplicant({ email: e.target.value })}
                  placeholder="hong@example.com"
                />
                {showEmailFormat(draft.applicant.email) && (
                  <span className="field-error">{t("올바른 이메일 형식으로 입력해 주세요.")}</span>
                )}
              </label>
              <p className="info-col__notice">
                {t("입력하신 연락처와 이메일로 발급된 모바일 카드를 조회할 수 있습니다.")}
              </p>
            </>

  );
}
