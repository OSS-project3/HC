import { companyInfo } from "../config/company";
import "./ContentPages.css";

const values = [
  { number: "01", title: "한국 이름", text: "한글의 소리와 뜻을 살펴 개인에게 어울리는 한국 이름을 제안합니다." },
  { number: "02", title: "문화의 기록", text: "이름에 담긴 이야기와 경험을 카드와 콘텐츠의 형태로 오래 남깁니다." },
  { number: "03", title: "함께하는 경험", text: "개인부터 기관·단체까지 한국 문화를 쉽고 즐겁게 경험하도록 돕습니다." },
];

export function CompanyPage() {
  return (
    <div className="content-page">
      <header className="subpage-hero page-container">
        <p className="eyebrow">회사 소개</p>
        <h1 className="subpage-hero__title">한글과 세종</h1>
        <p className="section-lead">한국 이름에서 시작해, 기억에 남는 문화 경험을 만듭니다.</p>
      </header>

      <section className="advisor-section page-container">
        <div className="advisor-video">
          <div className="advisor-video__placeholder" role="img" aria-label="담당 고문 인사 영상 준비 중">
            <span aria-hidden="true">▶</span>
            <p>담당 고문 인사 영상</p>
            <small>영상 준비 중</small>
          </div>
          {/*
            최종 영상 전달 후 위 placeholder를 아래와 같이 교체할 수 있습니다.
            <video controls poster="/images/company/advisor-poster.jpg">
              <source src="/videos/advisor-greeting.mp4" type="video/mp4" />
            </video>
          */}
        </div>
        <div className="advisor-copy">
          <p className="content-kicker">ADVISOR MESSAGE</p>
          <h2>한글로 이어지는 인연을 소중히 생각합니다.</h2>
          <p>
            한글은 생각과 마음을 담아 서로를 연결하는 소중한 문화유산입니다. 한글과 세종은 한국 이름을
            통해 한글의 아름다움과 한국 문화를 더욱 친근하게 경험할 수 있도록 노력하고 있습니다.
          </p>
          <p>
            한 분 한 분의 이름에 담긴 이야기가 오래 기억될 수 있도록 정성과 책임을 다하겠습니다.
          </p>
          <strong>한글과 세종 담당 고문</strong>
        </div>
      </section>

      <section className="content-intro page-container">
        <div className="content-intro__mark" aria-hidden="true">ㅎ</div>
        <div>
          <p className="content-kicker">HANGUL &amp; SEJONG</p>
          <h2>이름을 짓는 일은<br />한 사람의 이야기를 발견하는 일입니다.</h2>
          <p>
            한글과 세종은 한국 이름의 의미와 아름다움을 전하고, 그 경험을 명예한국인증과 방문증 등
            다양한 카드 콘텐츠로 연결합니다. 이름을 만들고 풀이하며 기록하는 모든 과정을 하나의
            문화 서비스로 제공합니다.
          </p>
        </div>
      </section>

      <section className="content-band">
        <div className="page-container">
          <div className="content-section-head">
            <p className="content-kicker">OUR VALUES</p>
            <h2>한글과 세종이 중요하게 생각하는 것</h2>
          </div>
          <div className="value-grid">
            {values.map((value) => (
              <article className="value-card" key={value.number}>
                <span>{value.number}</span>
                <h3>{value.title}</h3>
                <p>{value.text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="company-contact page-container">
        <div className="content-section-head">
          <p className="content-kicker">CONTACT</p>
          <h2>한글과 세종을 만나보세요</h2>
        </div>
        <dl className="company-contact__grid">
          <div><dt>주소</dt><dd>{companyInfo.address}</dd></div>
          <div><dt>대표전화</dt><dd>{companyInfo.phone}</dd></div>
          <div><dt>이메일</dt><dd>{companyInfo.email}</dd></div>
          <div><dt>운영시간</dt><dd>{companyInfo.businessHours}</dd></div>
        </dl>
      </section>
    </div>
  );
}
