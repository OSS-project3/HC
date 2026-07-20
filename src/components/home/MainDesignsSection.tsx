import { Link } from "react-router-dom";
import { ArrowUpRight } from "../ui/icons";

// Temporary preview card images (public/images/cards/).
const SEC2_LANDSCAPE = "/images/cards/main%20sec2%20ex1.png";
const SEC2_PORTRAIT = "/images/cards/main%20sec2%20ex2.png";

/**
 * "주요 디자인" — preview of the main card types with per-type 신청 buttons and a
 * "전체 보기" link to the full design page.
 */
export function MainDesignsSection() {
  return (
    <section className="main-designs page-container">
      <div className="main-designs__head">
        <h2 className="main-designs__title">주요 디자인</h2>
        <Link className="main-designs__all" to="/design">
          전체 보기
          <span className="main-designs__arrow" aria-hidden="true">
            <ArrowUpRight width={16} height={16} />
          </span>
        </Link>
      </div>

      <div className="main-designs__grid">
        <div className="design-group">
          <p className="design-group__label">명예한국인증 · 명예시민증 · 학생증</p>
          <div className="design-group__card design-group__card--landscape">
            <img className="design-group__img" src={SEC2_LANDSCAPE} alt="주요 디자인 예시" />
          </div>
          <div className="design-group__buttons">
            <Link className="design-chip" to="/apply?designId=honorary-korean-01">
              명예한국인증 신청
            </Link>
            <Link className="design-chip" to="/apply?designId=honorary-citizen-01">
              명예시민증 신청
            </Link>
            <Link className="design-chip" to="/apply?designId=student-01">
              학생증 신청
            </Link>
          </div>
        </div>

        <div className="main-designs__divider" aria-hidden="true" />

        <div className="design-group">
          <p className="design-group__label">방문증 · 학생증</p>
          <div className="design-group__card design-group__card--portrait">
            <img className="design-group__img" src={SEC2_PORTRAIT} alt="주요 디자인 예시" />
          </div>
          <div className="design-group__buttons">
            <Link className="design-chip design-chip--soft" to="/apply?designId=visitor-01">
              방문증 신청
            </Link>
            <Link className="design-chip design-chip--filled" to="/apply?designId=student-01">
              학생증 신청
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
