// Hook managing multi-step application draft state (with localStorage persistence).
import { useCallback, useEffect, useState } from "react";
import { createEmptyDraft, type ApplicationDraft } from "./types";

const STORAGE_KEY = "application-draft";

/**
 * Multi-step draft state.
 *
 * Persisted in **sessionStorage** (not localStorage) so personal data does not
 * linger indefinitely, and cleared once the application is submitted. File
 * objects/preview URLs are intentionally NOT persisted.
 */
export function useApplicationDraft() {
  const [draft, setDraft] = useState<ApplicationDraft>(() => {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (raw) {
        // 파일(File)은 sessionStorage에 바이너리로 저장되지 않는다(아래 저장 로직에서 name/size만 남김).
        // {name,size}만 남은 '유령 첨부'를 화면에 첨부된 것처럼 표시하면 제출 시점에야 실패하므로,
        // 복원 시 파일 필드는 제외하고 재첨부를 요구한다.
        const parsed = JSON.parse(raw) as ApplicationDraft;
        delete parsed.logoFile;
        delete parsed.sealFile;
        delete parsed.archiveFile;
        delete parsed.faceFile;
        return { ...createEmptyDraft(), ...parsed };
      }
    } catch {
      /* ignore malformed storage */
    }
    return createEmptyDraft();
  });

  useEffect(() => {
    try {
      // Strip non-serialisable preview URLs before persisting.
      const { logoFile, sealFile, archiveFile, faceFile, ...rest } = draft;
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          ...rest,
          logoFile: logoFile ? { name: logoFile.name, size: logoFile.size } : undefined,
          sealFile: sealFile ? { name: sealFile.name, size: sealFile.size } : undefined,
          archiveFile: archiveFile ? { name: archiveFile.name, size: archiveFile.size } : undefined,
          faceFile: faceFile ? { name: faceFile.name, size: faceFile.size } : undefined,
        }),
      );
    } catch {
      /* ignore quota errors */
    }
  }, [draft]);

  const update = useCallback((patch: Partial<ApplicationDraft>) => {
    setDraft((prev) => ({ ...prev, ...patch }));
  }, []);

  const clear = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setDraft(createEmptyDraft());
  }, []);

  return { draft, update, setDraft, clear };
}
