// Customer-support page with anchor-scrolled sections.
import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { companyInfo } from "../config/company";
import { PhoneIcon, MailIcon, ChatIcon, ArrowUpRight } from "../components/ui/icons";
import "./SupportPage.css";

const faqs = [
  {
    q: "제작 신청은 어떻게 진행되나요?",
    a: "디자인 페이지에서 원하는 카드의 ‘제작 신청’을 누르면 유형 선택부터 신청 완료까지 5단계로 진행됩니다.",
  },
  {
    q: "개인과 단체 신청의 차이는 무엇인가요?",
    a: "개인 신청은 한 명의 정보를 입력하며, 단체 신청은 명단 파일(ZIP)을 업로드하여 여러 건을 한 번에 신청합니다.",
  },
  {
    q: "모바일 발급과 실물 발급은 어떻게 다른가요?",
    a: "모바일 발급은 디지털 카드만 제공하며, 실물 발급을 선택하면 배송을 위한 수령인 정보를 추가로 입력합니다.",
  },
  {
    q: "신청 내역은 어디서 확인하나요?",
    a: "상단 ‘조회’ 메뉴에서 카드번호·신청번호와 연락처 등으로 신청 상태를 확인할 수 있습니다.",
  },
];

const notices = [
  { date: "2026.07.10", title: "명예한국인증 신규 디자인이 추가되었습니다." },
  { date: "2026.06.28", title: "여름철 배송 일정 안내." },
  { date: "2026.06.01", title: "한글과 세종 홈페이지가 새롭게 단장했습니다." },
];

export function SupportPage() {
  const { hash } = useLocation();

  // Scroll to the target section once the page is rendered.
  useEffect(() => {
    const id = hash.replace("#", "");
    if (!id) return;
    requestAnimationFrame(() => {
      document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }, [hash]);

  return (
    <div className="support">
      <div className="support__hero page-container">
        <p className="eyebrow">고객지원</p>
        <h1 className="support__title">무엇을 도와드릴까요?</h1>
        <p className="section-lead">공지사항부터 상담 문의까지, 한글과 세종의 고객지원 안내입니다.</p>
      </div>

      <section id="notice" className="support__section page-container">
        <h2 className="support__heading">공지사항</h2>
        <ul className="notice-list">
          {notices.map((n) => (
            <li key={n.title} className="notice-list__item">
              <span className="notice-list__date">{n.date}</span>
              <span className="notice-list__title">{n.title}</span>
            </li>
          ))}
        </ul>
      </section>

      <section id="faq" className="support__section page-container">
        <h2 className="support__heading">자주 묻는 질문</h2>
        <div className="faq">
          {faqs.map((f) => (
            <details key={f.q} className="faq__item">
              <summary className="faq__q">{f.q}</summary>
              <p className="faq__a">{f.a}</p>
            </details>
          ))}
        </div>
      </section>

      <section id="story" className="support__section page-container">
        <h2 className="support__heading">제작 이야기</h2>
        <p className="section-lead support__story">
          한글 오행을 바탕으로 외국인을 위한 한국 이름을 짓고, 명예한국인증·명예시민증·학생증·방문증으로
          이어지는 제작 과정을 소개합니다. 상세 콘텐츠는 준비되는 대로 업데이트됩니다.
        </p>
      </section>

      <section id="contact" className="support__section page-container">
        <h2 className="support__heading">상담 문의</h2>
        <div className="support__contact">
          <ContactCard icon={<PhoneIcon />} title="전화 상담" lines={[companyInfo.businessHours, `(${companyInfo.lunchHours})`]}>
            <a href={`tel:${companyInfo.phone.replace(/\D/g, "")}`} className="support__link">
              {companyInfo.phone}
            </a>
          </ContactCard>
          <ContactCard icon={<MailIcon />} title="이메일 문의" lines={["문의를 남겨주시면", "영업일 기준 1~2일 내 답변 드립니다"]}>
            <a href={`mailto:${companyInfo.email}`} className="support__link">
              {companyInfo.email}
            </a>
          </ContactCard>
          <ContactCard icon={<ChatIcon />} title="카카오톡 문의" lines={[companyInfo.businessHours, `(${companyInfo.lunchHours})`]}>
            <a href="https://pf.kakao.com/" target="_blank" rel="noreferrer noopener" className="support__link">
              한글과 세종 <ArrowUpRight width={14} height={14} />
            </a>
          </ContactCard>
        </div>
      </section>
    </div>
  );
}

function ContactCard({
  icon,
  title,
  lines,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  lines: string[];
  children: React.ReactNode;
}) {
  return (
    <article className="ccard">
      <span className="ccard__icon">{icon}</span>
      <h3 className="ccard__title">{title}</h3>
      {lines.map((l) => (
        <p key={l} className="ccard__line">
          {l}
        </p>
      ))}
      <div className="ccard__action">{children}</div>
    </article>
  );
}
