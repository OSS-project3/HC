// Root component: route table mapping URLs to pages.
import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { PublicLayout } from "./components/layout/PublicLayout";
import { ScrollToTop } from "./components/layout/ScrollToTop";
import { Toaster } from "./components/ui/toast";
import { HomePage } from "./pages/HomePage/HomePage";

const DesignPage = lazy(() => import("./pages/DesignPage/DesignPage").then((m) => ({ default: m.DesignPage })));
const LookupPage = lazy(() => import("./pages/LookupPage/LookupPage").then((m) => ({ default: m.LookupPage })));
const MobileCardPage = lazy(() => import("./pages/MobileCardPage/MobileCardPage").then((m) => ({ default: m.MobileCardPage })));
const SupportPage = lazy(() => import("./pages/SupportPage/SupportPage").then((m) => ({ default: m.SupportPage })));
const FaqPage = lazy(() => import("./pages/FaqPage/FaqPage").then((m) => ({ default: m.FaqPage })));
const NoticesPage = lazy(() => import("./pages/NoticesPage/NoticesPage").then((m) => ({ default: m.NoticesPage })));
const NoticeDetailPage = lazy(() => import("./pages/NoticeDetailPage/NoticeDetailPage").then((m) => ({ default: m.NoticeDetailPage })));
const MyPage = lazy(() => import("./pages/MyPage/MyPage").then((m) => ({ default: m.MyPage })));
const LoginPage = lazy(() => import("./pages/LoginPage/LoginPage").then((m) => ({ default: m.LoginPage })));
const SignupPage = lazy(() => import("./pages/SignupPage/SignupPage").then((m) => ({ default: m.SignupPage })));
const AccountRecoveryPage = lazy(() => import("./pages/AccountRecoveryPage/AccountRecoveryPage").then((m) => ({ default: m.AccountRecoveryPage })));
const AdminPage = lazy(() => import("./pages/AdminPage/AdminPage").then((m) => ({ default: m.AdminPage })));
const StubPage = lazy(() => import("./pages/StubPage/StubPage").then((m) => ({ default: m.StubPage })));
const CompanyPage = lazy(() => import("./pages/CompanyPage/CompanyPage").then((m) => ({ default: m.CompanyPage })));
const GreetingsPage = lazy(() => import("./pages/GreetingsPage/GreetingsPage").then((m) => ({ default: m.GreetingsPage })));
const ReviewsPage = lazy(() => import("./pages/ReviewsPage/ReviewsPage").then((m) => ({ default: m.ReviewsPage })));
const ReviewDetailPage = lazy(() => import("./pages/ReviewDetailPage/ReviewDetailPage").then((m) => ({ default: m.ReviewDetailPage })));
const ReviewEditorPage = lazy(() => import("./pages/ReviewEditorPage/ReviewEditorPage").then((m) => ({ default: m.ReviewEditorPage })));
const InquiryPage = lazy(() => import("./pages/InquiryPage/InquiryPage").then((m) => ({ default: m.InquiryPage })));
const InquiryDetailPage = lazy(() => import("./pages/InquiryDetailPage/InquiryDetailPage").then((m) => ({ default: m.InquiryDetailPage })));
const EventsPage = lazy(() => import("./pages/EventsPage/EventsPage").then((m) => ({ default: m.EventsPage })));
const ApplyPage = lazy(() => import("./pages/ApplyPage/ApplyPage").then((m) => ({ default: m.ApplyPage })));

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Suspense fallback={null}>
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
      </Suspense>
      <Toaster />
    </>
  );
}
