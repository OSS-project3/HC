import { Route, Routes } from "react-router-dom";
import { PublicLayout } from "./components/layout/PublicLayout";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { Toaster } from "./components/ui/toast";
import { HomePage } from "./pages/HomePage";
import { DesignPage } from "./pages/DesignPage";
import { LookupPage } from "./pages/LookupPage";
import { SupportPage } from "./pages/SupportPage";
import { LoginPage } from "./pages/LoginPage";
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
          <Route
            path="/admin"
            element={
              <StubPage
                title="관리"
                note="관리자 전용 페이지입니다. 접근 권한 확인 후 이용할 수 있습니다."
              />
            }
          />
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
