// Apply step: applicant & recipient information form.
import type { ApplicationDraft, IssuanceMethod, ApplicantInfo, RecipientInfo } from "../../../features/apply/types";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";
import { showToast } from "../../ui/toast";
import { openPostcodeSearch } from "../../../lib/postcode";
import { SelectField } from "../../ui/SelectField";
import { SearchableSelectField } from "../../ui/SearchableSelectField";
import { countries } from "../../../data/countries";
import { birthCitiesFor, formatUtcOffset } from "../../../data/birthCities";
import { api, type SchoolOption } from "../../../services/api";
import { useEffect, useMemo, useRef, useState } from "react";
import { useLanguage } from "../../../features/i18n/LanguageContext";

interface StepInfoProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
  onPrev: () => void;
}

export function StepInfo({ draft, update, onNext, onPrev }: StepInfoProps) {
  const { t, language } = useLanguage();
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
  const addressDetailRef = useRef<HTMLInputElement | null>(null);
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";
  // 학생증은 "법인·단체명" 대신 "학교명"을 사용한다.
  const orgLabel = isStudent ? "학교명" : "법인·단체명";

  const setApplicant = (patch: Partial<ApplicantInfo>) =>
    update({ applicant: { ...draft.applicant, ...patch } });
  const setRecipient = (patch: Partial<RecipientInfo>) =>
    update({ recipient: { ...draft.recipient, ...patch } });

  // 학교 검색select — 대학교+고등학교를 합치면 학교 수가 약 2,800개라 더 이상 전체 목록을 한 번에
  // 받지 않는다(백엔드 SchoolService 참고). 대신 검색어를 200~300ms debounce해 서버에 넘기고, 결과를
  // 그대로 옵션으로 쓴다(서버가 이미 최대 20건으로 제한 + 관련도순 정렬해 반환). 직접입력으로 채워진
  // 기존 draft(schoolName은 있고 schoolId는 없음)를 복원한 경우 직접입력 모드로 시작한다.
  //
  // 이미 학교가 선택된 draft를 복원할 때(schoolId 있음)는 그 학교를 initial 옵션으로 미리 채워둔다 —
  // 그래야 사용자가 아무것도 타이핑하지 않아도 선택select 트리거에 기존 학교명이 그대로 보인다(별도
  // API 호출 없이 draft가 이미 들고 있는 값으로 구성).
  const [schoolOptions, setSchoolOptions] = useState<SchoolOption[]>(() =>
    draft.applicant.schoolId != null && draft.applicant.schoolName
      ? [
          {
            id: draft.applicant.schoolId,
            name: draft.applicant.schoolName,
            schoolType: draft.applicant.schoolLevel === "highschool" ? "HIGH_SCHOOL" : "UNIVERSITY",
          },
        ]
      : [],
  );
  const [schoolQuery, setSchoolQuery] = useState("");
  const [manualSchoolInput, setManualSchoolInput] = useState(
    () => !!draft.applicant.schoolName && draft.applicant.schoolId == null,
  );

  useEffect(() => {
    if (!isStudent) return;
    const query = schoolQuery.trim();
    // 검색어가 비었으면(패널을 처음 열었을 때 등) 서버를 부르지 않는다 — 기존 옵션(복원된 선택 포함)을
    // 그대로 둔다. 서버도 빈 검색어에는 빈 목록을 돌려주므로 어차피 호출할 이유가 없다.
    if (!query) return;
    let cancelled = false;
    const timer = setTimeout(() => {
      api.searchSchools(query).then((options) => {
        if (!cancelled) setSchoolOptions(options);
      }).catch(() => {
        // 검색 실패해도 직접입력으로 계속 진행할 수 있으므로 별도 에러 처리는 하지 않는다.
      });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [isStudent, schoolQuery]);

  const schoolSelectOptions = useMemo(
    () =>
      schoolOptions.map((school) => ({
        value: String(school.id),
        label: school.name,
        keywords: school.schoolType === "UNIVERSITY" ? "대학교 University" : "고등학교 High School",
      })),
    [schoolOptions],
  );

  const selectSchool = (value: string) => {
    const school = schoolOptions.find((option) => String(option.id) === value);
    if (!school) return;
    const isUniversity = school.schoolType === "UNIVERSITY";
    setApplicant({
      schoolId: school.id,
      schoolName: school.name,
      // 단체 신청 화면의 "학교명"(orgLabel)은 organizationName을 그대로 쓰는 기존 필드라, 검색select로
      // 골라도 같은 값을 여기에도 채운다(개인 신청에서는 이 필드를 아무도 읽지 않아 영향 없음).
      organizationName: school.name,
      schoolLevel: isUniversity ? "university" : "highschool",
      ...(isUniversity ? {} : { studentNumber: "", department: "" }),
    });
  };

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

  const setIssuance = (method: IssuanceMethod) => {
    // When switching back to mobile-only, drop recipient data entirely.
    if (method === "mobile") {
      update({
        issuanceMethod: method,
        recipient: { ...draft.recipient, sameAsApplicant: false },
      });
    } else {
      update({ issuanceMethod: method });
    }
  };

  // Required-field validation. Errors only show after the first "다음" attempt;
  // once shown, they clear live as fields get filled (missingKeys recomputes each
  // render). Keys mirror the fields actually rendered for each applicant type.
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
    <div className="step">
      <p className="step__eyebrow">{isOrg ? t("법인·단체 신청") : t("개인 신청")}</p>
      <h2 className="step__heading">{t("정보 입력")}</h2>

      <div className={`form-grid ${isOrg ? "" : isStudent ? "form-grid--pair" : "form-grid--single"}`}>
        <fieldset className="form-block">
          <legend className="form-block__legend">{t("발급 유형 선택")}</legend>
          <div className="radio-row">
            <label className="check">
              <input
                type="radio"
                name="issuance"
                checked={draft.issuanceMethod === "mobile"}
                onChange={() => setIssuance("mobile")}
              />
              <span>{t("모바일 발급")}</span>
            </label>
            <label className="check">
              <input
                type="radio"
                name="issuance"
                checked={isPhysical}
                onChange={() => setIssuance("mobile_and_physical")}
              />
              <span>{t("모바일 + 실물 발급")}</span>
            </label>
          </div>
        </fieldset>

        {/* 학생증은 카드 방향(가로/세로)을 선택할 수 있고, 선택에 따라 견본 이미지가 바뀐다. */}
        {isStudent && (
          <fieldset className="form-block">
            <legend className="form-block__legend">{t("카드 방향")}</legend>
            <div className="radio-row">
              <label className="check">
                <input
                  type="radio"
                  name="cardOrientation"
                  checked={(draft.cardOrientation ?? "landscape") === "landscape"}
                  onChange={() => update({ cardOrientation: "landscape" })}
                />
                <span>{t("가로형")}</span>
              </label>
              <label className="check">
                <input
                  type="radio"
                  name="cardOrientation"
                  checked={(draft.cardOrientation ?? "landscape") === "portrait"}
                  onChange={() => update({ cardOrientation: "portrait" })}
                />
                <span>{t("세로형")}</span>
              </label>
            </div>
          </fieldset>
        )}

        {/* 단체 수량은 업로드한 엑셀의 유효 인원 수로 서버가 산정한다(사용자 입력 없음). */}
      </div>

      <div className={`info-columns ${isPhysical ? "info-columns--two" : ""}`}>
        {/* Applicant */}
        <section className="info-col">
          <h3 className="info-col__title">{t("신청인 정보")}</h3>
          {isOrg ? (
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
          ) : (
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
              {isStudent && !manualSchoolInput && (
                <>
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
          )}
        </section>

        {/* Recipient — only for physical issuance. */}
        {isPhysical && (
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
        )}
      </div>

      <p className="step__hint">{t("* 필수 입력 항목")}</p>

      <div className="step__actions">
        <Button variant="soft" onClick={onPrev}>
          <ChevronLeft width={16} height={16} /> {t("이전")}
        </Button>
        <Button onClick={handleNext} className={isComplete ? undefined : "btn--pending"}>
          {t("다음")} <ChevronRight width={16} height={16} />
        </Button>
      </div>
    </div>
  );
}
