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
export function SchoolFields({ draft, setApplicant, validation }: InfoSectionProps) {
  const { t } = useLanguage();
  const isStudent = draft.cardType === "student";
  const { registerField, inputCls, hasError } = validation;
  const { manualSchoolInput, setManualSchoolInput, selectSchool, setSchoolQuery, schoolSelectOptions } = useSchoolSearch(draft, setApplicant);
  return <>
              {isStudent && !manualSchoolInput && (
                <>
                  <fieldset className="form-block">
                    <legend className="form-block__legend">
                      {t("학교 구분")}<span className="req">*</span>
                    </legend>
                    <div className="radio-row">
                      <label className="check">
                        <input
                          type="radio"
                          name="schoolLevel"
                          checked={(draft.applicant.schoolLevel ?? "university") === "university"}
                          onChange={() =>
                            setApplicant({ schoolLevel: "university", schoolId: undefined, schoolName: "", studentNumber: "", department: "" })
                          }
                        />
                        <span>{t("대학교")}</span>
                      </label>
                      <label className="check">
                        <input
                          type="radio"
                          name="schoolLevel"
                          checked={draft.applicant.schoolLevel === "highschool"}
                          onChange={() =>
                            setApplicant({ schoolLevel: "highschool", schoolId: undefined, schoolName: "", studentNumber: "", department: "" })
                          }
                        />
                        <span>{t("고등학교")}</span>
                      </label>
                    </div>
                  </fieldset>
                  <label className="field" ref={registerField("schoolName")}>
                    <span className="field__label">{t("학교")}<span className="req">*</span></span>
                    <SearchableSelectField
                      ariaLabel={t("학교 선택")}
                      placeholder={t("학교명을 검색해 주세요")}
                      searchPlaceholder={t("학교명을 입력해 주세요")}
                      value={draft.applicant.schoolId != null ? String(draft.applicant.schoolId) : ""}
                      onChange={selectSchool}
                      onQueryChange={setSchoolQuery}
                      triggerClassName={`field__select${hasError("schoolName") ? " field__select--invalid" : ""}`}
                      options={schoolSelectOptions}
                    />
                  </label>
                  <button
                    type="button"
                    className="field__toggle"
                    onClick={() => setManualSchoolInput(true)}
                  >
                    {t("찾는 학교가 없나요? 직접 입력")}
                  </button>
                  {draft.applicant.schoolLevel === "highschool" ? null : (
                    <div className="field-row">
                      <label className="field" ref={registerField("studentNumber")}>
                        <span className="field__label">{t("학번")}<span className="req">*</span></span>
                        <input
                          className={inputCls("studentNumber")}
                          value={draft.applicant.studentNumber ?? ""}
                          onChange={(e) => setApplicant({ studentNumber: e.target.value })}
                          placeholder="20260001"
                        />
                      </label>
                      <label className="field" ref={registerField("department")}>
                        <span className="field__label">{t("학과")}<span className="req">*</span></span>
                        <input
                          className={inputCls("department")}
                          value={draft.applicant.department ?? ""}
                          onChange={(e) => setApplicant({ department: e.target.value })}
                          placeholder={t("학과를 입력해 주세요")}
                        />
                      </label>
                    </div>
                  )}
                </>
              )}
              {isStudent && manualSchoolInput && (
                <>
                  <fieldset className="form-block">
                    <legend className="form-block__legend">
                      {t("학교 구분")}<span className="req">*</span>
                    </legend>
                    <div className="radio-row">
                      <label className="check">
                        <input
                          type="radio"
                          name="schoolLevel"
                          checked={(draft.applicant.schoolLevel ?? "university") === "university"}
                          onChange={() => setApplicant({ schoolLevel: "university", schoolId: undefined })}
                        />
                        <span>{t("대학교")}</span>
                      </label>
                      <label className="check">
                        <input
                          type="radio"
                          name="schoolLevel"
                          checked={draft.applicant.schoolLevel === "highschool"}
                          onChange={() =>
                            // 고등학교 선택 시 대학교 전용 항목(학번·학과)은 비운다.
                            setApplicant({ schoolLevel: "highschool", schoolId: undefined, studentNumber: "", department: "" })
                          }
                        />
                        <span>{t("고등학교")}</span>
                      </label>
                    </div>
                  </fieldset>
                  {draft.applicant.schoolLevel === "highschool" ? (
                    <label className="field" ref={registerField("schoolName")}>
                      <span className="field__label">{t("학교명")}<span className="req">*</span></span>
                      <input
                        className={inputCls("schoolName")}
                        value={draft.applicant.schoolName ?? ""}
                        onChange={(e) => setApplicant({ schoolName: e.target.value, schoolId: undefined })}
                        placeholder={t("학교명을 입력해 주세요")}
                      />
                    </label>
                  ) : (
                    <>
                      <label className="field" ref={registerField("schoolName")}>
                        <span className="field__label">{t("대학교명")}<span className="req">*</span></span>
                        <input
                          className={inputCls("schoolName")}
                          value={draft.applicant.schoolName ?? ""}
                          onChange={(e) => setApplicant({ schoolName: e.target.value, schoolId: undefined })}
                          placeholder={t("대학교명을 입력해 주세요")}
                        />
                      </label>
                      <div className="field-row">
                        <label className="field" ref={registerField("studentNumber")}>
                          <span className="field__label">{t("학번")}<span className="req">*</span></span>
                          <input
                            className={inputCls("studentNumber")}
                            value={draft.applicant.studentNumber ?? ""}
                            onChange={(e) => setApplicant({ studentNumber: e.target.value })}
                            placeholder="20260001"
                          />
                        </label>
                        <label className="field" ref={registerField("department")}>
                          <span className="field__label">{t("학과")}<span className="req">*</span></span>
                          <input
                            className={inputCls("department")}
                            value={draft.applicant.department ?? ""}
                            onChange={(e) => setApplicant({ department: e.target.value })}
                            placeholder={t("학과를 입력해 주세요")}
                          />
                        </label>
                      </div>
                    </>
                  )}
                  <button
                    type="button"
                    className="field__toggle"
                    onClick={() => {
                      setApplicant({ schoolId: undefined, schoolName: "", studentNumber: "", department: "" });
                      setManualSchoolInput(false);
                    }}
                  >
                    {t("목록에서 학교 찾기")}
                  </button>
                </>
              )}
  </>;
}
