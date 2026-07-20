import type { ApplicationDraft, IssuanceMethod, ApplicantInfo, RecipientInfo } from "../../../features/apply/types";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";

interface StepInfoProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
  onPrev: () => void;
}

export function StepInfo({ draft, update, onNext, onPrev }: StepInfoProps) {
  const isOrg = draft.applicantType === "organization";
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";

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
        name: draft.applicant.name,
        phone: draft.applicant.phone,
        organizationName: draft.applicant.organizationName,
        department: draft.applicant.department,
      });
    } else {
      setRecipient({ sameAsApplicant: false });
    }
  };

  return (
    <div className="step">
      <h2 className="step__heading">정보 입력</h2>

      <div className="form-grid">
        <fieldset className="form-block">
          <legend className="form-block__legend">발급 유형 선택</legend>
          <div className="radio-row">
            <label className="radio">
              <input
                type="radio"
                name="issuance"
                checked={draft.issuanceMethod === "mobile"}
                onChange={() => setIssuance("mobile")}
              />
              모바일 발급
            </label>
            <label className="radio">
              <input
                type="radio"
                name="issuance"
                checked={isPhysical}
                onChange={() => setIssuance("mobile_and_physical")}
              />
              모바일 + 실물 발급
            </label>
          </div>
        </fieldset>

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
      </div>

      <div className={`info-columns ${isPhysical ? "info-columns--two" : ""}`}>
        {/* Applicant */}
        <section className="info-col">
          <h3 className="info-col__title">신청인 정보</h3>
          <div className="field">
            <span className="field__label">
              이름<span className="req">*</span>
            </span>
            <input
              className="field__input"
              value={draft.applicant.name}
              onChange={(e) => setApplicant({ name: e.target.value })}
              placeholder="홍 길 동"
            />
          </div>
          {isOrg && (
            <div className="field-row">
              <label className="field">
                <span className="field__label">
                  법인 단체명<span className="req">*</span>
                </span>
                <input
                  className="field__input"
                  value={draft.applicant.organizationName ?? ""}
                  onChange={(e) => setApplicant({ organizationName: e.target.value })}
                />
              </label>
              <label className="field">
                <span className="field__label">부서명</span>
                <input
                  className="field__input"
                  value={draft.applicant.department ?? ""}
                  onChange={(e) => setApplicant({ department: e.target.value })}
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
            <div className="field-row">
              <label className="field">
                {/* Optional per the client — never required. */}
                <span className="field__label">법인 단체명</span>
                <input
                  className="field__input"
                  value={draft.recipient.organizationName ?? ""}
                  onChange={(e) => setRecipient({ organizationName: e.target.value })}
                />
              </label>
              <label className="field">
                <span className="field__label">부서명</span>
                <input
                  className="field__input"
                  value={draft.recipient.department ?? ""}
                  onChange={(e) => setRecipient({ department: e.target.value })}
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
                <button type="button" className="postal-btn">
                  우편번호 찾기
                </button>
              </div>
              <input
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
