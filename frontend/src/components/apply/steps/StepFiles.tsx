// Apply step: photo / document upload.
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
  const isVisitor = draft.cardType === "visitor";
  const isStudent = draft.cardType === "student";

  return (
    <div className="step">
      <h2 className="step__heading">사진 및 파일 등록</h2>
      <p className="step__desc">
        {isVisitor
          ? "방문증에 사용할 본인 얼굴 사진을 등록해 주세요."
          : isStudent
            ? "학생증에 사용할 본인 프로필 사진과 학교 로고 및 직인을 등록해 주세요."
            : "등록된 로고 및 직인은 예시 이미지에 반영됩니다."}
      </p>

      {isVisitor ? (
        <FileUploadBox
          label="본인 얼굴 사진"
          accept="image/png,image/jpeg"
          hint="PNG, JPG 이미지 1개"
          variant="image"
          file={draft.faceFile}
          onChange={(f) => update({ faceFile: f })}
        />
      ) : isStudent ? (
        <>
          <FileUploadBox
            label="본인 프로필 사진"
            accept="image/png,image/jpeg"
            hint="PNG, JPG 이미지 1개"
            variant="image"
            file={draft.faceFile}
            onChange={(f) => update({ faceFile: f })}
          />
          <div className="upload-row">
            <FileUploadBox
              label="학교 로고"
              accept="image/png,image/jpeg"
              hint="PNG, JPG 이미지"
              variant="image"
              file={draft.logoFile}
              onChange={(f) => update({ logoFile: f })}
            />
            <FileUploadBox
              label="학교 직인"
              accept="image/png,image/jpeg"
              hint="PNG, JPG 이미지"
              variant="image"
              file={draft.sealFile}
              onChange={(f) => update({ sealFile: f })}
            />
          </div>
        </>
      ) : (
        <>
          <div className="upload-row">
            <FileUploadBox
              label="로고 이미지"
              accept="image/png,image/jpeg"
              hint="PNG, JPG (최대 ??MB)"
              variant="image"
              file={draft.logoFile}
              onChange={(f) => update({ logoFile: f })}
            />
            <FileUploadBox
              label="직인 이미지"
              accept="image/png,image/jpeg"
              hint="PNG, JPG (최대 ??MB)"
              variant="image"
              file={draft.sealFile}
              onChange={(f) => update({ sealFile: f })}
            />
          </div>

          <FileUploadBox
            label="제출 파일"
            accept=".zip,application/zip"
            hint="ZIP (최대 ??GB)"
            variant="archive"
            file={draft.archiveFile}
            onChange={(f) => update({ archiveFile: f })}
          />
        </>
      )}

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
