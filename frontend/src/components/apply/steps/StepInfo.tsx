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
import { useMemo, useRef, useState } from "react";

// Country options are static — build once (ko is the stored value, en aids search).
const countryOptions = countries.map((c) => ({ value: c.ko, label: c.ko, keywords: c.en }));

interface StepInfoProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
  onPrev: () => void;
}

export function StepInfo({ draft, update, onNext, onPrev }: StepInfoProps) {
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

  // 출생지역: 선택된 국적의 "시차 결정용 대표 도시"만 드롭다운에 노출하고, 라벨에 표준시 시차를
  // 함께 표기한다(예: "서울 (+9)"). 도시 데이터가 없는 국적은 자유 입력으로 폴백한다(아래 렌더 참고).
  const nationality = draft.applicant.nationality ?? "";
  const birthCities = useMemo(() => birthCitiesFor(nationality), [nationality]);
  const birthCityOptions = useMemo(
    () =>
      birthCities.map((city) => ({
        value: city.ko,
        label: `${city.ko} (UTC ${formatUtcOffset(city.offset)})`,
        keywords: `${city.en} ${city.ko}`,
      })),
    [birthCities],
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
      <p className="step__eyebrow">{isOrg ? "법인·단체 신청" : "개인 신청"}</p>
      <h2 className="step__heading">정보 입력</h2>

      <div className={`form-grid ${isOrg ? "" : isStudent ? "form-grid--pair" : "form-grid--single"}`}>
        <fieldset className="form-block">
          <legend className="form-block__legend">발급 유형 선택</legend>
          <div className="radio-row">
            <label className="check">
              <input
                type="radio"
                name="issuance"
                checked={draft.issuanceMethod === "mobile"}
                onChange={() => setIssuance("mobile")}
              />
              <span>모바일 발급</span>
            </label>
            <label className="check">
              <input
                type="radio"
                name="issuance"
                checked={isPhysical}
                onChange={() => setIssuance("mobile_and_physical")}
              />
              <span>모바일 + 실물 발급</span>
            </label>
          </div>
        </fieldset>

        {/* 학생증은 카드 방향(가로/세로)을 선택할 수 있고, 선택에 따라 견본 이미지가 바뀐다. */}
        {isStudent && (
          <fieldset className="form-block">
            <legend className="form-block__legend">카드 방향</legend>
            <div className="radio-row">
              <label className="check">
                <input
                  type="radio"
                  name="cardOrientation"
                  checked={(draft.cardOrientation ?? "landscape") === "landscape"}
                  onChange={() => update({ cardOrientation: "landscape" })}
                />
                <span>가로형</span>
              </label>
              <label className="check">
                <input
                  type="radio"
                  name="cardOrientation"
                  checked={(draft.cardOrientation ?? "landscape") === "portrait"}
                  onChange={() => update({ cardOrientation: "portrait" })}
                />
                <span>세로형</span>
              </label>
            </div>
          </fieldset>
        )}

        {/* 단체 수량은 업로드한 엑셀의 유효 인원 수로 서버가 산정한다(사용자 입력 없음). */}
      </div>

      <div className={`info-columns ${isPhysical ? "info-columns--two" : ""}`}>
        {/* Applicant */}
        <section className="info-col">
          <h3 className="info-col__title">신청인 정보</h3>
          {isOrg ? (
            <>
              <label className="field" ref={registerField("name")}>
                <span className="field__label">
                  이름<span className="req">*</span>
                </span>
                <input
                  className={inputCls("name")}
                  value={draft.applicant.name}
                  onChange={(e) => setApplicant({ name: e.target.value })}
                  placeholder="담당자 이름"
                />
              </label>
              <div className="field-row">
                <label className="field" ref={registerField("organizationName")}>
                  <span className="field__label">
                    {orgLabel}<span className="req">*</span>
                  </span>
                  <input
                    className={inputCls("organizationName")}
                    value={draft.applicant.organizationName ?? ""}
                    onChange={(e) => setApplicant({ organizationName: e.target.value })}
                    placeholder={isStudent ? "학교명을 입력해 주세요" : "법인·단체명을 입력해 주세요"}
                  />
                </label>
                <label className="field">
                  <span className="field__label">부서</span>
                  <input
                    className="field__input"
                    value={draft.applicant.department ?? ""}
                    onChange={(e) => setApplicant({ department: e.target.value })}
                    placeholder="부서 (선택)"
                  />
                </label>
              </div>
              <label className="field" ref={registerField("phone")}>
                <span className="field__label">
                  연락처<span className="req">*</span>
                </span>
                <input
                  className={inputCls("phone")}
                  inputMode="tel"
                  value={draft.applicant.phone}
                  onChange={(e) => setApplicant({ phone: e.target.value })}
                  placeholder="010-1234-5678"
                />
                {showPhoneFormat(draft.applicant.phone) && (
                  <span className="field-error">올바른 연락처 형식으로 입력해 주세요. (예: 010-1234-5678)</span>
                )}
              </label>
              <label className="field" ref={registerField("email")}>
                <span className="field__label">
                  이메일<span className="req">*</span>
                </span>
                <input
                  className={inputCls("email")}
                  type="email"
                  value={draft.applicant.email}
                  onChange={(e) => setApplicant({ email: e.target.value })}
                  placeholder="hong@example.com"
                />
                {showEmailFormat(draft.applicant.email) && (
                  <span className="field-error">올바른 이메일 형식으로 입력해 주세요.</span>
                )}
              </label>
              <p className="info-col__notice">
                입력하신 연락처와 이메일로 발급된 모바일 카드를 조회할 수 있습니다.
              </p>
            </>
          ) : (
            <>
              <label className="field" ref={registerField("englishName")}>
                <span className="field__label">
                  영문 이름<span className="req">*</span>
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
                    국적<span className="req">*</span>
                  </span>
                  <SearchableSelectField
                    ariaLabel="국적 선택"
                    placeholder="국적을 선택해 주세요"
                    searchPlaceholder="국가명을 입력해 주세요"
                    value={draft.applicant.nationality ?? ""}
                    onChange={setNationality}
                    triggerClassName={`field__select${hasError("nationality") ? " field__select--invalid" : ""}`}
                    options={countryOptions}
                  />
                </div>
                <div className="field" ref={registerField("birthPlace")}>
                  <span className="field__label">
                    출생지역<span className="req">*</span>
                  </span>
                  {!nationality ? (
                    // 국적이 정해져야 도시 목록이 정해지므로, 선택 전에는 비활성 안내를 보여준다.
                    <input
                      className="field__input"
                      value=""
                      disabled
                      placeholder="국적을 먼저 선택해 주세요"
                    />
                  ) : birthCityOptions.length > 0 ? (
                    <SearchableSelectField
                      ariaLabel="출생지역 선택"
                      placeholder="출생 도시를 선택해 주세요"
                      searchPlaceholder="도시명을 입력해 주세요"
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
                      placeholder="출생 도시를 입력해 주세요"
                    />
                  )}
                </div>
              </div>
              <div className="field-row field-row--inline">
                <label className="field" ref={registerField("birthDate")}>
                  <span className="field__label">
                    생년월일<span className="req">*</span>
                  </span>
                  <input
                    className={inputCls("birthDate")}
                    type="date"
                    value={draft.applicant.birthDate ?? ""}
                    onChange={(e) => setApplicant({ birthDate: e.target.value })}
                  />
                </label>
                <div className="field">
                  <span className="field__label">출생시간</span>
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
                    <span>출생시간을 모릅니다</span>
                  </label>
                </div>
              </div>
              <div className="field" ref={registerField("gender")}>
                <span className="field__label">
                  성별<span className="req">*</span>
                </span>
                <SelectField
                  ariaLabel="성별 선택"
                  placeholder="성별을 선택해 주세요"
                  value={draft.applicant.gender ?? ""}
                  onChange={(value) => setApplicant({ gender: value as "male" | "female" | "" })}
                  triggerClassName={`field__select${hasError("gender") ? " field__select--invalid" : ""}`}
                  options={[
                    { value: "male", label: "남성" },
                    { value: "female", label: "여성" },
                  ]}
                />
              </div>
              {isStudent && (
                <>
                  <fieldset className="form-block">
                    <legend className="form-block__legend">
                      학교 구분<span className="req">*</span>
                    </legend>
                    <div className="radio-row">
                      <label className="check">
                        <input
                          type="radio"
                          name="schoolLevel"
                          checked={(draft.applicant.schoolLevel ?? "university") === "university"}
                          onChange={() => setApplicant({ schoolLevel: "university" })}
                        />
                        <span>대학교</span>
                      </label>
                      <label className="check">
                        <input
                          type="radio"
                          name="schoolLevel"
                          checked={draft.applicant.schoolLevel === "highschool"}
                          onChange={() =>
                            // 고등학교 선택 시 대학교 전용 항목(학번·학과)은 비운다.
                            setApplicant({ schoolLevel: "highschool", studentNumber: "", department: "" })
                          }
                        />
                        <span>고등학교</span>
                      </label>
                    </div>
                  </fieldset>
                  {draft.applicant.schoolLevel === "highschool" ? (
                    <label className="field" ref={registerField("schoolName")}>
                      <span className="field__label">학교명<span className="req">*</span></span>
                      <input
                        className={inputCls("schoolName")}
                        value={draft.applicant.schoolName ?? ""}
                        onChange={(e) => setApplicant({ schoolName: e.target.value })}
                        placeholder="학교명을 입력해 주세요"
                      />
                    </label>
                  ) : (
                    <>
                      <label className="field" ref={registerField("schoolName")}>
                        <span className="field__label">대학교명<span className="req">*</span></span>
                        <input
                          className={inputCls("schoolName")}
                          value={draft.applicant.schoolName ?? ""}
                          onChange={(e) => setApplicant({ schoolName: e.target.value })}
                          placeholder="대학교명을 입력해 주세요"
                        />
                      </label>
                      <div className="field-row">
                        <label className="field" ref={registerField("studentNumber")}>
                          <span className="field__label">학번<span className="req">*</span></span>
                          <input
                            className={inputCls("studentNumber")}
                            value={draft.applicant.studentNumber ?? ""}
                            onChange={(e) => setApplicant({ studentNumber: e.target.value })}
                            placeholder="20260001"
                          />
                        </label>
                        <label className="field" ref={registerField("department")}>
                          <span className="field__label">학과<span className="req">*</span></span>
                          <input
                            className={inputCls("department")}
                            value={draft.applicant.department ?? ""}
                            onChange={(e) => setApplicant({ department: e.target.value })}
                            placeholder="학과를 입력해 주세요"
                          />
                        </label>
                      </div>
                    </>
                  )}
                </>
              )}
              <label className="field" ref={registerField("koreaEntryDate")}>
                <span className="field__label">
                  한국입국일<span className="req">*</span>
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
                  전화번호<span className="req">*</span>
                </span>
                <input
                  className={inputCls("phone")}
                  inputMode="tel"
                  value={draft.applicant.phone}
                  onChange={(e) => setApplicant({ phone: e.target.value })}
                  placeholder="010-1234-5678"
                />
                {showPhoneFormat(draft.applicant.phone) && (
                  <span className="field-error">올바른 전화번호 형식으로 입력해 주세요. (예: 010-1234-5678)</span>
                )}
              </label>
              <label className="field" ref={registerField("email")}>
                <span className="field__label">
                  이메일<span className="req">*</span>
                </span>
                <input
                  className={inputCls("email")}
                  type="email"
                  value={draft.applicant.email}
                  onChange={(e) => setApplicant({ email: e.target.value })}
                  placeholder="hong@example.com"
                />
                {showEmailFormat(draft.applicant.email) && (
                  <span className="field-error">올바른 이메일 형식으로 입력해 주세요.</span>
                )}
              </label>
              <p className="info-col__notice">
                입력하신 연락처와 이메일로 발급된 모바일 카드를 조회할 수 있습니다.
              </p>
            </>
          )}
        </section>

        {/* Recipient — only for physical issuance. */}
        {isPhysical && (
          <section className="info-col">
            <div className="info-col__head">
              <h3 className="info-col__title">수령인 정보</h3>
              <label className="check">
                <input
                  type="checkbox"
                  checked={draft.recipient.sameAsApplicant}
                  onChange={(e) => toggleSame(e.target.checked)}
                />
                <span>신청인과 동일합니다</span>
              </label>
            </div>
            <label className="field" ref={registerField("recipient.name")}>
              <span className="field__label">
                이름<span className="req">*</span>
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
                  <span className="field__label">{orgLabel}</span>
                  <input
                    className="field__input"
                    value={draft.recipient.organizationName ?? ""}
                    onChange={(e) => setRecipient({ organizationName: e.target.value })}
                    placeholder={isStudent ? "학교명 (선택)" : "법인·단체명 (선택)"}
                  />
                </label>
                <label className="field">
                  <span className="field__label">부서</span>
                  <input
                    className="field__input"
                    value={draft.recipient.department ?? ""}
                    onChange={(e) => setRecipient({ department: e.target.value })}
                    placeholder="부서 (선택)"
                  />
                </label>
              </div>
            )}
            <label className="field" ref={registerField("recipient.phone")}>
              <span className="field__label">
                연락처<span className="req">*</span>
              </span>
              <input
                className={inputCls("recipient.phone")}
                inputMode="tel"
                value={draft.recipient.phone}
                onChange={(e) => setRecipient({ phone: e.target.value })}
                placeholder="010-1234-5678"
              />
              {showPhoneFormat(draft.recipient.phone) && (
                <span className="field-error">올바른 연락처 형식으로 입력해 주세요. (예: 010-1234-5678)</span>
              )}
            </label>
            <div className="field">
              <span className="field__label">
                배송지 주소<span className="req">*</span>
              </span>
              <div className="field__with-btn">
                <input
                  ref={registerField("recipient.postalCode")}
                  className={inputCls("recipient.postalCode")}
                  value={draft.recipient.postalCode}
                  onChange={(e) => setRecipient({ postalCode: e.target.value })}
                  placeholder="우편번호"
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
                  우편번호 찾기
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
                placeholder="기본 주소"
              />
              <input
                className="field__input"
                value={draft.recipient.addressDetail}
                onChange={(e) => setRecipient({ addressDetail: e.target.value })}
                placeholder="상세 주소를 입력해 주세요"
              />
            </div>
            <label className="field">
              <span className="field__label">배송 요청사항</span>
              <input
                className="field__input"
                value={draft.recipient.deliveryRequest ?? ""}
                onChange={(e) => setRecipient({ deliveryRequest: e.target.value })}
              />
            </label>
          </section>
        )}
      </div>

      <p className="step__hint">* 필수 입력 항목</p>

      <div className="step__actions">
        <Button variant="soft" onClick={onPrev}>
          <ChevronLeft width={16} height={16} /> 이전
        </Button>
        <Button onClick={handleNext} className={isComplete ? undefined : "btn--pending"}>
          다음 <ChevronRight width={16} height={16} />
        </Button>
      </div>
    </div>
  );
}
