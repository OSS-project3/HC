// Apply step: photo / document upload.
import { useState } from "react";
import type { ApplicationDraft } from "../../../features/apply/types";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";
import { FileUploadBox } from "../FileUploadBox";

interface StepFilesProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
  onPrev: () => void;
}

export function StepFiles({ draft, update, onNext, onPrev }: StepFilesProps) {
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";

  // Required uploads mirror the submit-time checks in ApplyPage (seal is optional
  // for student cards; the ZIP + logo are always required for organizations).
  const [showErrors, setShowErrors] = useState(false);
  const missingKeys: string[] = [];
  if (isOrg) {
    if (!draft.logoFile?.file) missingKeys.push("logoFile");
    if (!isStudent && !draft.sealFile?.file) missingKeys.push("sealFile");
    if (!draft.archiveFile?.file) missingKeys.push("archiveFile");
  } else {
    if (!draft.faceFile?.file) missingKeys.push("faceFile");
    if (isStudent && !draft.logoFile?.file) missingKeys.push("logoFile");
  }
  const isComplete = missingKeys.length === 0;
  const hasError = (key: string) => showErrors && missingKeys.includes(key);

  const handleNext = () => {
    if (!isComplete) {
      setShowErrors(true);
      document.getElementById(`upload-${missingKeys[0]}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
      return;
    }
    onNext();
  };

  const desc = isStudent
    ? isOrg
      ? "학교 로고와 직인, 그리고 학생별 프로필 사진 및 사주 정보 파일을 등록해 주세요."
      : "학생증에 사용할 본인 프로필 사진과 학교 로고 및 직인을 등록해 주세요."
    : isOrg
      ? "법인·단체 로고와 직인, 그리고 개인별 프로필 사진 및 사주 정보 파일을 등록해 주세요."
      : "카드에 사용할 본인 프로필 사진을 등록해 주세요.";

  return (
    <div className="step">
      <p className="step__eyebrow">{isOrg ? "법인·단체 신청" : "개인 신청"}</p>
      <h2 className="step__heading">사진 및 파일 등록</h2>
      <p className="step__desc">{desc}</p>

      {isStudent ? (
        isOrg ? (
          <>
            <div className="upload-row">
              <FileUploadBox
                label="학교 로고"
                accept="image/png,image/jpeg"
                hint="PNG, JPG 이미지"
                variant="image"
                file={draft.logoFile}
                error={hasError("logoFile")}
                id="upload-logoFile"
                onChange={(f) => update({ logoFile: f })}
              />
              <FileUploadBox
                label="학교 직인 (선택)"
                accept="image/png,image/jpeg"
                hint="PNG, JPG 이미지"
                variant="image"
                file={draft.sealFile}
                onChange={(f) => update({ sealFile: f })}
              />
            </div>
            <FileUploadBox
              label="첨부파일"
              accept=".zip,application/zip"
              hint="ZIP (학생별 프로필 사진과 사주 정보 파일)"
              variant="archive"
              file={draft.archiveFile}
              error={hasError("archiveFile")}
              id="upload-archiveFile"
              onChange={(f) => update({ archiveFile: f })}
            />
          </>
        ) : (
          <>
            <FileUploadBox
              label="본인 프로필 사진"
              accept="image/png,image/jpeg"
              hint="PNG, JPG 이미지 1개"
              variant="image"
              file={draft.faceFile}
              error={hasError("faceFile")}
              id="upload-faceFile"
              onChange={(f) => update({ faceFile: f })}
            />
            <div className="upload-row">
              <FileUploadBox
                label="학교 로고"
                accept="image/png,image/jpeg"
                hint="PNG, JPG 이미지"
                variant="image"
                file={draft.logoFile}
                error={hasError("logoFile")}
                id="upload-logoFile"
                onChange={(f) => update({ logoFile: f })}
              />
              <FileUploadBox
                label="학교 직인 (선택)"
                accept="image/png,image/jpeg"
                hint="PNG, JPG 이미지"
                variant="image"
                file={draft.sealFile}
                onChange={(f) => update({ sealFile: f })}
              />
            </div>
          </>
        )
      ) : isOrg ? (
        <>
          <div className="upload-row">
            <FileUploadBox
              label="법인·단체 로고 이미지"
              accept="image/png,image/jpeg"
              hint="PNG, JPG 이미지"
              variant="image"
              file={draft.logoFile}
              error={hasError("logoFile")}
              id="upload-logoFile"
              onChange={(f) => update({ logoFile: f })}
            />
            <FileUploadBox
              label="법인·단체 직인 이미지"
              accept="image/png,image/jpeg"
              hint="PNG, JPG 이미지"
              variant="image"
              file={draft.sealFile}
              error={hasError("sealFile")}
              id="upload-sealFile"
              onChange={(f) => update({ sealFile: f })}
            />
          </div>

          <FileUploadBox
            label="첨부파일"
            accept=".zip,application/zip"
            hint="ZIP (개인별 프로필 사진과 사주 정보 파일)"
            variant="archive"
            file={draft.archiveFile}
            error={hasError("archiveFile")}
            id="upload-archiveFile"
            onChange={(f) => update({ archiveFile: f })}
          />
        </>
      ) : (
        <FileUploadBox
          label="프로필 사진"
          accept="image/png,image/jpeg"
          hint="PNG, JPG 이미지 1개"
          variant="image"
          file={draft.faceFile}
          error={hasError("faceFile")}
          id="upload-faceFile"
          onChange={(f) => update({ faceFile: f })}
        />
      )}

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
