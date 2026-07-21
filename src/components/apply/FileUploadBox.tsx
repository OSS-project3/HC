// Apply flow: drag-and-drop / click file upload box.
import { useRef, useState } from "react";
import clsx from "clsx";
import type { UploadFileInfo } from "../../features/apply/types";
import { ImageIcon } from "../ui/icons";

interface FileUploadBoxProps {
  label: string;
  accept: string;
  hint: string;
  variant?: "image" | "archive";
  file?: UploadFileInfo;
  onChange: (file: UploadFileInfo | undefined) => void;
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function FileUploadBox({ label, accept, hint, variant = "image", file, onChange }: FileUploadBoxProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);

  const handleFiles = (files: FileList | null) => {
    const f = files?.[0];
    if (!f) return;
    const previewUrl = variant === "image" ? URL.createObjectURL(f) : undefined;
    onChange({ name: f.name, size: f.size, previewUrl });
  };

  return (
    <div className="upload">
      <p className="upload__label">{label}</p>
      <div
        className={clsx(
          "upload__box",
          variant === "archive" && "upload__box--archive",
          dragOver && "upload__box--drag",
          file && "upload__box--filled",
        )}
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          handleFiles(e.dataTransfer.files);
        }}
      >
        {file ? (
          <div className="upload__filled">
            {file.previewUrl ? (
              <img className="upload__preview" src={file.previewUrl} alt={file.name} />
            ) : (
              <span className="upload__zip">ZIP</span>
            )}
            <span className="upload__filename">{file.name}</span>
            <span className="upload__filesize">{formatSize(file.size)}</span>
          </div>
        ) : (
          <div className="upload__empty">
            <span className="upload__icon">
              {variant === "archive" ? <span className="upload__zip">ZIP</span> : <ImageIcon width={40} height={40} />}
            </span>
            <span className="upload__cta">클릭하여 업로드</span>
            <span className="upload__hint">{hint}</span>
          </div>
        )}
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          className="visually-hidden"
          onChange={(e) => handleFiles(e.target.files)}
        />
      </div>

      {file && (
        <div className="upload__controls">
          <button type="button" onClick={() => inputRef.current?.click()}>
            교체
          </button>
          <button
            type="button"
            onClick={() => {
              if (file.previewUrl) URL.revokeObjectURL(file.previewUrl);
              onChange(undefined);
              if (inputRef.current) inputRef.current.value = "";
            }}
          >
            삭제
          </button>
        </div>
      )}
    </div>
  );
}
