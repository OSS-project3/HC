import { HeroSection } from "../components/home/HeroSection";
import { MainDesignsSection } from "../components/home/MainDesignsSection";
import { ServiceCoreSection } from "../components/home/ServiceCoreSection";
import { MerchandiseSection } from "../components/home/MerchandiseSection";
import { ContactSection } from "../components/home/ContactSection";
import { PartnersSection } from "../components/home/PartnersSection";
import { companyInfo } from "../config/company";
import "../components/home/home.css";

export function HomePage() {
  return (
    <div className="home">
      {/* Tagline text under the header (home page only). */}
      <p className="home-tagline page-container">{companyInfo.tagline}</p>

      <HeroSection />
      <MainDesignsSection />
      <ServiceCoreSection />
      <MerchandiseSection />
      <ContactSection />
      <PartnersSection />
    </div>
  );
}
