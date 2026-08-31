// Home partner-organizations logo grid.
import { partners } from "../../data/partners";
import { useLanguage } from "../../features/i18n/LanguageContext";
import { ImagePlaceholder } from "../ui/ImagePlaceholder";

/**
 * "한글과 세종이 함께할 예정인 각종 기관 단체". Real logos are delivered later; each
 * slot is a black box (fixed size, object-fit: contain when the asset lands).
 */
export function PartnersSection() {
  const { t } = useLanguage();
  return (
    <section className="partners page-container">
      <h2 className="partners__title">{t("한글과 세종이 함께할 예정인 각종 기관 단체")}</h2>
      <ul className="partners__grid">
        {partners.map((p) => (
          <li className="partners__cell" key={p.id}>
            {p.logo ? (
              <img className="partners__logo" src={p.logo} alt={t(p.name)} />
            ) : (
              <ImagePlaceholder label={t("기관 로고")} />
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
