import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { companyInfo } from "../config/company";
import { PhoneIcon, MailIcon, DocIcon, ChatIcon, ArrowUpRight } from "../components/ui/icons";
import { Modal } from "../components/ui/Modal";
import "./SupportPage.css";

const notices = [
  { title: "단체 제작 신청 안내", date: "2027.02.05" },
  { title: "신청양식 다운로드", date: "2027.02.05" },
  { title: "모바일·실물 카드 수령 안내", date: "2027.01.26" },
  { title: "공식 서비스 및 운영 안내", date: "2027.01.08" },
];

const faqs = [
  { q: "제작된 카드는 실제 신분증으로 사용할 수 있나요?", a: "아니요.\n본 상품은 신분증으로서의 법적 효력을 갖지 않습니다." },
  { q: "작명 의뢰는 각 개인이 직접 하여야 하나요?", a: "개인 신청과 단체 신청 모두 가능하며, 신청 유형에 맞는 정보를 입력해 주시면 됩니다." },
  { q: "의뢰 후 제작 기간은 어느정도 걸리나요?", a: "상담과 자료 확인이 완료된 뒤 제작 일정과 수령 방법을 개별 안내드립니다." },
  { q: "전국 기관을 대상으로 업무가 가능한가요?", a: "네. 전국 기관 및 단체를 대상으로 상담과 제작 업무를 진행할 수 있습니다." },
  { q: "소량 제작도 가능한가요?", a: "가능합니다. 수량과 제작 사양에 따라 상세 견적을 안내드립니다." },
  { q: "신규 발급 뿐 아니라 재발급도 가능한가요?", a: "가능합니다. 기존 신청 정보 확인 후 재발급 절차를 안내드립니다." },
  { q: "카드 발급에 필요한 자료는 무엇인가요?", a: "카드 유형에 따라 신청자 정보와 사진 등 필요한 자료가 달라집니다. 제작 신청 화면에서 확인해 주세요." },
];

const stories = [
  { title: "한국 이름을 지어줄 때\n한글로 된 이름을\n지어주나요?", body: "한국 이름의 소리와 뜻을 함께 고려해 한글 이름을 제안합니다." },
  { title: "한국 이름은 한글로\n작명하는 경우가 대부분인데\n어떻게 작명하나요?", body: "이름에 담길 뜻과 발음, 사용 환경을 종합해 작명 방향을 정합니다." },
  { title: "한국 이름 지어주기는\n어떻게 어떤 방식으로\n지어주나요?", body: "신청 정보를 바탕으로 이름 후보와 풀이를 구성해 전달합니다." },
  { title: "한글 이름에도\n사주 오행이 있나요?", body: "요청한 서비스 유형에 따라 관련 설명과 이름 풀이를 함께 제공합니다." },
];

export function SupportPage() {
  const { hash } = useLocation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [searchBy, setSearchBy] = useState("전체");
  const [storyIndex, setStoryIndex] = useState<number | null>(null);

  useEffect(() => {
    const id = hash.replace("#", "");
    if (!id) return;
    requestAnimationFrame(() => {
      document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }, [hash]);

  const filteredNotices = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return notices;
    return notices.filter((notice) => {
      const target = searchBy === "작성일" ? notice.date : notice.title;
      return target.toLowerCase().includes(keyword);
    });
  }, [query, searchBy]);

  return (
    <div className="support">
      <header className="support__hero subpage-hero page-container">
        <p className="eyebrow">고객지원</p>
        <h1 className="support__title subpage-hero__title">고객지원</h1>
      </header>

      <section id="notice" className="support__section page-container">
        <SectionRule />
        <h2 className="support__heading">공지사항</h2>
        <form className="notice-search" onSubmit={(e) => e.preventDefault()}>
          <select value={searchBy} onChange={(e) => setSearchBy(e.target.value)} aria-label="검색 조건">
            <option>전체</option>
            <option>제목</option>
            <option>작성일</option>
          </select>
          <label>
            <span className="visually-hidden">검색어 입력</span>
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="검색어를 입력하세요" />
            <button type="submit" aria-label="검색">
              <SearchGlyph />
            </button>
          </label>
        </form>

        <div className="notice-table">
          <div className="notice-table__head">
            <span>번호</span><span>제목</span><span>작성일</span>
          </div>
          {filteredNotices.map((notice) => (
            <article className="notice-table__row" key={notice.title}>
              <span className="notice-table__badge">공지</span>
              <span className="notice-table__title">{notice.title}</span>
              <time>{notice.date}</time>
            </article>
          ))}
          {filteredNotices.length === 0 && <p className="notice-table__empty">검색 결과가 없습니다.</p>}
        </div>
        <nav className="support-pagination" aria-label="공지사항 페이지">
          <button aria-label="이전 페이지">‹</button><b>1</b><button aria-label="다음 페이지">›</button>
        </nav>
      </section>

      <section id="faq" className="support__section page-container">
        <SectionRule />
        <h2 className="support__heading support__heading--plain">자주 묻는 질문</h2>
        <p className="faq__intro">
          자주 문의하시는 내용을 정리했습니다.<br />
          원하시는 내용을 찾지 못하신 경우 상담 문의를 이용해 주세요.
        </p>
        <div className="faq">
          {faqs.map((faq, index) => (
            <details key={faq.q} className="faq__item" open={index === 0 ? true : undefined}>
              <summary className="faq__q"><b>Q.</b><span>{faq.q}</span></summary>
              <p className="faq__a"><b>A.</b><span>{faq.a}</span></p>
            </details>
          ))}
        </div>
        <div className="faq-help">
          <span className="faq-help__icon"><PhoneIcon /></span>
          <p>상담 문의 {companyInfo.phone}　 |　 {companyInfo.businessHours}<br />고객지원에서 다양한 상담 방법을 이용하실 수 있습니다.</p>
          <button onClick={() => navigate("/support#contact")}>고객 지원</button>
        </div>
      </section>

      <section id="story" className="support__section page-container">
        <SectionRule />
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
        <SectionRule />
        <h2 className="support__heading">상담·문의</h2>
        <div className="support__contact">
          <ContactCard icon={<PhoneIcon />} title="전화 상담" lines={[companyInfo.businessHours, `(${companyInfo.lunchHours})`]}>
            <a href={`tel:${companyInfo.phone.replace(/\D/g, "")}`} className="support__link">{companyInfo.phone}</a>
          </ContactCard>
          <ContactCard icon={<MailIcon />} title="이메일 문의" lines={["문의를 남겨주시면", "영업일 기준 1~2일 내 답변 드립니다"]}>
            <a href={`mailto:${companyInfo.email}`} className="support__link">{companyInfo.email}</a>
          </ContactCard>
          <ContactCard icon={<DocIcon />} title="1:1 문의" lines={["문의를 남겨주시면", "영업일 기준 1~2일 내 답변 드립니다"]}>
            <button className="support__link">1:1 문의하기　›</button>
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
      >
        <p>{storyIndex === null ? "" : stories[storyIndex].body}</p>
      </Modal>
    </div>
  );
}

function SectionRule() {
  return <div className="support-rule" aria-hidden="true"><i /></div>;
}

function SearchGlyph() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>;
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
