import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { translations } from "./translations";

export type Language = "ko" | "en";

// Module-level mirror of the current language so non-component code
// (toast helpers, validators) can translate without the hook.
let currentLanguage: Language = localStorage.getItem("site-language") === "en" ? "en" : "ko";

/** Translate outside React components (toast messages, validation errors). */
export function translateText(text: string): string {
  return currentLanguage === "en" ? translations[text] ?? text : text;
}

/** Current UI language for non-component code (e.g. API client headers). */
export function getLanguage(): Language {
  return currentLanguage;
}

interface LanguageValue { language: Language; setLanguage: (language: Language) => void; t: (text: string) => string }
const LanguageContext = createContext<LanguageValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<Language>(currentLanguage);
  const setLanguage = (next: Language) => {
    setLanguageState(next);
    currentLanguage = next;
    localStorage.setItem("site-language", next);
  };
  useEffect(() => { document.documentElement.lang = language; }, [language]);
  const value = useMemo(() => ({ language, setLanguage, t: (text: string) => language === "en" ? translations[text] ?? text : text }), [language]);
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) throw new Error("useLanguage must be used within LanguageProvider");
  return context;
}
