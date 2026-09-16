import { useMemo } from "react";
import { birthCitiesFor, formatUtcOffset } from "../../../data/birthCities";
import { countries } from "../../../data/countries";
import type { ApplicantInfo, ApplicationDraft } from "../../../features/apply/types";
import { useInfoValidation } from "../../../features/apply/useInfoValidation";
import { useLanguage } from "../../../features/i18n/LanguageContext";
import { SearchableSelectField } from "../../ui/SearchableSelectField";
import { SelectField } from "../../ui/SelectField";
import { SchoolFields } from "./SchoolFields";
interface InfoSectionProps {
  draft: ApplicationDraft;
  setApplicant: (patch: Partial<ApplicantInfo>) => void;
  validation: ReturnType<typeof useInfoValidation>;
}
export function IndividualFields({ draft, setApplicant, validation }: InfoSectionProps) {
  const { t, language } = useLanguage();
  const isStudent = draft.cardType === "student";
  const { registerField, inputCls, hasError, showPhoneFormat, showEmailFormat } = validation;

  // Country options: ko stays the stored value; the label follows the UI language
  // (countries.ts carries both names) and both names remain searchable.
  const countryOptions = useMemo(
    () =>
      countries.map((c) => ({
        value: c.ko,
        label: language === "en" ? c.en : c.ko,
        keywords: `${c.en} ${c.ko}`,
      })),
    [language],
  );
  // 출생지역: 선택된 국적의 "시차 결정용 대표 도시"만 드롭다운에 노출하고, 라벨에 표준시 시차를
  // 함께 표기한다(예: "서울 (+9)"). 도시 데이터가 없는 국적은 자유 입력으로 폴백한다(아래 렌더 참고).
  const nationality = draft.applicant.nationality ?? "";
  const birthCities = useMemo(() => birthCitiesFor(nationality), [nationality]);
  const birthCityOptions = useMemo(
    () =>
      birthCities.map((city) => ({
        value: city.ko,
        label: `${language === "en" ? city.en : city.ko} (UTC ${formatUtcOffset(city.offset)})`,
        keywords: `${city.en} ${city.ko}`,
      })),
    [birthCities, language],
  );

  // 국적을 바꾸면 이전 출생도시가 새 국적의 목록에 없을 때 초기화한다(엉뚱한 도시가 남지 않도록).
  const setNationality = (value: string) => {
    const stillValid = birthCitiesFor(value).some((city) => city.ko === draft.applicant.birthPlace);
    setApplicant({ nationality: value, birthPlace: stillValid ? draft.applicant.birthPlace : "" });
  };


  return (
            <>
              <label className="field" ref={registerField("englishName")}>
                <span className="field__label">
                  {t("영문 이름")}<span className="req">*</span>
                </span>
                <input
                  className={inputCls("englishName")}
                  value={draft.applicant.englishName ?? ""}
                  onChange={(e) => setApplicant({ englishName: e.target.value })}
                  placeholder="HONG GIL DONG"
                />
              </label>
              <div className="field-row">
                <div className="field" ref={registerField("nationality")}>
                  <span className="field__label">
                    {t("국적")}<span className="req">*</span>
                  </span>
                  <SearchableSelectField
                    ariaLabel={t("국적 선택")}
                    placeholder={t("국적을 선택해 주세요")}
                    searchPlaceholder={t("국가명을 입력해 주세요")}
                    value={draft.applicant.nationality ?? ""}
                    onChange={setNationality}
                    triggerClassName={`field__select${hasError("nationality") ? " field__select--invalid" : ""}`}
                    options={countryOptions}
                  />
                </div>
                <div className="field" ref={registerField("birthPlace")}>
                  <span className="field__label">
                    {t("출생지역")}<span className="req">*</span>
                  </span>
                  {!nationality ? (
                    // 국적이 정해져야 도시 목록이 정해지므로, 선택 전에는 비활성 안내를 보여준다.
                    <input
                      className="field__input"
                      value=""
                      disabled
                      placeholder={t("국적을 먼저 선택해 주세요")}
                    />
                  ) : birthCityOptions.length > 0 ? (
                    <SearchableSelectField
                      ariaLabel={t("출생지역 선택")}
                      placeholder={t("출생 도시를 선택해 주세요")}
                      searchPlaceholder={t("도시명을 입력해 주세요")}
                      value={draft.applicant.birthPlace ?? ""}
                      onChange={(value) => setApplicant({ birthPlace: value })}
                      triggerClassName={`field__select${hasError("birthPlace") ? " field__select--invalid" : ""}`}
                      options={birthCityOptions}
                    />
                  ) : (
                    // 대표 도시 데이터가 없는 국적은 자유 입력으로 폴백한다.
                    <input
                      className={inputCls("birthPlace")}
                      value={draft.applicant.birthPlace ?? ""}
                      onChange={(e) => setApplicant({ birthPlace: e.target.value })}
                      placeholder={t("출생 도시를 입력해 주세요")}
                    />
                  )}
                </div>
              </div>
              <div className="field-row field-row--inline">
                <label className="field" ref={registerField("birthDate")}>
                  <span className="field__label">
                    {t("생년월일")}<span className="req">*</span>
                  </span>
                  <input
                    className={inputCls("birthDate")}
                    type="date"
                    value={draft.applicant.birthDate ?? ""}
                    onChange={(e) => setApplicant({ birthDate: e.target.value })}
                  />
                </label>
                <div className="field">
                  <span className="field__label">{t("출생시간")}</span>
                  <input
                    className="field__input"
                    type="time"
                    value={draft.applicant.birthTime ?? ""}
                    disabled={draft.applicant.birthTimeUnknown}
                    onChange={(e) => setApplicant({ birthTime: e.target.value })}
                  />
                  <label className="check birth-time-check">
                    <input
                      type="checkbox"
                      checked={draft.applicant.birthTimeUnknown ?? false}
                      onChange={(e) =>
                        setApplicant({
                          birthTimeUnknown: e.target.checked,
                          birthTime: e.target.checked ? "" : draft.applicant.birthTime,
                        })
                      }
                    />
                    <span>{t("출생시간을 모릅니다")}</span>
                  </label>
                </div>
              </div>
              <div className="field" ref={registerField("gender")}>
                <span className="field__label">
                  {t("성별")}<span className="req">*</span>
                </span>
                <SelectField
                  ariaLabel={t("성별 선택")}
                  placeholder={t("성별을 선택해 주세요")}
                  value={draft.applicant.gender ?? ""}
                  onChange={(value) => setApplicant({ gender: value as "male" | "female" | "" })}
                  triggerClassName={`field__select${hasError("gender") ? " field__select--invalid" : ""}`}
                  options={[
                    { value: "male", label: "남성" },
                    { value: "female", label: "여성" },
                  ]}
                />
              </div>
              <SchoolFields draft={draft} setApplicant={setApplicant} validation={validation} />
              <label className="field" ref={registerField("koreaEntryDate")}>
                <span className="field__label">
                  {t("한국입국일")}<span className="req">*</span>
                </span>
                <input
                  className={inputCls("koreaEntryDate")}
                  type="date"
                  value={draft.applicant.koreaEntryDate ?? ""}
                  onChange={(e) => setApplicant({ koreaEntryDate: e.target.value })}
                />
              </label>
              {!isStudent && (
                <label className="field" ref={registerField("address")}>
                  <span className="field__label">
                    {t("주소")}<span className="req">*</span>
                  </span>
                  <input
                    className={inputCls("address")}
                    value={draft.applicant.address ?? ""}
                    onChange={(e) => setApplicant({ address: e.target.value })}
                    placeholder={t("카드에 표시될 주소를 입력해 주세요")}
                  />
                </label>
              )}
              <label className="field" ref={registerField("phone")}>
                <span className="field__label">
                  {t("전화번호")}<span className="req">*</span>
                </span>
                <input
                  className={inputCls("phone")}
                  inputMode="tel"
                  value={draft.applicant.phone}
                  onChange={(e) => setApplicant({ phone: e.target.value })}
                  placeholder="010-1234-5678"
                />
                {showPhoneFormat(draft.applicant.phone) && (
                  <span className="field-error">{t("올바른 전화번호 형식으로 입력해 주세요. (예: 010-1234-5678)")}</span>
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
