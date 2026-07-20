import { partners } from "../../data/partners";
import { ImagePlaceholder } from "../ui/ImagePlaceholder";

/**
 * "한글과 세종이 함께할 예정인 각종 기관 단체". Real logos are delivered later; each
 * slot is a black box (fixed size, object-fit: contain when the asset lands).
 */
export function PartnersSection() {
  return (
    <section className="partners page-container">
      <h2 className="partners__title">한글과 세종이 함께할 예정인 각종 기관 단체</h2>
      <ul className="partners__grid">
        {partners.map((p) => (
          <li className="partners__cell" key={p.id}>
            {p.logo ? (
              <img className="partners__logo" src={p.logo} alt={p.name} />
            ) : (
              <ImagePlaceholder label="기관 로고" />
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
