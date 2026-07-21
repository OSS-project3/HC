// Design page: full card-design gallery by type.
import { Link } from "react-router-dom";
import { cardCategories } from "../data/cards";
import { CardCarousel } from "../components/gallery/CardCarousel";
import { ArrowUpRight } from "../components/ui/icons";
import "./DesignPage.css";

export function DesignPage() {
  return (
    <div className="design">
      <header className="design__hero page-container">
        <p className="eyebrow">카드 디자인</p>
        <h1 className="design__title">카드 디자인</h1>
        <p className="section-lead">한국의 결을 따라 전통과 현재를 담다</p>
      </header>

      {cardCategories.map((cat) => {
        const firstId = cat.cards[0]?.id ?? "";
        return (
          <section className="design__cat page-container" key={cat.cardType}>
            <div className="design__cat-head">
              <h2 className="design__cat-title">{cat.title}</h2>
              <Link className="design__view-all" to={`/design#${cat.cardType}`}>
                전체 보기
                <span className="design__view-arrow" aria-hidden="true">
                  <ArrowUpRight width={16} height={16} />
                </span>
              </Link>
            </div>
            <div className="design__divider" />

            <CardCarousel cards={cat.cards} orientation={cat.cards[0].orientation} />

            <div className="design__cat-foot">
              <p className="design__note">※ 위 증은 특허출원에 의한 견본품입니다</p>
              <Link className="design__apply-btn" to={`/apply?designId=${firstId}`}>
                제작 신청
              </Link>
            </div>
          </section>
        );
      })}
    </div>
  );
}
