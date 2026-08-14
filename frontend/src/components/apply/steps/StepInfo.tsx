// Apply step: applicant & recipient information form.
import type { ApplicationDraft, IssuanceMethod, ApplicantInfo, RecipientInfo } from "../../../features/apply/types";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";
import { showToast } from "../../ui/toast";
import { openPostcodeSearch } from "../../../lib/postcode";
import { useRef } from "react";

interface StepInfoProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
  onPrev: () => void;
}

export function StepInfo({ draft, update, onNext, onPrev }: StepInfoProps) {
  const addressDetailRef = useRef<HTMLInputElement>(null);
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";
  // 학생증은 "법인·단체명" 대신 "학교명"을 사용한다.
  const orgLabel = isStudent ? "학교명" : "법인·단체명";

  const setApplicant = (patch: Partial<ApplicantInfo>) =>
    update({ applicant: { ...draft.applicant, ...patch } });
  const setRecipient = (patch: Partial<RecipientInfo>) =>
    update({ recipient: { ...draft.recipient, ...patch } });

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

      <div className={`form-grid ${isOrg ? "" : "form-grid--single"}`}>
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

        {/* 수량 선택은 법인·단체 신청에서만 노출한다. 개인 신청은 1매 고정. */}
        {isOrg && (
          <label className="field field--quantity">
            <span className="field__label">신청 수량</span>
            <div className="field__with-suffix">
              <input
                className="field__input"
                type="number"
                min={1}
                value={draft.quantity}
                onChange={(e) => update({ quantity: Math.max(1, Number(e.target.value) || 1) })}
              />
              <span className="field__suffix">매</span>
            </div>
          </label>
        )}
      </div>

      <div className={`info-columns ${isPhysical ? "info-columns--two" : ""}`}>
        {/* Applicant */}
        <section className="info-col">
          <h3 className="info-col__title">신청인 정보</h3>
          {isOrg ? (
            <>
              <label className="field">
                <span className="field__label">
                  이름<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  value={draft.applicant.name}
                  onChange={(e) => setApplicant({ name: e.target.value })}
                  placeholder="담당자 이름"
                />
              </label>
              <div className="field-row">
                <label className="field">
                  <span className="field__label">
                    {orgLabel}<span className="req">*</span>
                  </span>
                  <input
                    className="field__input"
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
              <label className="field">
                <span className="field__label">
                  연락처<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  inputMode="tel"
                  value={draft.applicant.phone}
                  onChange={(e) => setApplicant({ phone: e.target.value })}
                  placeholder="010-1234-5678"
                />
              </label>
              <label className="field">
                <span className="field__label">
                  이메일<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  type="email"
                  value={draft.applicant.email}
                  onChange={(e) => setApplicant({ email: e.target.value })}
                  placeholder="hong@example.com"
                />
              </label>
              <p className="info-col__notice">
                입력하신 연락처와 이메일로 발급된 모바일 카드를 조회할 수 있습니다.
              </p>
            </>
          ) : (
            <>
              <label className="field">
                <span className="field__label">
                  영문 이름<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  value={draft.applicant.englishName ?? ""}
                  onChange={(e) => setApplicant({ englishName: e.target.value })}
                  placeholder="HONG GIL DONG"
                />
              </label>
              <div className="field-row">
                <label className="field">
                  <span className="field__label">
                    국적<span className="req">*</span>
                  </span>
                  <input
                    className="field__input"
                    value={draft.applicant.nationality ?? ""}
                    onChange={(e) => setApplicant({ nationality: e.target.value })}
                    placeholder="대한민국"
                  />
                </label>
                <label className="field">
                  <span className="field__label">
                    출생지역<span className="req">*</span>
                  </span>
                  <input
                    className="field__input"
                    value={draft.applicant.birthPlace ?? ""}
                    onChange={(e) => setApplicant({ birthPlace: e.target.value })}
                    placeholder="서울"
                  />
                </label>
              </div>
              <div className="field-row field-row--inline">
                <label className="field">
                  <span className="field__label">
                    생년월일<span className="req">*</span>
                  </span>
                  <input
                    className="field__input"
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
              <label className="field">
                <span className="field__label">
                  성별<span className="req">*</span>
                </span>
                <select
                  className="field__select"
                  value={draft.applicant.gender ?? ""}
                  onChange={(e) =>
                    setApplicant({ gender: e.target.value as "male" | "female" | "" })
                  }
                >
                  <option value="">성별을 선택해 주세요</option>
                  <option value="male">남성</option>
                  <option value="female">여성</option>
                </select>
              </label>
              {isStudent && (
                <div className="field-row">
                  <label className="field">
                    <span className="field__label">학번<span className="req">*</span></span>
                    <input
                      className="field__input"
                      value={draft.applicant.studentNumber ?? ""}
                      onChange={(e) => setApplicant({ studentNumber: e.target.value })}
                      placeholder="20260001"
                    />
                  </label>
                  <label className="field">
                    <span className="field__label">학과<span className="req">*</span></span>
                    <input
                      className="field__input"
                      value={draft.applicant.department ?? ""}
                      onChange={(e) => setApplicant({ department: e.target.value })}
                      placeholder="학과를 입력해 주세요"
                    />
                  </label>
                </div>
              )}
              <label className="field">
                <span className="field__label">
                  한국입국일<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  type="date"
                  value={draft.applicant.koreaEntryDate ?? ""}
                  onChange={(e) => setApplicant({ koreaEntryDate: e.target.value })}
                />
              </label>
              <label className="field">
                <span className="field__label">
                  전화번호<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  inputMode="tel"
                  value={draft.applicant.phone}
                  onChange={(e) => setApplicant({ phone: e.target.value })}
                  placeholder="010-1234-5678"
                />
              </label>
              <label className="field">
                <span className="field__label">
                  이메일<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  type="email"
                  value={draft.applicant.email}
                  onChange={(e) => setApplicant({ email: e.target.value })}
                  placeholder="hong@example.com"
                />
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
            <label className="field">
              <span className="field__label">
                이름<span className="req">*</span>
              </span>
              <input
                className="field__input"
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
            <label className="field">
              <span className="field__label">
                연락처<span className="req">*</span>
              </span>
              <input
                className="field__input"
                inputMode="tel"
                value={draft.recipient.phone}
                onChange={(e) => setRecipient({ phone: e.target.value })}
              />
            </label>
            <div className="field">
              <span className="field__label">
                배송지 주소<span className="req">*</span>
              </span>
              <div className="field__with-btn">
                <input
                  className="field__input"
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
                ref={addressDetailRef}
                className="field__input"
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
        <Button onClick={onNext}>
          다음 <ChevronRight width={16} height={16} />
        </Button>
      </div>
    </div>
  );
}
