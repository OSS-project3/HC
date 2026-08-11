// Home page: composes hero, 주요 디자인, 서비스 핵심, merchandise, contact, and partners sections.
import { useState } from "react";
import { HeroSection } from "../components/home/HeroSection";
import { MainDesignsSection } from "../components/home/MainDesignsSection";
import { ServiceCoreSection } from "../components/home/ServiceCoreSection";
import { MerchandiseSection } from "../components/home/MerchandiseSection";
import { ContactSection } from "../components/home/ContactSection";
import { PartnersSection } from "../components/home/PartnersSection";
import { companyInfo } from "../config/company";
import "../components/home/home.css";

export function HomePage() {
  const [zodiacIndex, setZodiacIndex] = useState(0);

  return (
    <div className="home">
      {/* Tagline: its own strip flush under the header (not part of the header). */}
      <p className="home-tagline page-container">{companyInfo.tagline}</p>

      <HeroSection zodiacIndex={zodiacIndex} onZodiacSelect={setZodiacIndex} />
      <MainDesignsSection zodiacIndex={zodiacIndex} onZodiacChange={setZodiacIndex} />
      <ServiceCoreSection />
      <MerchandiseSection />
      <ContactSection />
      <PartnersSection />
    </div>
  );
}
