// Root component: route table mapping URLs to pages.
import { Route, Routes } from "react-router-dom";
import { PublicLayout } from "./components/layout/PublicLayout";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { Toaster } from "./components/ui/toast";
import { HomePage } from "./pages/HomePage";
import { DesignPage } from "./pages/DesignPage";
import { LookupPage } from "./pages/LookupPage";
import { MobileCardPage } from "./pages/MobileCardPage";
import { SupportPage } from "./pages/SupportPage";
import { FaqPage } from "./pages/FaqPage";
import { NoticesPage } from "./pages/NoticesPage";
import { NoticeDetailPage } from "./pages/NoticeDetailPage";
import { MyPage } from "./pages/MyPage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { AccountRecoveryPage } from "./pages/AccountRecoveryPage";
import { AdminPage } from "./pages/AdminPage";
import { StubPage } from "./pages/StubPage";
import { CompanyPage } from "./pages/CompanyPage";
import { GreetingsPage } from "./pages/GreetingsPage";
import { ReviewsPage } from "./pages/ReviewsPage";
import { ReviewDetailPage } from "./pages/ReviewDetailPage";
import { ReviewEditorPage } from "./pages/ReviewEditorPage";
import { InquiryPage } from "./pages/InquiryPage";
import { EventsPage } from "./pages/EventsPage";
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
          <Route path="/mobile-card" element={<MobileCardPage />} />
          <Route path="/support" element={<SupportPage />} />
          <Route path="/inquiry" element={<InquiryPage />} />
          <Route path="/faq" element={<FaqPage />} />
          <Route path="/notices" element={<NoticesPage />} />
          <Route path="/notices/:noticeId" element={<NoticeDetailPage />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/account-recovery" element={<AccountRecoveryPage />} />
          <Route path="/company" element={<CompanyPage />} />
          <Route path="/greetings" element={<GreetingsPage />} />
          <Route path="/reviews" element={<ReviewsPage />} />
          <Route path="/reviews/new" element={<ReviewEditorPage />} />
          <Route path="/reviews/:reviewId/edit" element={<ReviewEditorPage />} />
          <Route path="/reviews/:reviewId" element={<ReviewDetailPage />} />
          <Route path="/events" element={<EventsPage />} />
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
