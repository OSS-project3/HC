import { useNavigate } from "react-router-dom";
import { companyInfo } from "../../config/company";
import { PhoneIcon, MailIcon, DocIcon, ChatIcon, ChevronRight, ArrowUpRight } from "../ui/icons";

/**
 * "상담 문의". The whole band uses a #F2E9DF background at 50% opacity — applied
 * via a background *alpha* (not `opacity` on the element), so text/icons stay
 * fully opaque. See `.contact::before` in home.css.
 */
export function ContactSection() {
  const navigate = useNavigate();

  return (
    <section className="contact">
      <div className="contact__inner page-container">
        <h2 className="contact__title">상담 문의</h2>
        <div className="contact__grid">
          <article className="contact__item">
            <span className="contact__icon">
              <PhoneIcon />
            </span>
            <h3 className="contact__name">전화 상담</h3>
            <p className="contact__meta">
              {companyInfo.businessHours}
              <br />({companyInfo.lunchHours})
            </p>
            <a className="contact__action" href={`tel:${companyInfo.phone.replace(/\D/g, "")}`}>
              {companyInfo.phone}
            </a>
          </article>

          <article className="contact__item">
            <span className="contact__icon">
              <MailIcon />
            </span>
            <h3 className="contact__name">이메일 문의</h3>
            <p className="contact__meta">
              문의를 남겨주시면
              <br />
              영업일 기준 1~2일 내 답변 드립니다
            </p>
            <a className="contact__action" href={`mailto:${companyInfo.email}`}>
              {companyInfo.email}
            </a>
          </article>

          <article className="contact__item">
            <span className="contact__icon">
              <DocIcon />
            </span>
            <h3 className="contact__name">1:1 문의</h3>
            <p className="contact__meta">
              문의를 남겨주시면
              <br />
              영업일 기준 1~2일 내 답변 드립니다
            </p>
            <button className="contact__action contact__action--btn" onClick={() => navigate("/support#contact")}>
              1:1 문의하기 <ChevronRight width={15} height={15} />
            </button>
          </article>

          <article className="contact__item">
            <span className="contact__icon">
              <ChatIcon />
            </span>
            <h3 className="contact__name">카카오톡 문의</h3>
            <p className="contact__meta">
              {companyInfo.businessHours}
              <br />({companyInfo.lunchHours})
            </p>
            <a
              className="contact__action"
              href="https://pf.kakao.com/"
              target="_blank"
              rel="noreferrer noopener"
            >
              한글과 세종 <ArrowUpRight width={14} height={14} />
            </a>
          </article>
        </div>
      </div>
    </section>
  );
}
