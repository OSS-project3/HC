import { Link } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../../features/auth/AuthContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../../components/admin/ContentAdminPanel";
import { ImagePlaceholder } from "../../components/ui/ImagePlaceholder";
import { Modal } from "../../components/ui/Modal";
import "../../styles/ContentPages.css";
import "./EventsPage.css";

const programs = [
  { tag: "문화 체험", title: "한국 이름 만들기", text: "참가자의 정보를 바탕으로 한국 이름을 제안하고 이름에 담긴 뜻과 이야기를 소개합니다." },
  { tag: "기관·단체", title: "맞춤형 카드 제작", text: "행사 성격과 기관의 목적에 맞춰 명예한국인증·방문증 등 카드 콘텐츠를 구성합니다." },
  { tag: "현장 운영", title: "행사 연계 프로그램", text: "관광·교육·교류 행사 안에서 이름 체험과 카드 수령이 자연스럽게 이어지도록 설계합니다." },
];

const process = ["상담 및 목적 확인", "참가자 정보 접수", "이름·카드 제작", "현장 운영 및 전달"];

interface FeedPost {
  date: string;
  title: string;
  place: string;
  host: string;
  cardLabel: string;
  text: string;
  image?: string;
}

// 부스 운영 — 실제 부스를 운영한 기록을 게시글처럼 올린다.
// 가장 최근 행사가 맨 위(크게), 지난 행사는 아래로 순서대로.
// 본문 문구는 동일한 틀을 쓰고, 행사명·일자·장소·카드 종류만 바뀐다.
const BOOTH_TEXT =
  "부스를 찾은 방문객에게 한글 오행으로 지은 한국 이름과 카드를 현장에서 제작해 전달했습니다. 참가자와 함께한 인증 사진과 현장 후기를 이곳에 기록으로 남깁니다.";
const boothPosts: FeedPost[] = [
  { date: "2026. 12", title: "서울공예트렌드페어", place: "서울 코엑스 Hall C", host: "(재)한국공예·디자인문화진흥원", cardLabel: "명예한국인증 · 방문증", text: BOOTH_TEXT, image: "/images/events/booth-hero.webp" },
  { date: "2026. 10", title: "한국전통문화박람회", place: "경주 화백컨벤션센터", host: "문화체육관광부", cardLabel: "명예한국인증 · 학생증", text: BOOTH_TEXT, image: "/images/events/booth-calligraphy.webp" },
  { date: "2026. 08", title: "한글주간 문화행사", place: "국립한글박물관", host: "국립한글박물관", cardLabel: "방문증", text: BOOTH_TEXT, image: "/images/events/booth-display.webp" },
  { date: "2026. 06", title: "부산국제관광전", place: "부산 벡스코", host: "부산광역시", cardLabel: "방문증", text: BOOTH_TEXT, image: "/images/events/booth-card-delivery.webp" },
];
const BOOTH_GALLERY = [
  "/images/events/booth-hero.webp",
  "/images/events/booth-calligraphy.webp",
  "/images/events/booth-display.webp",
  "/images/events/booth-card-delivery.webp",
];

// 법인·단체 협업 — 해외 선수단·단체에 한국 이름과 카드를 발급하고 협업 사실을 기록한다.
const COLLAB_TEXT =
  "해외 참가자에게 한글 이름을 지어 방문증·명예한국인증을 발급하고, 함께한 인증 사진으로 협업을 기록합니다. 발급한 카드와 참여 인원은 협업 단위로 관리됩니다.";
const collabPosts: FeedPost[] = [
  { date: "2026. 11", title: "국제 태권도 협회 초청 선수단", place: "○○ 태권도 협회", host: "세계태권도연맹", cardLabel: "명예한국인증 · 방문증", text: COLLAB_TEXT, image: "/images/events/collaboration-3.webp" },
  { date: "2026. 09", title: "해외 대학 교류 사절단", place: "△△ 대학교", host: "△△ 대학교 국제교류처", cardLabel: "학생증", text: COLLAB_TEXT, image: "/images/events/collaboration-2.webp" },
  { date: "2026. 07", title: "글로벌 기업 임직원 초청", place: "□□ 컨퍼런스", host: "□□ 그룹", cardLabel: "명예시민증", text: COLLAB_TEXT, image: "/images/events/collaboration-7.webp" },
  { date: "2026. 05", title: "국제 문화교류 사절단", place: "◇◇ 문화교류협회", host: "◇◇ 문화교류협회", cardLabel: "명예한국인증", text: COLLAB_TEXT, image: "/images/events/collaboration-5.webp" },
];
const COLLAB_GALLERY = [
  "/images/events/collaboration-1.webp",
  "/images/events/collaboration-2.webp",
  "/images/events/collaboration-3.webp",
  "/images/events/collaboration-5.webp",
  "/images/events/collaboration-7.webp",
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

      <EventFeed
        title="부스 운영"
        tagline="현장에서 고객과 직접 만나 정성을 담은 서비스를 제공합니다"
        posts={boothPosts}
        gallery={BOOTH_GALLERY}
      />

      <EventFeed
        title="법인·단체 협업"
        tagline="현장에서 고객과 직접 만나 정성을 담은 서비스를 제공합니다"
        posts={collabPosts}
        gallery={COLLAB_GALLERY}
      />

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

/** A single record card (photo on top, content below). */
function EventCard({ post, onOpen, wide = false }: { post: FeedPost; onOpen: (post: FeedPost) => void; wide?: boolean }) {
  return (
    <article className={`event-card${wide ? " event-card--wide" : ""}`}>
      <div className="event-card__media">
        {post.image
          ? <img src={post.image} alt={`${post.title} 사진`} loading="lazy" />
          : <ImagePlaceholder label={`${post.title} 사진`} />}
      </div>
      <div className="event-card__body">
        <h3>{post.title}</h3>
        <div className="event-card__meta">
          <time>{post.date}</time>
          <span className="event-card__label">{post.cardLabel}</span>
        </div>
        <p className={`event-card__text${wide ? " event-card__text--full" : ""}`}>{post.text}</p>
        <button type="button" className="event-card__more" onClick={() => onOpen(post)}>
          자세히 보기<span aria-hidden="true"> →</span>
        </button>
      </div>
    </article>
  );
}

/**
 * 부스 운영 / 법인·단체 협업 공통 피드.
 * 대표 기록 1개를 상단에 사진 60% / 글 40% 와이드 카드로, 나머지는 한 행에 3개 카드로 배열한다.
 * 카드 하단의 "자세히 보기"를 누르면 상세 팝업이 뜬다.
 */
function EventFeed({ title, tagline, posts, gallery }: {
  title: string;
  tagline: string;
  posts: FeedPost[];
  gallery: string[];
}) {
  const [active, setActive] = useState<FeedPost | null>(null);
  const [featured, ...rest] = posts;

  return (
    <section className="event-feed page-container">
      <header className="event-feed__head">
        <h2 className="event-feed__title">{title}</h2>
        <span className="event-feed__rule" aria-hidden="true" />
        <p className="event-feed__tagline">{tagline}</p>
      </header>

      {featured && <EventCard post={featured} onOpen={setActive} wide />}

      <div className="event-card-grid">
        {rest.map((post) => <EventCard key={post.title} post={post} onOpen={setActive} />)}
      </div>

      <Modal open={active !== null} onClose={() => setActive(null)} title={active?.title ?? ""} className="event-modal">
        {active && <EventDetail key={active.title} post={active} sectionLabel={title} gallery={gallery} />}
      </Modal>
    </section>
  );
}

/** Detail popup content: gallery (main image + thumbnails) left, info right. */
function EventDetail({ post, sectionLabel, gallery }: { post: FeedPost; sectionLabel: string; gallery: string[] }) {
  const images = [post.image, ...gallery].filter((src, i, arr): src is string => Boolean(src) && arr.indexOf(src) === i);
  const [index, setIndex] = useState(0);
  const current = images[index];
  const move = (delta: number) => setIndex((i) => (i + delta + images.length) % images.length);

  return (
    <div className="event-modal__grid">
      <div className="event-modal__gallery">
        <div className="event-modal__main">
          {current
            ? <img src={current} alt={`${post.title} 사진 ${index + 1}`} />
            : <ImagePlaceholder label={`${post.title} 사진`} />}
        </div>
        {images.length > 1 && (
          <div className="event-modal__thumbs">
            <button type="button" className="event-modal__arrow" onClick={() => move(-1)} aria-label="이전 사진">‹</button>
            <ul>
              {images.map((src, i) => (
                <li key={src}>
                  <button
                    type="button"
                    className={`event-modal__thumb${i === index ? " is-active" : ""}`}
                    onClick={() => setIndex(i)}
                    aria-label={`${i + 1}번째 사진`}
                    aria-current={i === index}
                  >
                    <img src={src} alt="" />
                  </button>
                </li>
              ))}
            </ul>
            <button type="button" className="event-modal__arrow" onClick={() => move(1)} aria-label="다음 사진">›</button>
          </div>
        )}
      </div>
      <div className="event-modal__info">
        <p className="event-modal__kicker">{sectionLabel}</p>
        <h2 className="event-modal__title">{post.title}</h2>
        <p className="event-modal__date">{post.date}</p>
        <dl className="event-modal__table">
          <div><dt>장소</dt><dd>{post.place}</dd></div>
          <div><dt>주최</dt><dd>{post.host}</dd></div>
          <div><dt>발급 카드</dt><dd>{post.cardLabel}</dd></div>
        </dl>
        <p className="event-modal__text">{post.text}</p>
      </div>
    </div>
  );
}
