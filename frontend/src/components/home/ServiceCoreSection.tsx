/**
 * "서비스 핵심" — two traditional-window cards. The lattice shutters open on
 * hover/focus to reveal the label, echoing the mock-up's 창문 style.
 */
export function ServiceCoreSection() {
  return (
    <section className="service-core page-container">
      <p className="eyebrow">서비스 핵심</p>
      <h2 className="section-title service-core__title">
        한국 이름과 명예한국인증·명예시민증·학생증·방문증을 한 번에
      </h2>
      <p className="section-lead service-core__lead">
        정보 입력, 한국 이름 추천, 명예 한국인증·명예 시민증·학생증·방문증 미리보기까지 하나의 흐름으로 구성했습니다.
        <br />
        관광객이 한국 문화 체험을 개인화된 기념 콘텐츠로 남길 수 있습니다.
      </p>

      <div className="service-core__windows">
        <WindowCard label="각종 한국 이름 풀이" />
        <WindowCard label="한국 이름의 작명 원리" />
      </div>
    </section>
  );
}

function WindowCard({ label }: { label: string }) {
  return (
    <button className="window" aria-label={`${label} — 열어보기`}>
      <span className="window__label">{label}</span>
      <span className="window__shutter window__shutter--left" aria-hidden="true" />
      <span className="window__shutter window__shutter--right" aria-hidden="true" />
    </button>
  );
}
