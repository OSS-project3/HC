import { Outlet } from "react-router-dom";
import { Header } from "../header/Header";
import { Footer } from "../footer/Footer";

export function PublicLayout() {
  return (
    <>
      <Header />
      <main id="main">
        <Outlet />
      </main>
      <Footer />
    </>
  );
}
