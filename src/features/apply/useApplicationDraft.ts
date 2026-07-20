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
      if (raw) return { ...createEmptyDraft(), ...(JSON.parse(raw) as ApplicationDraft) };
    } catch {
      /* ignore malformed storage */
    }
    return createEmptyDraft();
  });

  useEffect(() => {
    try {
      // Strip non-serialisable preview URLs before persisting.
      const { logoFile, sealFile, archiveFile, ...rest } = draft;
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          ...rest,
          logoFile: logoFile ? { name: logoFile.name, size: logoFile.size } : undefined,
          sealFile: sealFile ? { name: sealFile.name, size: sealFile.size } : undefined,
          archiveFile: archiveFile ? { name: archiveFile.name, size: archiveFile.size } : undefined,
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
