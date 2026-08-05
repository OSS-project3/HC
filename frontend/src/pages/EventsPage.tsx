import { Link } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../features/auth/AuthContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../components/admin/ContentAdminPanel";
import "./ContentPages.css";

const programs = [
  { tag: "문화 체험", title: "한국 이름 만들기", text: "참가자의 정보를 바탕으로 한국 이름을 제안하고 이름에 담긴 뜻과 이야기를 소개합니다." },
  { tag: "기관·단체", title: "맞춤형 카드 제작", text: "행사 성격과 기관의 목적에 맞춰 명예한국인증·방문증 등 카드 콘텐츠를 구성합니다." },
  { tag: "현장 운영", title: "행사 연계 프로그램", text: "관광·교육·교류 행사 안에서 이름 체험과 카드 수령이 자연스럽게 이어지도록 설계합니다." },
];

const process = ["상담 및 목적 확인", "프로그램 구성", "참가자 정보 접수", "이름·카드 제작", "현장 운영 및 전달"];

export function EventsPage() {
  const { isAdmin } = useAuth();
  const defaults: ManagedContent[] = programs.map((program, index) => ({ id: `event-${index}`, title: program.title, content: program.text, meta: program.tag }));
  const [managedPrograms, setManagedPrograms] = useState(() => loadManagedContent("events", defaults));
  const updatePrograms = (items: ManagedContent[]) => { localStorage.setItem("managed-content:events", JSON.stringify(items)); setManagedPrograms(items); };
  return (
    <div className="content-page">
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
