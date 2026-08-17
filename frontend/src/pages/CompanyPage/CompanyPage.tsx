import { useRef } from "react";
import { companyInfo } from "../../config/company";
import { useScrollReveal } from "../../lib/useScrollReveal";
import "./CompanyPage.css";

const promises = [
  {
    title: "의미 있는 이름",
    text: "한국 사람은 누구나 뜻과 의미가 있는 이름을 사용하고 있습니다. 우리나라를 방문한 외국인도 체류하는 동안 뜻과 의미가 있는 한국 이름을 갖고 사용하게 된다면, 한국에 대한 호감도가 더욱 상승할 것이며 이름의 뜻과 의미대로 인생을 살도록 노력할 것입니다.",
  },
  {
    title: "정중한 경험",
    text: "전 세계 어떤 나라도 자국을 방문하는 외국인에게 이름을 지어주지는 않습니다. 하지만 우리나라가 선도적으로 외국인에게 한국 이름을 지어준다면, 한국의 문화를 체험하며 한국인과 소통하고 교류하는 것에 상당한 의미 부여를 할 것입니다.",
  },
  {
    title: "오래 남는 기념",
    text: "한국 이름을 갖게 되는 것은 특별한 경험으로 외국인과 한국인이 하나가 되는 것이며, 한국 문화를 공유하며 한글의 우수성을 알게 될 것이며, 한국어로 교류하려는 현상이 자연스럽게 형성될 것입니다. 그 어떠한 관광 상품보다 한국 이름을 갖게 되는 체험은 오래도록 기억에 남을 것입니다. 또한 자국에 돌아가서도 한국에 대한 호감은 바뀌지 않을 것입니다.",
  },
];

const process = [
  { title: "정보 입력", text: "생년월일과 출생 시간, 지역을 입력합니다. 모든 정보는 작명에만 사용됩니다." },
  { title: "한글 오행 분석", text: "명리학 관점에서 사주를 분석해 어울리는 기운과 글자의 방향을 찾습니다." },
  { title: "한국 이름 추천", text: "뜻과 발음을 함께 고려한 한국 이름 후보와 풀이를 제안합니다." },
  { title: "모바일 카드 발급", text: "이름과 십이지 캐릭터가 담긴 명예한국인증·명예시민증·학생증·방문증을 발급합니다." },
];

const history = [
  { date: "2026. 04", title: "프로젝트 기획", text: "한글 오행을 기반으로 한국 이름 작명 특허를 출원하였습니다." },
  { date: "2026. 06", title: "홈페이지 개설", text: "영어를 포함한 다국어 안내로 더 많은 나라의 방문객을 맞이하기 시작했습니다." },
  { date: "2026. 07", title: "2027년 한국 방문의 해 3,000만 명 방문 예비사업", text: "한국을 찾는 전 세계 방문객에게 한국 이름을 작명하여 각종 명예한국인증·명예시민증·학생증·방문증에 발급하고, 세계인이 한국 문화를 체험하며 한글의 우수성을 알게 합니다. 또한 세계인과 한국인이 ‘우리’가 되는 경험을 합니다." },
  { date: "2026. 10", title: "문화 콘텐츠 정식 출시", text: "한국 이름으로 된 명예한국인증·명예시민증·학생증·방문증 발행 업무를 시작했습니다." },
];

const roadmap = [
  { date: "2027. 01", title: "한국 방문객 3,000만 시대 대비", text: "외국인 근로자 한국 이름 작명 발급(2025년 278만 명 체류 중)\n외국인 요양 보호사 한국 이름 발급(2040년 요양 보호사 200만 명 수요)" },
  { date: "2027. 03", title: "외국 유학생 62만 명", text: "한국 이름 학생증 발급" },
];

export function CompanyPage() {
  const rootRef = useRef<HTMLDivElement>(null);
  useScrollReveal(rootRef);

  return (
    <div className="company-page" ref={rootRef}>
      <header id="about" className="company-hero subpage-hero page-container anchor-section">
        <p className="eyebrow">회사 소개</p>
        <h1 className="subpage-hero__title">한글과 세종</h1>
      </header>

      <section className="company-overview page-container reveal">
        <div className="company-overview__lead">
          <h2>이름 하나로,<br />한국과 깊이 연결되도록</h2>
          <p>여행은 끝나도 이름은 남습니다.<br />저희는 한국을 찾은 한 분 한 분께 한글 오행에 기반한 한국 이름을 지어 드리고, 그 이름을 디지털 명예 시민증에 담아 오래도록 간직할 수 있는 기념으로 만들어 드립니다.</p>
        </div>
        <div className="company-overview__details">
          <article>
            <h3>정통 명리 성명학</h3>
            <p>수십 년간 전통 작명과 명리학을 연구해 온 전문가의 관점으로, 생년월일시와 출생 지역을 해석해 이름에 뜻과 의미를 담습니다.</p>
          </article>
          <article>
            <h3>따뜻한 경험</h3>
            <p>낯선 곳에서 이방인으로 생활한다는 것은 인생의 몇 번 없는 모험입니다. 또한 현지인들과의 문화적 차이로 생활의 어려움을 겪으며 심지어는 보이지 않는 차별 등으로 적응함에 어려움을 겪는 경우도 많습니다. 그러나 한국인은 본래 따뜻함과 친절한 마음을 가지고 살아가며 낯선 이방인에게도 관대하고 포용력이 좋은 민족입니다. 만약 당신이 한국 이름을 갖고 한국 생활을 한다면 더욱더 따뜻한 환대를 받을 것이며 너와 내가 아닌 ‘우리가 되어’ 차별받지 않고 동등한 위치에서 살아가며 한국의 문화를 더욱 폭넓게 이해하고 적응하며 한국어 학습에 크게 도움이 될 것입니다. 또한 한국에서의 체류 기간이 끝나고 당신의 나라로 돌아가도 한국에 따뜻한 환대는 영원히 기억될 것입니다.</p>
          </article>
          <article>
            <h3>디지털 명예 한국인증 · 명예 시민증 · 학생증 · 방문증</h3>
            <p>종이가 아닌, 언제 어디서나 꺼내 볼 수 있는 디지털 시민증으로 한국에서의 추억을 안전하게 보관합니다.</p>
          </article>
        </div>
      </section>

      <section className="company-promises page-container reveal-group">
        <h2 className="reveal-item">저희가 드리는 약속</h2>
        <div className="company-promises__list">
          {promises.map((item) => <article className="reveal-item" key={item.title}><h3>{item.title}</h3><p>{item.text}</p></article>)}
        </div>
      </section>

      <section className="company-process page-container reveal-group">
        <h2 className="reveal-item">이름이 만들어지는 과정</h2>
        <p className="reveal-item">복잡한 절차 없이, 네 단계면 나만의 한국 이름과 명예한국인증·명예시민증·학생증·방문증</p>
        <div className="company-process__grid">
          {process.map((item, index) => <article className="reveal-item" key={item.title}><span>0{index + 1}</span><h3>{item.title}</h3><p>{item.text}</p></article>)}
        </div>
      </section>

      <section className="company-roadmap page-container reveal-group">
        <div className="company-section-kicker reveal-item">연혁 및 로드맵</div>
        <div className="company-roadmap__columns">
          <Timeline title="걸어온 길" items={history} />
          <Timeline title="걸어갈 길" items={roadmap} />
        </div>
      </section>

      <section className="company-tree reveal">
        <div className="company-tree__inner page-container">
          <img src="/images/logo/tree.png" alt="흰 꽃이 핀 이팝나무" loading="lazy" decoding="async" />
          <div>
            <div className="company-section-kicker">상징수</div>
            <h2>이팝나무</h2>
            <p>이팝나무는 이밥 나무라고도 합니다.</p>
            <p>나무의 꽃잎이 마치 쌀알처럼 생겼고 태조 이성계 덕분에 일반 백성도 쌀밥을 먹게 되면서 이씨(李氏)가 주는 밥이다 하여 이밥 나무라는 이름으로 전해져 내려오고 있습니다.</p>
            <p>한글을 창제한 세종대왕이 성씨(姓氏)가 이씨(李氏)이고 한글과 세종에서 한국 이름 지어주는 사업이 이팝나무 꽃잎처럼 수많은 세계인에게 한국 이름을 지어주기를 바라는 뜻이 담겨 있습니다.</p>
          </div>
        </div>
      </section>

      <section id="directions" className="company-location page-container anchor-section reveal">
        <div className="company-section-kicker">위치</div>
        <h2>찾아오시는 길</h2>
        <img
          className="company-location__map"
          src="/images/common/map.jpg"
          alt="한글과 세종 찾아오시는 길 약도"
          loading="lazy"
        />
        <div className="company-location__info">
          <div>
            <p>⌖ {companyInfo.address}</p>
            <p>◷ {companyInfo.businessHours} · 주말 및 공휴일 휴무</p>
            <p>♧ {companyInfo.phone}</p>
          </div>
          <a href={`https://map.naver.com/p/search/${encodeURIComponent(companyInfo.address)}`} target="_blank" rel="noreferrer">길찾기</a>
        </div>
      </section>
    </div>
  );
}

function Timeline({ title, items }: { title: string; items: typeof history }) {
  return (
    <div className="timeline">
      <h2>{title}</h2>
      <ol>
        {items.map((item) => <li className="reveal-item" key={item.date}><strong>{item.date}</strong><h3>{item.title}</h3><p>{item.text}</p></li>)}
      </ol>
    </div>
  );
}
