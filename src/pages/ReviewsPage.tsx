import { Link } from "react-router-dom";
import "./ContentPages.css";

const reviews = [
  { type: "개인 신청", title: "한국에서의 추억이 이름과 카드로 남았어요.", text: "이름의 뜻을 함께 설명해 주셔서 여행이 끝난 뒤에도 특별한 기억으로 간직하고 있습니다.", author: "명예한국인증 신청자" },
  { type: "단체 신청", title: "행사 참가자에게 색다른 경험을 선물했습니다.", text: "신청부터 수령까지 과정이 명확했고, 참가자들의 만족도도 높아 다음 행사에서도 활용하고 싶습니다.", author: "문화행사 운영 담당자" },
  { type: "방문증", title: "한국 문화를 자연스럽게 소개할 수 있었습니다.", text: "방문객 정보에 맞춘 카드가 행사 안내와 기념품 역할을 함께해 현장 반응이 좋았습니다.", author: "기관 방문 프로그램 담당자" },
  { type: "모바일 카드", title: "휴대전화로 간편하게 확인하고 공유했어요.", text: "실물 카드와 함께 모바일 카드도 받을 수 있어 가족과 친구들에게 쉽게 보여줄 수 있었습니다.", author: "개인 신청자" },
  { type: "이름 풀이", title: "이름에 담긴 의미가 가장 인상적이었습니다.", text: "단순히 이름만 받는 것이 아니라 뜻과 이야기를 함께 알 수 있어 더욱 의미 있었습니다.", author: "한국 문화 체험 참가자" },
  { type: "기관 협업", title: "프로그램의 완성도를 높여준 콘텐츠였습니다.", text: "기존 체험 과정에 자연스럽게 연결할 수 있었고 결과물까지 제공되어 운영하기 편리했습니다.", author: "교육 프로그램 담당자" },
];

export function ReviewsPage() {
  return (
    <div className="content-page">
      <header className="subpage-hero page-container">
        <p className="eyebrow">후기</p>
        <h1 className="subpage-hero__title">함께 만든 이야기</h1>
        <p className="section-lead">한글과 세종을 경험한 분들의 이야기를 소개합니다.</p>
      </header>

      <section className="reviews-section page-container">
        <div className="review-cards">
          {reviews.map((review, index) => (
            <article className="review-card" key={review.title}>
              <div className="review-card__top">
                <span>{review.type}</span>
                <b>{String(index + 1).padStart(2, "0")}</b>
              </div>
              <h2>{review.title}</h2>
              <p>{review.text}</p>
              <small>{review.author}</small>
            </article>
          ))}
        </div>
      </section>

      <section className="content-cta">
        <div className="page-container">
          <div>
            <p className="content-kicker">YOUR STORY</p>
            <h2>나만의 한국 이름과 카드를 만나보세요.</h2>
          </div>
          <Link to="/apply/honorary-korean">제작 신청　→</Link>
        </div>
      </section>
    </div>
  );
}
