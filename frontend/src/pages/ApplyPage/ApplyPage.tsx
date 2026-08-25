// Application page: orchestrates the multi-step create flow.
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { useApplicationDraft } from "../../features/apply/useApplicationDraft";
import { findCardDesign, honoraryKoreanCards, honoraryCitizenCards, studentCards, visitorCards, cardTypeLabels, cardTypeIds } from "../../data/cards";
import { toGender, toIssueType, toOrientation, toSchoolType } from "../../features/apply/mappers";
import { loadApplications, saveApplications, type AdminApplicationDetail } from "../../data/adminMock";
import { Stepper } from "../../components/apply/Stepper";
import { CardPreviewPanel } from "../../components/apply/CardPreviewPanel";
import { StepType } from "../../components/apply/steps/StepType";
import { StepInfo } from "../../components/apply/steps/StepInfo";
import { StepFiles } from "../../components/apply/steps/StepFiles";
import { StepReview } from "../../components/apply/steps/StepReview";
import { StepComplete } from "../../components/apply/steps/StepComplete";
import "./ApplyPage.css";
import { useAuth } from "../../features/auth/AuthContext";
import { api, ApiError } from "../../services/api";
import { showToast } from "../../components/ui/toast";

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

  const saveLocalApplication = (num: string, serverQuantity?: number) => {
    const applications = loadApplications();
    const isOrg = draft.applicantType === "organization";
    const isPhysical = draft.issuanceMethod === "mobile_and_physical";
    const genderLabel = draft.applicant.gender === "male" ? "남성" : draft.applicant.gender === "female" ? "여성" : undefined;
    const schoolLevelLabel = draft.applicant.schoolLevel === "highschool" ? "고등학교" : draft.applicant.schoolLevel === "university" ? "대학교" : undefined;
    const detail: AdminApplicationDetail = {
      issuanceMethod: isPhysical ? "모바일 + 실물 발급" : "모바일 발급",
      email: draft.applicant.email || undefined,
      englishName: draft.applicant.englishName || undefined,
      nationality: draft.applicant.nationality || undefined,
      birthPlace: draft.applicant.birthPlace || undefined,
      birthDate: draft.applicant.birthDate || undefined,
      birthTime: draft.applicant.birthTime || undefined,
      gender: genderLabel,
      koreaEntryDate: draft.applicant.koreaEntryDate || undefined,
      schoolLevel: design.cardType === "student" ? schoolLevelLabel : undefined,
      schoolName: draft.applicant.schoolName || undefined,
      studentNumber: draft.applicant.studentNumber || undefined,
      department: draft.applicant.department || undefined,
      organizationName: isOrg ? draft.applicant.organizationName || undefined : undefined,
      recipientName: isPhysical ? draft.recipient.name || undefined : undefined,
      recipientPhone: isPhysical ? draft.recipient.phone || undefined : undefined,
      recipientAddress: isPhysical ? [draft.recipient.address, draft.recipient.addressDetail].filter(Boolean).join(" ") || undefined : undefined,
      deliveryRequest: isPhysical ? draft.recipient.deliveryRequest || undefined : undefined,
    };
    saveApplications([{ applicationNumber: num, applicantType: isOrg ? "법인·단체" : "개인", cardType: cardTypeLabels[design.cardType], applicantName: draft.applicant.name || draft.applicant.englishName || "", applicantEmail: draft.applicant.email, ownerEmail: user?.email, phone: draft.applicant.phone, quantity: serverQuantity ?? draft.quantity, status: "SUBMITTED", submittedAt: new Date().toISOString().slice(0, 10), detail }, ...applications]);
    setApplicationNumber(num);
    goTo(4);
  };

  const submit = async () => {
    if (user?.source === "api") {
      try {
        const physical = draft.issuanceMethod === "mobile_and_physical";
        const isOrg = draft.applicantType === "organization";
        const isStudent = design.cardType === "student";
        // 학생증만 orientation/schoolType를 전송한다(일반 카드는 학생증 필드를 보내면 안 됨).
        const orientation = isStudent ? toOrientation(draft.cardOrientation ?? design.orientation) : undefined;
        const schoolType = isStudent ? toSchoolType(draft.applicant.schoolLevel) : undefined;
        const isUniversity = schoolType === "UNIVERSITY";
        const receiver = physical
          ? { sameAsApplicant: draft.recipient.sameAsApplicant, ...(isOrg ? { organizationName: draft.recipient.organizationName, department: draft.recipient.department } : {}), name: draft.recipient.name, phone: draft.recipient.phone, zipCode: draft.recipient.postalCode, address: draft.recipient.address, detailAddress: draft.recipient.addressDetail, deliveryRequest: draft.recipient.deliveryRequest }
          : undefined;
        const request = isOrg ? {
          cardTypeId: cardTypeIds[design.cardType], issueType: toIssueType(draft.issuanceMethod), orientation, schoolType,
          applicant: { organizationName: draft.applicant.organizationName, department: draft.applicant.department, name: draft.applicant.name, phone: draft.applicant.phone, email: draft.applicant.email || undefined },
          receiver,
        } : {
          cardTypeId: cardTypeIds[design.cardType], issueType: toIssueType(draft.issuanceMethod), orientation, schoolType,
          applicant: { name: draft.applicant.name || draft.applicant.englishName, phone: draft.applicant.phone, email: draft.applicant.email || undefined },
          receiver,
          // 학번·학과는 대학교(UNIVERSITY)에서만 전송한다(고등학교·비학생증은 미전송).
          member: { englishName: draft.applicant.englishName || draft.applicant.name, birthDate: draft.applicant.birthDate, nationality: draft.applicant.nationality, birthTime: draft.applicant.birthTime || undefined, birthRegion: draft.applicant.birthPlace, gender: toGender(draft.applicant.gender), entryDate: draft.applicant.koreaEntryDate || undefined, studentId: isStudent && isUniversity ? draft.applicant.studentNumber : undefined, department: isStudent && isUniversity ? draft.applicant.department : undefined },
        };
        const form = new FormData();
        form.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
        if (isOrg) {
          // logo·submitFile은 항상, seal은 일반 카드 필수·학생증 선택.
          if (!draft.logoFile?.file || !draft.archiveFile?.file) throw new Error("로고와 제출 ZIP 파일을 다시 선택해 주세요.");
          if (!isStudent && !draft.sealFile?.file) throw new Error("직인 파일을 다시 선택해 주세요.");
          form.append("logo", draft.logoFile.file); form.append("submitFile", draft.archiveFile.file);
          if (draft.sealFile?.file) form.append("seal", draft.sealFile.file);
        } else {
          if (!draft.faceFile?.file) throw new Error("본인 사진을 다시 선택해 주세요.");
          if (isStudent && !draft.logoFile?.file) throw new Error("학교 로고 파일을 선택해 주세요.");
          form.append("photo", draft.faceFile.file);
          if (draft.logoFile?.file) form.append("schoolLogo", draft.logoFile.file);
          if (draft.sealFile?.file) form.append("schoolSeal", draft.sealFile.file);
        }
        const result = await api.createApplication(form, isOrg);
        // 단체 수량은 사용자 입력이 아니라 서버가 계산한 totalQuantity를 표시한다.
        saveLocalApplication(result.applicationNumber, result.totalQuantity);
        return;
      } catch (error) {
        if (error instanceof ApiError && error.errors?.length) {
          const detail = error.errors.slice(0, 3).map((e) => `${e.row != null ? `${e.row}행 ` : ""}${e.field}: ${e.message}`).join(" / ");
          showToast(`${error.message}${detail ? ` — ${detail}` : ""}`);
        } else {
          showToast(error instanceof Error ? error.message : "신청 API 호출에 실패했습니다.");
        }
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
