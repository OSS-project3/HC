import { Link } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../../features/auth/AuthContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../../components/admin/ContentAdminPanel";
import { EventFeedAdminPanel } from "../../components/admin/EventFeedAdminPanel";
import { ImagePlaceholder } from "../../components/ui/ImagePlaceholder";
import { Modal } from "../../components/ui/Modal";
import { BOOTH_GALLERY, COLLAB_GALLERY, boothPosts, collabPosts, loadFeedPosts, saveFeedPosts, type FeedPost } from "../../data/eventFeedPosts";
import "../../styles/ContentPages.css";
import "./EventsPage.css";

const programs = [
  { tag: "문화 체험", title: "한국 이름 만들기", text: "참가자의 정보를 바탕으로 한국 이름을 제안하고 이름에 담긴 뜻과 이야기를 소개합니다." },
  { tag: "기관·단체", title: "맞춤형 카드 제작", text: "행사 성격과 기관의 목적에 맞춰 명예한국인증·방문증 등 카드 콘텐츠를 구성합니다." },
  { tag: "현장 운영", title: "행사 연계 프로그램", text: "관광·교육·교류 행사 안에서 이름 체험과 카드 수령이 자연스럽게 이어지도록 설계합니다." },
];

const process = ["상담 및 목적 확인", "참가자 정보 접수", "이름·카드 제작", "현장 운영 및 전달"];

export function EventsPage() {
  const { isAdmin } = useAuth();
  const defaults: ManagedContent[] = programs.map((program, index) => ({ id: `event-${index}`, title: program.title, content: program.text, meta: program.tag }));
  const [managedPrograms, setManagedPrograms] = useState(() => loadManagedContent("events", defaults));
  const [managedBoothPosts, setManagedBoothPosts] = useState(() => loadFeedPosts("booth-posts", boothPosts));
  const [managedCollabPosts, setManagedCollabPosts] = useState(() => loadFeedPosts("collab-posts", collabPosts));
  const updatePrograms = (items: ManagedContent[]) => { localStorage.setItem("managed-content:events", JSON.stringify(items)); setManagedPrograms(items); };
  const updateBoothPosts = (items: FeedPost[]) => { saveFeedPosts("booth-posts", items); setManagedBoothPosts(items); };
  const updateCollabPosts = (items: FeedPost[]) => { saveFeedPosts("collab-posts", items); setManagedCollabPosts(items); };
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

      {isAdmin && <EventFeedAdminPanel label="부스 운영 게시글" items={managedBoothPosts} onChange={updateBoothPosts} />}
      <EventFeed
        title="부스 운영"
        tagline="현장에서 고객과 직접 만나 정성을 담은 서비스를 제공합니다"
        posts={managedBoothPosts}
        gallery={BOOTH_GALLERY}
        pageSize={4}
      />

      {isAdmin && <EventFeedAdminPanel label="법인·단체 협업 게시글" items={managedCollabPosts} onChange={updateCollabPosts} showCompanyFields />}
      <EventFeed
        title="법인·단체 협업"
        tagline="다양한 법인•단체와의 협업을 통해 한글 이름과 카드를 제공합니다"
        posts={managedCollabPosts}
        gallery={COLLAB_GALLERY}
        layout="collaboration"
        pageSize={8}
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
function EventCard({ post, onOpen, wide = false, compact = false }: { post: FeedPost; onOpen: (post: FeedPost) => void; wide?: boolean; compact?: boolean }) {
  return (
    <article className={`event-card${wide ? " event-card--wide" : ""}${compact ? " event-card--compact" : ""}`}>
      <div className="event-card__media">
        {post.image
          ? <img src={post.image} alt={`${post.title} 사진`} loading="lazy" />
          : <ImagePlaceholder label={`${post.title} 사진`} />}
      </div>
      <div className="event-card__body">
        {compact && (
          <span className={`event-card__logo${post.logoUrl ? ` event-card__logo--${post.logoUrl.split("/").pop()?.replace(".svg", "")}` : ""}`}>
            {post.logoUrl ? <img src={post.logoUrl} alt={post.company ?? post.host} /> : post.company ?? post.cardLabel}
          </span>
        )}
        <h3>{post.title}</h3>
        {!compact && (
          <div className="event-card__meta">
            <time>{post.date}</time>
            <span className="event-card__label">{post.cardLabel}</span>
          </div>
        )}
        <p className={`event-card__text${wide ? " event-card__text--full" : ""}`}>{post.text}</p>
        {compact ? (
          <footer className="event-card__foot">
            <time>{post.date}</time>
            <button type="button" className="event-card__more" onClick={() => onOpen(post)}>
              자세히 보기<span aria-hidden="true"> →</span>
            </button>
          </footer>
        ) : (
          <button type="button" className="event-card__more" onClick={() => onOpen(post)}>
            자세히 보기<span aria-hidden="true"> →</span>
          </button>
        )}
      </div>
    </article>
  );
}

/**
 * 부스 운영 / 법인·단체 협업 공통 피드.
 * 대표 기록 1개를 상단에 사진 60% / 글 40% 와이드 카드로, 나머지는 한 행에 3개 카드로 배열한다.
 * 카드 하단의 "자세히 보기"를 누르면 상세 팝업이 뜬다.
 */
function EventFeed({ title, tagline, posts, gallery, layout = "featured", pageSize }: {
  title: string;
  tagline: string;
  posts: FeedPost[];
  gallery: string[];
  layout?: "featured" | "collaboration";
  pageSize?: number;
}) {
  const [active, setActive] = useState<FeedPost | null>(null);
  const [page, setPage] = useState(1);
  const isCollaboration = layout === "collaboration";
  const size = pageSize ?? (isCollaboration ? 8 : 4);
  const totalPages = Math.max(1, Math.ceil(posts.length / size));
  const currentPage = Math.min(page, totalPages);
  const visiblePosts = posts.slice((currentPage - 1) * size, currentPage * size);
  const [featured, ...rest] = visiblePosts;

  return (
    <section className={`event-feed page-container${isCollaboration ? " event-feed--collaboration" : ""}`}>
      <header className="event-feed__head">
        <h2 className="event-feed__title">{title}</h2>
        <span className="event-feed__rule" aria-hidden="true" />
        <p className="event-feed__tagline">{tagline}</p>
      </header>

      {!isCollaboration && featured && <EventCard post={featured} onOpen={setActive} wide />}

      <div className={isCollaboration ? "event-card-grid event-card-grid--collaboration" : "event-card-grid"}>
        {(isCollaboration ? visiblePosts : rest).map((post) => <EventCard key={`${post.title}-${post.date}`} post={post} onOpen={setActive} compact={isCollaboration} />)}
      </div>

      {totalPages > 1 && (
        <nav className="event-feed__pagination" aria-label={`${title} 페이지`}>
          <button type="button" className="event-feed__page-arrow" aria-label="이전 페이지" disabled={currentPage === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>‹</button>
          <div className="event-feed__page-dots">
            {Array.from({ length: totalPages }, (_, index) => index + 1).map((number) => (
              <button
                type="button"
                key={number}
                className={number === currentPage ? "is-current" : ""}
                aria-label={`${number}페이지로 이동`}
                aria-current={number === currentPage ? "page" : undefined}
                onClick={() => setPage(number)}
              />
            ))}
          </div>
          <button type="button" className="event-feed__page-arrow" aria-label="다음 페이지" disabled={currentPage === totalPages} onClick={() => setPage((value) => Math.min(totalPages, value + 1))}>›</button>
        </nav>
      )}

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
