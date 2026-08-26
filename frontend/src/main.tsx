// App entry point: mounts the React root with router, auth, and toast providers.
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { siteNameKo } from "./config/company";
import { AuthProvider } from "./features/auth/AuthContext";
import { LanguageProvider } from "./features/i18n/LanguageContext";
import "./styles/globals.css";

document.title = siteNameKo + " · 한글 오행 기반 한국 이름 발급";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <LanguageProvider><AuthProvider><App /></AuthProvider></LanguageProvider>
    </BrowserRouter>
  </StrictMode>,
);
