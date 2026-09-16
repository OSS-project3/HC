import type { ApplicantInfo, ApplicationDraft, IssuanceMethod, RecipientInfo } from "../../../features/apply/types";
import { useInfoValidation } from "../../../features/apply/useInfoValidation";
import { useLanguage } from "../../../features/i18n/LanguageContext";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";
import { IndividualFields } from "../info/IndividualFields";
import { OrganizationFields } from "../info/OrganizationFields";
import { RecipientSection } from "../info/RecipientSection";
interface StepInfoProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
  onPrev: () => void;
}
export function StepInfo({ draft, update, onNext, onPrev }: StepInfoProps) {
  const { t } = useLanguage();
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";
  const setApplicant = (patch: Partial<ApplicantInfo>) => update({ applicant: { ...draft.applicant, ...patch } });
  const setRecipient = (patch: Partial<RecipientInfo>) => update({ recipient: { ...draft.recipient, ...patch } });
  const validation = useInfoValidation(draft, onNext);
  const { isComplete, handleNext } = validation;
  const setIssuance = (method: IssuanceMethod) => {
    // Mobile-only submissions omit recipient data; keep draft inputs for switching back.
    if (method === "mobile") {
      update({
        issuanceMethod: method,
        recipient: { ...draft.recipient, sameAsApplicant: false },
      });
    } else {
      update({ issuanceMethod: method });
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
          {isOrg
            ? <OrganizationFields draft={draft} setApplicant={setApplicant} validation={validation} />
            : <IndividualFields draft={draft} setApplicant={setApplicant} validation={validation} />}

        </section>

        {/* Recipient — only for physical issuance. */}
        {isPhysical && (
          <RecipientSection draft={draft} setRecipient={setRecipient} validation={validation} />
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
