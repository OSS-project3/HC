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
  return (
    <div className="step">
      <h2 className="step__heading">사진 및 파일 등록</h2>
      <p className="step__desc">등록된 로고 및 직인은 예시 이미지에 반영됩니다.</p>

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
