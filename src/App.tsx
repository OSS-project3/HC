// Root component: route table mapping URLs to pages.
import { Route, Routes } from "react-router-dom";
import { PublicLayout } from "./components/layout/PublicLayout";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { Toaster } from "./components/ui/toast";
import { HomePage } from "./pages/HomePage";
import { DesignPage } from "./pages/DesignPage";
import { LookupPage } from "./pages/LookupPage";
import { SupportPage } from "./pages/SupportPage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { AdminPage } from "./pages/AdminPage";
import { StubPage } from "./pages/StubPage";
import { ApplyPage } from "./pages/apply/ApplyPage";

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<HomePage />} />
          <Route path="/design" element={<DesignPage />} />
          <Route path="/apply/*" element={<ApplyPage />} />
          <Route path="/lookup" element={<LookupPage />} />
          <Route path="/support" element={<SupportPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route
            path="/company"
            element={<StubPage title="회사 소개" note="레이아웃 검토 중입니다." />}
          />
          <Route
            path="/reviews"
            element={<StubPage title="후기" note="콘텐츠가 준비되는 대로 제공됩니다." />}
          />
          <Route
            path="/events"
            element={<StubPage title="행사사업" note="콘텐츠가 준비되는 대로 제공됩니다." />}
          />
          <Route path="/admin" element={<AdminPage />} />
          <Route
            path="*"
            element={<StubPage title="페이지를 찾을 수 없습니다" note="주소를 다시 확인해 주세요." />}
          />
        </Route>
      </Routes>
      <Toaster />
    </>
  );
}
