import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { companyInfo } from "../../config/company";
import { PhoneIcon, MailIcon, DocIcon, ChatIcon, ArrowUpRight } from "../../components/ui/icons";
import { Modal } from "../../components/ui/Modal";
import "./SupportPage.css";

export const notices = [
  { id: "group-application", title: "단체 제작 신청 안내", date: "2027.02.05", content: "단체 카드 제작을 신청하실 경우 제작 신청 페이지에서 법인·단체 신청을 선택해 주세요. 담당자 확인 후 제작 일정과 필요한 자료를 안내드립니다.", attachment: "단체_카드_신청안내.txt" },
  { id: "application-form", title: "신청양식 다운로드", date: "2027.02.05", content: "단체 카드(명예 한국인증, 명예 시민증, 학생증, 방문증) 신청 시 필요한 신청양식입니다.\n아래 첨부파일을 다운로드하여 작성 후 신청 시 함께 첨부해 주세요.", attachment: "단체_카드_신청양식.txt" },
  { id: "card-delivery", title: "모바일·실물 카드 수령 안내", date: "2027.01.26", content: "모바일 카드는 발급 완료 후 신청 조회 페이지에서 확인할 수 있습니다. 실물 카드를 함께 신청하신 경우 등록한 수령지로 순차 배송됩니다.", attachment: "카드_수령_안내.txt" },
  { id: "service-guide", title: "공식 서비스 및 운영 안내", date: "2027.01.08", content: "한글과 세종 공식 서비스 운영 안내입니다. 서비스 이용과 제작 신청에 관한 문의는 고객지원 상담·문의 메뉴를 이용해 주세요.", attachment: "서비스_운영_안내.txt" },
];

export const faqs = [
  { q: "제작된 카드는 실제 신분증으로 사용할 수 있나요?", a: "아니요.\n본 상품은 신분증으로서의 법적 효력을 갖지 않습니다." },
  { q: "작명 의뢰는 각 개인이 직접 하여야 하나요?", a: "개인 신청과 단체 신청 모두 가능하며, 신청 유형에 맞는 정보를 입력해 주시면 됩니다." },
  { q: "의뢰 후 제작 기간은 어느정도 걸리나요?", a: "상담과 자료 확인이 완료된 뒤 제작 일정과 수령 방법을 개별 안내드립니다." },
  { q: "전국 기관을 대상으로 업무가 가능한가요?", a: "네. 전국 기관 및 단체를 대상으로 상담과 제작 업무를 진행할 수 있습니다." },
  { q: "소량 제작도 가능한가요?", a: "가능합니다. 수량과 제작 사양에 따라 상세 견적을 안내드립니다." },
  { q: "신규 발급 뿐 아니라 재발급도 가능한가요?", a: "가능합니다. 기존 신청 정보 확인 후 재발급 절차를 안내드립니다." },
  { q: "카드 발급에 필요한 자료는 무엇인가요?", a: "카드 유형에 따라 신청자 정보와 사진 등 필요한 자료가 달라집니다. 제작 신청 화면에서 확인해 주세요." },
];

const stories = [
  {
    title: "한국 이름을 지어줄 때\n한글로 된 이름을 지어주나요?",
    body: "예! 그렇습니다. 한문으로 해석된 이름과 순우리말 이름도 지어줍니다.",
  },
  {
    title: "한국 이름은 한문으로\n작명하는 경우가 대부분인데\n어떻게 작명하나요?",
    body: "한국 이름은 순우리말 이름이 있고 한문 이름도 한글로 된 한문 이름으로 작명하여 뜻과 의미를 해석하여 공급합니다. 한국, 중국, 일본 등은 한자 문화권으로 한문 이름이 많습니다. 한문 이름으로 작명하여도 한글로 해석하여 제공합니다.",
  },
  {
    title: "한국 이름 지어주기는\n어떻게, 어떤 방식으로\n지어주나요?",
    body: "순우리말과 한국식 이름을 작명하며, 뜻과 의미가 있는 이름을 지어줍니다. 사람마다 태어난 년·월·일·시가 제각각이고 태어난 나라와 도시가 다르기 때문에 사람마다 사주 오행을 분석하여 동양철학 전문가가 이름을 지어줍니다. 이름의 뜻과 의미도 함께 설명하여 줍니다.",
  },
  {
    title: "한글 이름에도\n사주 오행이 있나요?",
    body: "그렇습니다. 한문 이름에도 사주 오행이 있으며, 한글에도 사주 오행이 있습니다. 한글의 자음을 각기 다른 오행으로 분류합니다.",
  },
];

export function SupportPage() {
  const { hash } = useLocation();
  const navigate = useNavigate();
  const [storyIndex, setStoryIndex] = useState<number | null>(null);

  useEffect(() => {
    const id = hash.replace("#", "");
    if (!id) return;
    requestAnimationFrame(() => {
      document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }, [hash]);

  return (
    <div className="support">
      <header className="support__hero subpage-hero page-container">
        <div className="support__hero-copy">
          <p className="eyebrow">고객지원</p>
          <h1 className="support__title subpage-hero__title">고객지원</h1>
        </div>
        <img className="support__hero-art" src="/images/support/support-bg.png" alt="" aria-hidden="true" />
      </header>

      <section id="notice" className="support__section page-container">
        <div className="notice-summary__head">
          <h2>공지사항</h2>
          <button onClick={() => navigate("/notices")}>더보기 <span aria-hidden="true">›</span></button>
        </div>
        <div className="notice-table">
          {notices.slice(0, 4).map((notice) => (
            <article className="notice-table__row" key={notice.title}>
              <span className="notice-table__badge">공지</span>
              <Link className="notice-table__title" to={`/notices/${notice.id}`}>{notice.title}</Link>
              <time>{notice.date}</time>
            </article>
          ))}
        </div>
      </section>

      <section id="faq" className="support__section page-container">
        <SectionRule plain />
        <div className="notice-summary__head">
          <h2>자주 묻는 질문</h2>
          <button onClick={() => navigate("/faq")}>더보기 <span aria-hidden="true">›</span></button>
        </div>
        <div className="faq">
          {faqs.map((faq, index) => (
            <details key={faq.q} className="faq__item" open={index === 0 ? true : undefined}>
              <summary className="faq__q"><b>Q.</b><span>{faq.q}</span></summary>
              <p className="faq__a"><b>A.</b><span>{faq.a}</span></p>
            </details>
          ))}
        </div>
      </section>

      <section id="story" className="support__section page-container">
        <SectionRule plain />
        <h2 className="support__heading">제작 이야기</h2>
        <div className="story-grid">
          {stories.map((story, index) => (
            <button className="story-card" key={story.title} onClick={() => setStoryIndex(index)}>
              <span>{story.title}</span><b aria-hidden="true">+</b>
            </button>
          ))}
        </div>
      </section>

      <section id="contact" className="support__section support__section--contact page-container">
        <SectionRule plain />
        <h2 className="support__heading">상담·문의</h2>
        <div className="support__contact">
          <ContactCard icon={<PhoneIcon />} title="전화 상담" lines={[companyInfo.businessHours, `(${companyInfo.lunchHours})`]}>
            <a href={`tel:${companyInfo.phone.replace(/\D/g, "")}`} className="support__link">{companyInfo.phone}</a>
          </ContactCard>
          <ContactCard icon={<MailIcon />} title="이메일 문의" lines={["문의를 남겨주시면", "영업일 기준 1~2일 내 답변 드립니다"]}>
            <a href={`mailto:${companyInfo.email}`} className="support__link">{companyInfo.email}</a>
          </ContactCard>
          <ContactCard icon={<DocIcon />} title="1:1 문의" lines={["문의를 남겨주시면", "영업일 기준 1~2일 내 답변 드립니다"]}>
            <button className="support__link" onClick={() => navigate("/inquiry")}>1:1 문의하기　›</button>
          </ContactCard>
          <ContactCard icon={<ChatIcon />} title="카카오톡 문의" lines={[companyInfo.businessHours, `(${companyInfo.lunchHours})`]}>
            <a href="https://pf.kakao.com/" target="_blank" rel="noreferrer noopener" className="support__link">
              한글과 세종 <ArrowUpRight width={14} height={14} />
            </a>
          </ContactCard>
        </div>
      </section>

      <Modal
        open={storyIndex !== null}
        onClose={() => setStoryIndex(null)}
        title={storyIndex === null ? "제작 이야기" : stories[storyIndex].title.replace(/\n/g, " ")}
        className="story-modal"
      >
        <p>{storyIndex === null ? "" : stories[storyIndex].body}</p>
      </Modal>
    </div>
  );
}

function SectionRule({ plain = false }: { plain?: boolean }) {
  return <div className={`support-rule${plain ? " support-rule--plain" : ""}`} aria-hidden="true"><i /></div>;
}

function ContactCard({ icon, title, lines, children }: { icon: React.ReactNode; title: string; lines: string[]; children: React.ReactNode }) {
  return (
    <article className="ccard">
      <span className="ccard__icon">{icon}</span>
      <h3 className="ccard__title">{title}</h3>
      {lines.map((line) => <p key={line} className="ccard__line">{line}</p>)}
      <div className="ccard__action">{children}</div>
    </article>
  );
}
