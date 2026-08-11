import { Link } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../features/auth/AuthContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../components/admin/ContentAdminPanel";
import "./ContentPages.css";
import "./EventsPage.css";

const programs = [
  { tag: "문화 체험", title: "한국 이름 만들기", text: "참가자의 정보를 바탕으로 한국 이름을 제안하고 이름에 담긴 뜻과 이야기를 소개합니다." },
  { tag: "기관·단체", title: "맞춤형 카드 제작", text: "행사 성격과 기관의 목적에 맞춰 명예한국인증·방문증 등 카드 콘텐츠를 구성합니다." },
  { tag: "현장 운영", title: "행사 연계 프로그램", text: "관광·교육·교류 행사 안에서 이름 체험과 카드 수령이 자연스럽게 이어지도록 설계합니다." },
];

const process = ["상담 및 목적 확인", "프로그램 구성", "참가자 정보 접수", "이름·카드 제작", "현장 운영 및 전달"];

const boothDetails = [
  { image: "/images/events/booth-calligraphy.webp", title: "한국 이름 작명 상담", text: "참가자의 이야기를 듣고 이름의 소리와 의미를 함께 살펴보는 맞춤형 상담을 진행합니다." },
  { image: "/images/events/booth-card-delivery.webp", title: "현장 카드 전달", text: "완성된 한국 이름과 카드의 의미를 설명하며 참가자에게 직접 전달합니다." },
  { image: "/images/events/booth-display.webp", title: "전시와 체험 동선", text: "카드 견본과 전통 소재를 활용해 대기부터 수령까지 자연스럽게 이어지는 공간을 구성합니다." },
];

const collaborations = [
  { image: "/images/events/collaboration-1.webp", mark: "H", title: "호텔·리조트", text: "체크인 경험과 연계한 한국 이름 웰컴 카드로 특별한 첫인상을 만듭니다." },
  { image: "/images/events/collaboration-2.webp", mark: "U", title: "대학·교육기관", text: "외국인 학생 오리엔테이션과 교류 행사에 맞춘 학생증형 콘텐츠를 제공합니다." },
  { image: "/images/events/collaboration-3.webp", mark: "S", title: "스포츠·선수단", text: "국제대회 참가자와 선수단을 위한 기념 카드와 문화 체험을 운영합니다." },
  { image: "/images/events/collaboration-4.webp", mark: "T", title: "관광·여행", text: "관광 안내와 지역 방문 경험을 한국 이름 방문증으로 오래 남깁니다." },
  { image: "/images/events/collaboration-5.webp", mark: "M", title: "박물관·문화기관", text: "전시와 교육 프로그램에 한글 작명과 카드 제작 체험을 결합합니다." },
  { image: "/images/events/collaboration-6.webp", mark: "B", title: "뷰티·라이프스타일", text: "브랜드 팝업의 감도에 맞춘 이름 카드와 패키지형 기념품을 제안합니다." },
  { image: "/images/events/collaboration-7.webp", mark: "C", title: "기업·컨퍼런스", text: "글로벌 임직원과 초청 고객을 위한 한국 문화 네트워킹 프로그램을 구성합니다." },
  { image: "/images/events/collaboration-8.webp", mark: "F", title: "지역축제·지자체", text: "지역의 이야기와 발행 주체를 담은 맞춤 카드로 축제 참여 경험을 확장합니다." },
];

export function EventsPage() {
  const { isAdmin } = useAuth();
  const defaults: ManagedContent[] = programs.map((program, index) => ({ id: `event-${index}`, title: program.title, content: program.text, meta: program.tag }));
  const [managedPrograms, setManagedPrograms] = useState(() => loadManagedContent("events", defaults));
  const updatePrograms = (items: ManagedContent[]) => { localStorage.setItem("managed-content:events", JSON.stringify(items)); setManagedPrograms(items); };
  return (
    <div className="content-page events-page">
      <header className="subpage-hero page-container">
        <p className="eyebrow">행사사업</p>
        <h1 className="subpage-hero__title">한글로 연결되는 문화 행사</h1>
        <p className="section-lead">기관과 행사의 목적에 맞는 한국 이름·카드 체험 프로그램을 제안합니다.</p>
      </header>

      <section className="event-programs page-container">
        {isAdmin && <ContentAdminPanel label="이벤트" items={managedPrograms} onChange={updatePrograms} />}
        <div className="content-section-head">
          <p className="content-kicker">PROGRAM</p>
          <h2>행사에 맞춰 다양하게 구성합니다.</h2>
        </div>
        <div className="program-grid">
          {managedPrograms.map((program, index) => (
            <article className="program-card" key={program.title}>
              <span>{program.meta}</span>
              <b>{String(index + 1).padStart(2, "0")}</b>
              <h3>{program.title}</h3>
              <p>{program.content}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="event-booth page-container">
        <div className="content-section-head event-feature__head">
          <p className="content-kicker">BOOTH OPERATION</p>
          <h2>현장에서 완성되는 한국 이름 체험</h2>
          <p>행사의 규모와 공간에 맞춰 상담, 작명, 카드 제작과 전달이 하나의 흐름으로 이어지는 부스를 운영합니다.</p>
        </div>
        <figure className="event-booth__hero">
          <img src="/images/events/booth-hero.webp" alt="국제 문화 행사에서 운영 중인 한옥형 한국 이름 체험 부스" />
          <figcaption><strong>공간 기획부터 현장 운영까지</strong><span>브랜드의 성격과 방문객 동선을 고려해 따뜻하고 정돈된 체험 환경을 만듭니다.</span></figcaption>
        </figure>
        <div className="event-booth__details">
          {boothDetails.map((item) => <article key={item.title}><img src={item.image} alt={item.title} loading="lazy" /><h3>{item.title}</h3><p>{item.text}</p></article>)}
        </div>
      </section>

      <section className="event-collaboration">
        <div className="page-container">
          <div className="content-section-head event-feature__head">
            <p className="content-kicker">BRAND COLLABORATION</p>
            <h2>브랜드의 경험에 한글의 이야기를 더합니다</h2>
            <p>기업과 기관의 아이덴티티를 존중하면서 한국 이름과 카드가 자연스럽게 연결되는 협업 프로그램을 설계합니다.</p>
          </div>
          <div className="event-collaboration__grid">
            {collaborations.map((item) => (
              <article className="collaboration-card" key={item.title}>
                <img src={item.image} alt={`${item.title} 브랜드 협업 예시`} loading="lazy" />
                <div className="collaboration-card__copy"><span className="collaboration-card__mark" aria-hidden="true">{item.mark}</span><div><h3>{item.title}</h3><p>{item.text}</p></div></div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="content-band">
        <div className="page-container">
          <div className="content-section-head">
            <p className="content-kicker">PROCESS</p>
            <h2>행사 진행 과정</h2>
          </div>
          <ol className="process-list">
            {process.map((item, index) => (
              <li key={item}><span>{String(index + 1).padStart(2, "0")}</span><p>{item}</p></li>
            ))}
          </ol>
        </div>
      </section>

      <section className="event-inquiry page-container">
        <div>
          <p className="content-kicker">PARTNERSHIP</p>
          <h2>기관·단체 행사를 준비하고 계신가요?</h2>
          <p>행사 일정과 예상 인원, 필요한 카드 유형을 알려주시면 적합한 운영 방식을 안내해 드립니다.</p>
        </div>
        <Link to="/support#contact">행사 상담 문의　→</Link>
      </section>
    </div>
  );
}
