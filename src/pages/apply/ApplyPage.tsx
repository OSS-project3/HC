import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useApplicationDraft } from "../../features/apply/useApplicationDraft";
import { findCardDesign, honoraryKoreanCards, cardTypeLabels } from "../../data/cards";
import { Stepper } from "../../components/apply/Stepper";
import { CardPreviewPanel } from "../../components/apply/CardPreviewPanel";
import { StepType } from "../../components/apply/steps/StepType";
import { StepInfo } from "../../components/apply/steps/StepInfo";
import { StepFiles } from "../../components/apply/steps/StepFiles";
import { StepReview } from "../../components/apply/steps/StepReview";
import { StepComplete } from "../../components/apply/steps/StepComplete";
import "./apply.css";

export function ApplyPage() {
  const [params] = useSearchParams();
  const designId = params.get("designId");
  const { draft, update, clear } = useApplicationDraft();

  const [step, setStep] = useState(0);
  const [applicationNumber, setApplicationNumber] = useState("");

  // The selected design is carried through the URL so it survives refresh.
  const design = useMemo(() => findCardDesign(designId) ?? honoraryKoreanCards[0], [designId]);

  // Sync the design/cardType into the draft when the URL changes.
  useEffect(() => {
    if (design && (draft.designId !== design.id || draft.cardType !== design.cardType)) {
      update({ designId: design.id, cardType: design.cardType });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [design.id]);

  const goTo = (next: number) => {
    setStep(Math.max(0, Math.min(4, next)));
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const submit = () => {
    // In production this POSTs to /api/applications/{draftId}/submit and uses
    // the server-issued number. Here we generate a placeholder.
    const num = `APP-2026-${String(Math.floor(100000 + (Date.now() % 900000))).padStart(6, "0")}`;
    setApplicationNumber(num);
    goTo(4);
  };

  const title = design ? `${cardTypeLabels[design.cardType]} 신청` : "제작 신청";
  // Overlays only appear once uploaded, previewed on the sample card.
  const logoOverlay = step >= 2 ? draft.logoFile?.previewUrl : undefined;
  const sealOverlay = step >= 2 ? draft.sealFile?.previewUrl : undefined;

  return (
    <div className="apply">
      <div className="apply__inner page-container">
        <div className="apply__main">
          <h1 className="apply__title">{title}</h1>
          <Stepper current={step} />

          <div className="apply__step">
            {step === 0 && <StepType draft={draft} update={update} onNext={() => goTo(1)} />}
            {step === 1 && (
              <StepInfo draft={draft} update={update} onNext={() => goTo(2)} onPrev={() => goTo(0)} />
            )}
            {step === 2 && (
              <StepFiles draft={draft} update={update} onNext={() => goTo(3)} onPrev={() => goTo(1)} />
            )}
            {step === 3 && (
              <StepReview
                draft={draft}
                design={design}
                onSubmit={submit}
                onPrev={() => goTo(2)}
                onEdit={goTo}
              />
            )}
            {step === 4 && (
              <StepComplete
                draft={draft}
                applicationNumber={applicationNumber}
                onDone={() => {
                  // Clear temporary personal data once finished.
                  clear();
                }}
              />
            )}
          </div>
        </div>

        <CardPreviewPanel design={design} logoOverlay={logoOverlay} sealOverlay={sealOverlay} />
      </div>
    </div>
  );
}
