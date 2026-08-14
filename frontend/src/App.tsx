// Root component: route table mapping URLs to pages.
import { Route, Routes } from "react-router-dom";
import { PublicLayout } from "./components/layout/PublicLayout";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { Toaster } from "./components/ui/toast";
import { HomePage } from "./pages/HomePage/HomePage";
import { DesignPage } from "./pages/DesignPage/DesignPage";
import { LookupPage } from "./pages/LookupPage/LookupPage";
import { MobileCardPage } from "./pages/MobileCardPage/MobileCardPage";
import { SupportPage } from "./pages/SupportPage/SupportPage";
import { FaqPage } from "./pages/FaqPage/FaqPage";
import { NoticesPage } from "./pages/NoticesPage/NoticesPage";
import { NoticeDetailPage } from "./pages/NoticeDetailPage/NoticeDetailPage";
import { MyPage } from "./pages/MyPage/MyPage";
import { LoginPage } from "./pages/LoginPage/LoginPage";
import { SignupPage } from "./pages/SignupPage/SignupPage";
import { AccountRecoveryPage } from "./pages/AccountRecoveryPage/AccountRecoveryPage";
import { AdminPage } from "./pages/AdminPage/AdminPage";
import { StubPage } from "./pages/StubPage/StubPage";
import { CompanyPage } from "./pages/CompanyPage/CompanyPage";
import { GreetingsPage } from "./pages/GreetingsPage/GreetingsPage";
import { ReviewsPage } from "./pages/ReviewsPage/ReviewsPage";
import { ReviewDetailPage } from "./pages/ReviewDetailPage/ReviewDetailPage";
import { ReviewEditorPage } from "./pages/ReviewEditorPage/ReviewEditorPage";
import { InquiryPage } from "./pages/InquiryPage/InquiryPage";
import { InquiryDetailPage } from "./pages/InquiryDetailPage/InquiryDetailPage";
import { EventsPage } from "./pages/EventsPage/EventsPage";
import { ApplyPage } from "./pages/ApplyPage/ApplyPage";

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
          <Route path="/mypage/inquiry/:inquiryId" element={<InquiryDetailPage />} />
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
