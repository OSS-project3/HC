// Apply flow: sample preview panel shown alongside the application form.
import type { CardDesign } from "../../data/cards";

interface CardPreviewPanelProps {
  design: CardDesign;
  /** 사용자가 선택한 카드 방향. 학생증에서 견본 이미지를 가로/세로로 바꾼다. */
  orientation?: "landscape" | "portrait";
  logoOverlay?: string;
  sealOverlay?: string;
}

type SampleImage = { src: string; side: string };

const sampleImages: Record<CardDesign["cardType"], SampleImage[]> = {
  "honorary-korean": [
    { src: "/images/cards/width/kor-tiger-front.webp", side: "앞면" },
    { src: "/images/cards/width/kor-tiger-back.webp", side: "뒷면" },
  ],
  "honorary-citizen": [
    { src: "/images/cards/width/city-dragon2-front.webp", side: "앞면" },
    { src: "/images/cards/width/city-dragon2-back.webp", side: "뒷면" },
  ],
  // 학생증은 방향(가로/세로)에 따라 아래 studentSamples에서 다시 선택한다.
  student: [
    { src: "/images/cards/length/student-pig-front.webp", side: "앞면" },
    { src: "/images/cards/length/student-pig-back.webp", side: "뒷면" },
  ],
  visitor: [
    { src: "/images/cards/example-visit.webp", side: "앞면" },
    { src: "/images/cards/example-visit-back.webp", side: "뒷면" },
  ],
};

// 학생증 전용: 가로형/세로형 견본 세트.
const studentSamples: Record<"landscape" | "portrait", SampleImage[]> = {
  landscape: [
    { src: "/images/cards/width/stu-pig-front.webp", side: "앞면" },
    { src: "/images/cards/width/stu-pig-back.webp", side: "뒷면" },
  ],
  portrait: [
    { src: "/images/cards/length/student-pig-front.webp", side: "앞면" },
    { src: "/images/cards/length/student-pig-back.webp", side: "뒷면" },
  ],
};

/** Right-hand sample preview shown alongside the application form. */
export function CardPreviewPanel({ design, orientation }: CardPreviewPanelProps) {
  // 학생증은 선택한 방향(기본 가로)에 맞는 견본을 보여준다.
  const resolvedOrientation = orientation ?? "landscape";
  const isStudent = design.cardType === "student";
  const images = isStudent ? studentSamples[resolvedOrientation] : sampleImages[design.cardType];

  // 세로형 두 장은 가로 한 줄로 나란히, 가로형은 세로로 쌓는다.
  const showHorizontal =
    images.length > 1 &&
    (design.cardType === "visitor" || (isStudent && resolvedOrientation === "portrait"));
  const rowModifier =
    images.length === 1
      ? " apply-preview__row--single"
      : showHorizontal
        ? " apply-preview__row--horizontal"
        : "";

  return (
    <aside className="apply-preview" aria-label="예시 카드">
      <p className="apply-preview__note-top">※ 특허출원에 의한 견본품</p>
      <div className={`apply-preview__row${rowModifier}`}>
        {images.map((image) => (
          <div className="apply-preview__card" key={image.src}>
            <img className="apply-preview__img" src={image.src} alt={`${design.name} 견본품 ${image.side}`} />
          </div>
        ))}
      </div>
      <p className="apply-preview__caption">
        이해를 돕기 위한 예시 이미지 입니다
        <br />
        실제 발급의 디자인과 구성은 변경될 수 있습니다
      </p>
    </aside>
  );
}
