import { createContext, useContext, useEffect, useMemo, useState } from "react";

export type Language = "ko" | "en";

const translations: Record<string, string> = {
  "회사 소개": "About Us", "디자인": "Designs", "제작 신청": "Apply", "조회": "Lookup", "후기": "Reviews", "행사사업": "Events", "고객지원": "Support", "관리": "Admin",
  "인사말": "Greetings", "회사소개": "Company", "오시는 길": "Directions", "명예한국인증": "Honorary Korean ID", "명예시민증": "Honorary Citizen ID", "학생증": "Student ID", "방문증": "Visitor Pass",
  "공지사항": "Notices", "자주 묻는 질문": "FAQ", "제작 이야기": "Our Stories", "상담 문의": "Contact", "로그인": "Sign in", "로그아웃": "Sign out", "한국어": "Korean", "영어": "English",
  "개인정보처리방침": "Privacy Policy", "이용약관": "Terms of Use", "이메일무단수집거부": "Email Collection Policy", "사이트맵": "Sitemap",
};

interface LanguageValue { language: Language; setLanguage: (language: Language) => void; t: (text: string) => string }
const LanguageContext = createContext<LanguageValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() => localStorage.getItem("site-language") === "en" ? "en" : "ko");
  const setLanguage = (next: Language) => { setLanguageState(next); localStorage.setItem("site-language", next); };
  useEffect(() => { document.documentElement.lang = language; }, [language]);
  const value = useMemo(() => ({ language, setLanguage, t: (text: string) => language === "en" ? translations[text] ?? text : text }), [language]);
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) throw new Error("useLanguage must be used within LanguageProvider");
  return context;
}
