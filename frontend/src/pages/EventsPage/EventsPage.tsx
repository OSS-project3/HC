import { Link } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../features/auth/AuthContext";
import { useLanguage } from "../../features/i18n/LanguageContext";
import { ContentAdminPanel, loadManagedContent, type ManagedContent } from "../../components/admin/ContentAdminPanel";
import { EventAdminPanel } from "../../components/admin/EventAdminPanel";
import { ImagePlaceholder } from "../../components/ui/ImagePlaceholder";
import { Modal } from "../../components/ui/Modal";
import { eventToFeedPost, type FeedPost } from "../../data/eventFeedPosts";
import { api } from "../../services/api";
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
  const { t, language } = useLanguage();
  // PROGRAM 카드는 백엔드에 대응 API가 없어 로컬 목데이터로 유지한다(FRONTEND_API_REQUIREMENTS §12 CMS 범위).
  const defaults: ManagedContent[] = programs.map((program, index) => ({ id: `event-${index}`, title: program.title, content: program.text, meta: program.tag }));
  const [managedPrograms, setManagedPrograms] = useState(() => loadManagedContent("events", defaults));
  const updatePrograms = (items: ManagedContent[]) => { localStorage.setItem("managed-content:events", JSON.stringify(items)); setManagedPrograms(items); };

  const [managedBoothPosts, setManagedBoothPosts] = useState<FeedPost[]>([]);
  const [managedCollabPosts, setManagedCollabPosts] = useState<FeedPost[]>([]);
  // 언어 전환 시 Accept-Language가 바뀌므로 목록을 재조회한다.
  const reloadBooth = useCallback(() => { api.listEvents({ type: "BOOTH", size: 100 }).then((d) => setManagedBoothPosts(d.content.map(eventToFeedPost))).catch(() => undefined); }, [language]);
  const reloadCollab = useCallback(() => { api.listEvents({ type: "COLLABORATION", size: 100 }).then((d) => setManagedCollabPosts(d.content.map(eventToFeedPost))).catch(() => undefined); }, [language]);
  useEffect(() => { reloadBooth(); reloadCollab(); }, [reloadBooth, reloadCollab]);
  return (
    <div className="content-page events-page">
      <header className="subpage-hero page-container">
        <p className="eyebrow">{t("행사사업")}</p>
        <h1 className="subpage-hero__title">{t("한글로 연결되는 문화 행사")}</h1>
        <p className="section-lead">{t("기관과 행사의 목적에 맞는 한국 이름·카드 체험 프로그램을 제안합니다.")}</p>
      </header>

      <section className="event-programs page-container">
        {isAdmin && <ContentAdminPanel label="이벤트" items={managedPrograms} onChange={updatePrograms} />}
        <div className="content-section-head">
          <p className="content-kicker">PROGRAM</p>
          <h2>{t("행사에 맞춰 다양하게 구성합니다.")}</h2>
        </div>
        <div className="program-grid">
          {managedPrograms.map((program, index) => (
            <article className="program-card" key={program.title}>
              <span>{t(program.meta ?? "")}</span>
              <b>{String(index + 1).padStart(2, "0")}</b>
              <h3>{t(program.title)}</h3>
              <p>{t(program.content)}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="content-band">
        <div className="page-container">
          <div className="content-section-head">
            <p className="content-kicker">PROCESS</p>
            <h2>{t("행사 진행 과정")}</h2>
          </div>
          <ol className="process-list">
            {process.map((item, index) => (
              <li key={item}><span>{String(index + 1).padStart(2, "0")}</span><p>{t(item)}</p></li>
            ))}
          </ol>
        </div>
      </section>

      {isAdmin && <EventAdminPanel label="부스 운영 게시글" eventType="BOOTH" onChanged={reloadBooth} />}
      <EventFeed
        title="부스 운영"
        tagline="현장에서 고객과 직접 만나 정성을 담은 서비스를 제공합니다"
        posts={managedBoothPosts}
        pageSize={4}
      />

      {isAdmin && <EventAdminPanel label="법인·단체 협업 게시글" eventType="COLLABORATION" onChanged={reloadCollab} />}
      <EventFeed
        title="법인·단체 협업"
        tagline="다양한 법인•단체와의 협업을 통해 한글 이름과 카드를 제공합니다"
        posts={managedCollabPosts}
        layout="collaboration"
        pageSize={8}
      />

      <section className="event-inquiry page-container">
        <div>
          <p className="content-kicker">PARTNERSHIP</p>
          <h2>{t("기관·단체 행사를 준비하고 계신가요?")}</h2>
          <p>{t("행사 일정과 예상 인원, 필요한 카드 유형을 알려주시면 적합한 운영 방식을 안내해 드립니다.")}</p>
        </div>
        <Link to="/support#contact">{t("행사 상담 문의")}　→</Link>
      </section>
    </div>
  );
}

/** A single record card (photo on top, content below). */
function EventCard({ post, onOpen, wide = false, compact = false }: { post: FeedPost; onOpen: (post: FeedPost) => void; wide?: boolean; compact?: boolean }) {
  const { t, language } = useLanguage();
  const photoAlt = language === "en" ? `Photo from ${t(post.title)}` : `${post.title} 사진`;
  return (
    <article className={`event-card${wide ? " event-card--wide" : ""}${compact ? " event-card--compact" : ""}`}>
      <div className="event-card__media">
        {post.image
          ? <img src={post.image} alt={photoAlt} loading="lazy" />
          : <ImagePlaceholder label={photoAlt} />}
      </div>
      <div className="event-card__body">
        {compact && (
          <span className={`event-card__logo${post.logoUrl ? ` event-card__logo--${post.logoUrl.split("/").pop()?.replace(".svg", "")}` : ""}`}>
            {post.logoUrl ? <img src={post.logoUrl} alt={post.company ?? post.host} /> : post.company ?? t(post.cardLabel)}
          </span>
        )}
        <h3>{t(post.title)}</h3>
        {!compact && (
          <div className="event-card__meta">
            <time>{post.date}</time>
            <span className="event-card__label">{t(post.cardLabel)}</span>
          </div>
        )}
        <p className={`event-card__text${wide ? " event-card__text--full" : ""}`}>{t(post.text)}</p>
        {compact ? (
          <footer className="event-card__foot">
            <time>{post.date}</time>
            <button type="button" className="event-card__more" onClick={() => onOpen(post)}>
              {t("자세히 보기")}<span aria-hidden="true"> →</span>
            </button>
          </footer>
        ) : (
          <button type="button" className="event-card__more" onClick={() => onOpen(post)}>
            {t("자세히 보기")}<span aria-hidden="true"> →</span>
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
function EventFeed({ title, tagline, posts, layout = "featured", pageSize }: {
  title: string;
  tagline: string;
  posts: FeedPost[];
  layout?: "featured" | "collaboration";
  pageSize?: number;
}) {
  const { t, language } = useLanguage();
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
        <h2 className="event-feed__title">{t(title)}</h2>
        <span className="event-feed__rule" aria-hidden="true" />
        <p className="event-feed__tagline">{t(tagline)}</p>
      </header>

      {!isCollaboration && featured && <EventCard post={featured} onOpen={setActive} wide />}

      <div className={isCollaboration ? "event-card-grid event-card-grid--collaboration" : "event-card-grid"}>
        {(isCollaboration ? visiblePosts : rest).map((post) => <EventCard key={`${post.title}-${post.date}`} post={post} onOpen={setActive} compact={isCollaboration} />)}
      </div>

      {totalPages > 1 && (
        <nav className="event-feed__pagination" aria-label={language === "en" ? `${t(title)} pages` : `${title} 페이지`}>
          <button type="button" className="event-feed__page-arrow" aria-label={t("이전 페이지")} disabled={currentPage === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>‹</button>
          <div className="event-feed__page-dots">
            {Array.from({ length: totalPages }, (_, index) => index + 1).map((number) => (
              <button
                type="button"
                key={number}
                className={number === currentPage ? "is-current" : ""}
                aria-label={language === "en" ? `Go to page ${number}` : `${number}페이지로 이동`}
                aria-current={number === currentPage ? "page" : undefined}
                onClick={() => setPage(number)}
              />
            ))}
          </div>
          <button type="button" className="event-feed__page-arrow" aria-label={t("다음 페이지")} disabled={currentPage === totalPages} onClick={() => setPage((value) => Math.min(totalPages, value + 1))}>›</button>
        </nav>
      )}

      <Modal open={active !== null} onClose={() => setActive(null)} title={active ? t(active.title) : ""} className="event-modal">
        {active && <EventDetail key={active.id ?? active.title} post={active} sectionLabel={title} />}
      </Modal>
    </section>
  );
}

/** Detail popup content: gallery (main image + thumbnails) left, info right.
 *  Gallery images are fetched per-post from the Event detail API. */
function EventDetail({ post, sectionLabel }: { post: FeedPost; sectionLabel: string }) {
  const { t, language } = useLanguage();
  const [galleryUrls, setGalleryUrls] = useState<string[]>([]);
  useEffect(() => {
    if (post.id == null) return;
    let cancelled = false;
    api.getEvent(post.id).then((data) => { if (!cancelled) setGalleryUrls(data.images.map((img) => img.url)); }).catch(() => undefined);
    return () => { cancelled = true; };
  }, [post.id]);
  const images = [post.image, ...galleryUrls].filter((src, i, arr): src is string => Boolean(src) && arr.indexOf(src) === i);
  const [index, setIndex] = useState(0);
  const current = images[index];
  const move = (delta: number) => setIndex((i) => (i + delta + images.length) % images.length);

  return (
    <div className="event-modal__grid">
      <div className="event-modal__gallery">
        <div className="event-modal__main">
          {current
            ? <img src={current} alt={language === "en" ? `${t(post.title)} photo ${index + 1}` : `${post.title} 사진 ${index + 1}`} />
            : <ImagePlaceholder label={language === "en" ? `Photo from ${t(post.title)}` : `${post.title} 사진`} />}
        </div>
        {images.length > 1 && (
          <div className="event-modal__thumbs">
            <button type="button" className="event-modal__arrow" onClick={() => move(-1)} aria-label={t("이전 사진")}>‹</button>
            <ul>
              {images.map((src, i) => (
                <li key={src}>
                  <button
                    type="button"
                    className={`event-modal__thumb${i === index ? " is-active" : ""}`}
                    onClick={() => setIndex(i)}
                    aria-label={language === "en" ? `Photo ${i + 1}` : `${i + 1}번째 사진`}
                    aria-current={i === index}
                  >
                    <img src={src} alt="" />
                  </button>
                </li>
              ))}
            </ul>
            <button type="button" className="event-modal__arrow" onClick={() => move(1)} aria-label={t("다음 사진")}>›</button>
          </div>
        )}
      </div>
      <div className="event-modal__info">
        <p className="event-modal__kicker">{t(sectionLabel)}</p>
        <h2 className="event-modal__title">{t(post.title)}</h2>
        <p className="event-modal__date">{post.date}</p>
        <dl className="event-modal__table">
          <div><dt>{t("장소")}</dt><dd>{t(post.place)}</dd></div>
          <div><dt>{t("주최")}</dt><dd>{t(post.host)}</dd></div>
          <div><dt>{t("발급 카드")}</dt><dd>{t(post.cardLabel)}</dd></div>
        </dl>
        <p className="event-modal__text">{t(post.text)}</p>
      </div>
    </div>
  );
}
