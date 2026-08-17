// Application page: orchestrates the multi-step create flow.
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { useApplicationDraft } from "../../features/apply/useApplicationDraft";
import { findCardDesign, honoraryKoreanCards, honoraryCitizenCards, studentCards, visitorCards, cardTypeLabels } from "../../data/cards";
import { loadApplications, saveApplications } from "../../data/adminMock";
import { Stepper } from "../../components/apply/Stepper";
import { CardPreviewPanel } from "../../components/apply/CardPreviewPanel";
import { StepType } from "../../components/apply/steps/StepType";
import { StepInfo } from "../../components/apply/steps/StepInfo";
import { StepFiles } from "../../components/apply/steps/StepFiles";
import { StepReview } from "../../components/apply/steps/StepReview";
import { StepComplete } from "../../components/apply/steps/StepComplete";
import "./ApplyPage.css";
import { useAuth } from "../../features/auth/AuthContext";
import { api } from "../../services/api";
import { showToast } from "../../components/ui/toast";

const cardTypeIds: Record<string, number> = {
  "honorary-korean": Number(import.meta.env.VITE_CARD_TYPE_HONOR_KOREAN_ID ?? 1),
  "honorary-citizen": Number(import.meta.env.VITE_CARD_TYPE_HONOR_CITIZEN_ID ?? 2),
  visitor: Number(import.meta.env.VITE_CARD_TYPE_VISITOR_ID ?? 3),
  student: Number(import.meta.env.VITE_CARD_TYPE_STUDENT_ID ?? 4),
};

export function ApplyPage() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const { pathname, search } = useLocation();
  const routeDesignId = pathname.endsWith("/visitor")
    ? visitorCards[0]?.id
    : pathname.endsWith("/honorary-citizen")
      ? honoraryCitizenCards[0]?.id
      : pathname.endsWith("/student")
        ? studentCards[0]?.id
    : pathname.endsWith("/honorary-korean")
      ? honoraryKoreanCards[0]?.id
      : undefined;
  const designId = params.get("designId") ?? routeDesignId;
  const { draft, update, clear } = useApplicationDraft();

  const [step, setStep] = useState(0);
  const [applicationNumber, setApplicationNumber] = useState("");

  useEffect(() => {
    setStep(0);
    setApplicationNumber("");
  }, [pathname]);

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

  const saveLocalApplication = (num: string) => {
    const applications = loadApplications();
    saveApplications([{ applicationNumber: num, applicantType: draft.applicantType === "organization" ? "법인·단체" : "개인", cardType: cardTypeLabels[design.cardType], applicantName: draft.applicant.name || draft.applicant.englishName || "", applicantEmail: draft.applicant.email, phone: draft.applicant.phone, quantity: draft.quantity, status: "SUBMITTED", submittedAt: new Date().toISOString().slice(0, 10) }, ...applications]);
    setApplicationNumber(num);
    goTo(4);
  };

  const submit = async () => {
    if (user?.source === "api") {
      try {
        const physical = draft.issuanceMethod === "mobile_and_physical";
        const request = draft.applicantType === "organization" ? {
          cardTypeId: cardTypeIds[design.cardType], issueType: physical ? "MOBILE_AND_PHYSICAL" : "MOBILE",
          applicant: { organizationName: draft.applicant.organizationName, department: draft.applicant.department, name: draft.applicant.name, phone: draft.applicant.phone },
          receiver: physical ? { sameAsApplicant: draft.recipient.sameAsApplicant, organizationName: draft.recipient.organizationName, department: draft.recipient.department, name: draft.recipient.name, phone: draft.recipient.phone, zipCode: draft.recipient.postalCode, address: draft.recipient.address, detailAddress: draft.recipient.addressDetail, deliveryRequest: draft.recipient.deliveryRequest } : undefined,
        } : {
          cardTypeId: cardTypeIds[design.cardType], issueType: physical ? "MOBILE_AND_PHYSICAL" : "MOBILE",
          applicant: { name: draft.applicant.name || draft.applicant.englishName, phone: draft.applicant.phone },
          receiver: physical ? { sameAsApplicant: draft.recipient.sameAsApplicant, name: draft.recipient.name, phone: draft.recipient.phone, zipCode: draft.recipient.postalCode, address: draft.recipient.address, detailAddress: draft.recipient.addressDetail, deliveryRequest: draft.recipient.deliveryRequest } : undefined,
          member: { englishName: draft.applicant.englishName || draft.applicant.name, birthDate: draft.applicant.birthDate, nationality: draft.applicant.nationality, birthTime: draft.applicant.birthTime || undefined, birthRegion: draft.applicant.birthPlace, gender: draft.applicant.gender?.toUpperCase(), entryDate: draft.applicant.koreaEntryDate || undefined, studentId: draft.applicant.studentNumber, department: draft.applicant.department },
        };
        const form = new FormData();
        form.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
        if (draft.applicantType === "organization") {
          if (!draft.logoFile?.file || !draft.sealFile?.file || !draft.archiveFile?.file) throw new Error("로고, 직인, 제출 ZIP 파일을 다시 선택해 주세요.");
          form.append("logo", draft.logoFile.file); form.append("seal", draft.sealFile.file); form.append("submitFile", draft.archiveFile.file);
        } else {
          if (!draft.faceFile?.file) throw new Error("본인 사진을 다시 선택해 주세요.");
          form.append("photo", draft.faceFile.file);
          if (draft.logoFile?.file) form.append("schoolLogo", draft.logoFile.file);
          if (draft.sealFile?.file) form.append("schoolSeal", draft.sealFile.file);
        }
        const result = await api.createApplication(form, draft.applicantType === "organization");
        saveLocalApplication(result.applicationNumber);
        return;
      } catch (error) {
        showToast(error instanceof Error ? error.message : "신청 API 호출에 실패했습니다.");
        return;
      }
    }
    const num = `APP-2026-${String(Math.floor(100000 + (Date.now() % 900000))).padStart(6, "0")}`;
    saveLocalApplication(num);
  };

  const title = design ? `${cardTypeLabels[design.cardType]} 신청` : "제작 신청";
  // Overlays only appear once uploaded, previewed on the sample card.
  const logoOverlay = step >= 2 ? draft.logoFile?.previewUrl : undefined;
  const sealOverlay = step >= 2 ? draft.sealFile?.previewUrl : undefined;

  if (!user) {
    return (
      <div className="apply">
        <header className="apply__page-head subpage-hero page-container">
          <p className="eyebrow">제작 신청</p>
          <h1 className="apply__title subpage-hero__title">{title}</h1>
          <p className="section-lead">제작 신청은 로그인 후 이용할 수 있습니다.</p>
        </header>
        <section className="apply-login-required page-container">
          <h2>로그인이 필요합니다</h2>
          <p>신청자 정보와 제작 진행 내역을 안전하게 관리하기 위해 로그인한 회원만 제작 신청을 진행할 수 있습니다.</p>
          <Link to={`/login?returnTo=${encodeURIComponent(pathname + search)}`}>로그인하기</Link>
        </section>
      </div>
    );
  }

  return (
    <div className="apply">
      <header className="apply__page-head subpage-hero page-container">
        <p className="eyebrow">제작 신청</p>
        <h1 className="apply__title subpage-hero__title">{title}</h1>
      </header>
      <div className="apply__inner page-container">
        <div className="apply__main">
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

        <CardPreviewPanel design={design} orientation={draft.cardOrientation} logoOverlay={logoOverlay} sealOverlay={sealOverlay} />
      </div>
    </div>
  );
}
