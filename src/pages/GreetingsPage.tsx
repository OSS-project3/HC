import "./ContentPages.css";
import "./GreetingsPage.css";

const messages = [
  {
    role: "고문",
    sectionTitle: "고문 인사말",
    title: "한국과 한국의 미래",
    paragraphs: [
      "미래 학자로서 한국의 우수성에 언제나 경탄해 마지않고 있습니다. 특히 과학자로서의 사고로도 한글의 위대함은 경이로우며, AI 시대 세계 문화를 선도하는 한국의 힘의 원천은 한글에 있다 해도 과언이 아닙니다.",
      "지금 한류가 세계사적 흐름에 지대한 영향을 미치고 있습니다. 세계인이 한국을 선망의 눈으로 바라보며 가고 싶은 나라, 닮고 싶은 나라로 한국을 지목하고 있습니다. 한국어를 배우려는 사람이 갈수록 늘어나는 이때 그들에게 한국 이름을 갖게 하여, 자연스럽게 한글을 배우게 되고 한국 문화를 이해하게 되며 더 나아가 한국의 역사까지도 공부하여 한국을 더욱더 잘 알게 될 것입니다.",
      "미래는 문화를 선도하는 나라가 진정한 선진국이 될 것입니다. 특히 한국을 찾는 세계인을 따뜻하게 안아주고 동등하게 대하며 그들이 한국에 체류하는 동안에 한국 사람들과 ‘우리’가 되는 체험을 한다면, 그들은 한국을 제2의 모국이라 생각할 것입니다.",
      "앞으로 더욱더 많은 세계인이 한국을 찾게 하는 방법이 바로 한국 이름을 지어주기입니다. 우리나라가 한글과 세종대왕 덕분에 한국 문화로 세계를 정복하는 미래 시대를 꿈꿔 봅니다. 아니 이미 시작되었는지도 모를 일입니다.",
      "감사합니다.",
    ],
    organization: "(주)한글과 세종 고문",
    name: "KAIST 총장  이광형",
    photo: "/images/company/kaist-president.jpg",
    photoAlt: "KAIST 총장 이광형",
  },
  {
    role: "이사장",
    sectionTitle: "이사장 인사말",
    title: "한국 이름으로 세계를 정복하는 새로운 광개토 시대를 엽니다",
    paragraphs: [
      "21세기 우리나라는 여러 면에서 세계적인 강국이 되어가고 있습니다. 특히 문화강국으로서의 위상은 날로 높아지고 있습니다. 세계적인 한국문화 열풍에는 한글이 큰 비중을 차지하고 있습니다.",
      "정부에서는 2027년 한국방문 3,000만 명을 목표로 하고 있으며 갈수록 늘어나는 체류형 외국인도 약 280만 명 정도가 됩니다. 이러한 때에 세계인과 한국인이 하나가 되는 것을 목표로 한글로 된 한국 이름 지어주는 일을 하고자 합니다.",
      "외국인이 한국 이름을 갖는 것은 한글과 우리말을 익히게 되는 첫 번째 관문이 되고 한국문화를 이해하고 그 문화에 녹아드는 가장 손쉬운 방법이 됩니다. 우리나라의 이름에는 뜻과 의미가 있습니다. 외국인도 뜻과 의미가 있는 한국 이름을 사용한다면 그들은 한국을 제2의 모국이라 생각할 것이며 한국 사람이 되어 한국을 깊은 애정을 가지고 대할 것입니다. 또한 외국인을 대하는 우리도 그들을 따뜻하게 동등하게 대할 것입니다.",
      "한글과 세종은 공익적인 법인으로 우리나라의 위상을 높이고 한글의 세계화를 이루고자 우선 한국 이름 지어주기 1억 명을 목표로 하고 있습니다. 이 일은 세계 최초의 일이며 세종대왕의 한글로 새로운 문화 시대를 열 것입니다.",
    ],
    organization: "(주)한글과 세종 이사장",
    name: "유철호",
    photo: "/images/company/chairman.jpg",
    photoAlt: "한글과 세종 이사장 유철호",
  },
];

export function GreetingsPage() {
  return (
    <div className="content-page greetings-page">
      <header className="greetings-hero subpage-hero page-container">
        <p className="eyebrow">회사 소개 · 인사말</p>
        <h1 className="subpage-hero__title">한글과 세종</h1>
      </header>

      <div className="greetings-page__messages page-container">
        {messages.map((message) => (
          <section className="greeting-block" key={message.role}>
            <h2 className="greeting-block__label">{message.sectionTitle}</h2>
            <div className="greeting-block__body">
              <div className="greeting-block__photo">
                <img src={message.photo} alt={message.photoAlt} />
              </div>
              <div className="greeting-block__copy">
                <h3>{message.title}</h3>
                {message.paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
                <div className="greeting-block__signature">
                  <strong>{message.organization}</strong>
                  <span>{message.name}</span>
                </div>
              </div>
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
