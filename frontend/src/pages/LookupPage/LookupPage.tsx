// 조회 페이지: 회원가입 없이, 발급받은 본인의 모바일 카드만 확인하는 독립 페이지.
// 전화번호+이메일 또는 카드번호를 입력하면 본인 카드가 표시되고, 클릭하면 뒤집혀 뒷면이 보인다.
import { useState } from "react";
import { Button } from "../../components/ui/Button";
import { FlipCard } from "../../components/ui/FlipCard";
import { showToast } from "../../components/ui/toast";
import { loadApplications } from "../../data/adminMock";
import { api } from "../../services/api";
import { downloadCompositeCard, downloadImageFile } from "../../lib/cardDownload";
import "./LookupPage.css";

type LookupMethod = "contact" | "card";

const DEMO_PHONE = "01012345678";
const DEMO_EMAIL = "qwer@gmail.com";
const DEMO_CARD_NUMBER = "ROK-12345-6789";

// 테스트용: 카드번호에 admin-test 를 입력하면 실제 발급 여부와 무관하게 데모 카드가 뜬다.
// (실제 서비스에서는 API 조회 결과로 대체된다.)
const TEST_CARD_NUMBER = "ADMIN-TEST";
const DEMO_CARD_FRONT = "/images/cards/width/kor-mouse-front.webp";
const DEMO_CARD_BACK = "/images/cards/width/kor-mouse-back.webp";

interface FoundCard {
  frontUrl: string;
  backUrl: string;
}

function normalizePhone(value: string) {
  return value.replace(/\D/g, "");
}

function normalizeCardNumber(value: string) {
  return value.trim().toUpperCase().replace(/\s/g, "");
}

// 앞면 이미지 파일명(...-front.jpg)에서 매칭되는 뒷면(...-back.jpg) 경로를 유도한다.
// API가 뒷면 URL을 따로 주지 않아도 앞면과 짝이 맞는 뒷면이 뜨도록 보장한다.
function backFromFront(frontUrl: string) {
  return frontUrl.replace(/-front(\.[^./]+)$/i, "-back$1");
}

export function LookupPage() {
  const [method, setMethod] = useState<LookupMethod>("contact");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [cardNumber, setCardNumber] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [card, setCard] = useState<FoundCard | null>(null);
  const [downloading, setDownloading] = useState(false);

  const changeMethod = (next: LookupMethod) => {
    setMethod(next);
    setError(null);
  };

  const resetLookup = () => {
    setCard(null);
    setError(null);
  };

  // 이미지 URL에서 확장자를 뽑아 파일명에 붙인다(없으면 png).
  const extOf = (url: string) => (url.split("?")[0].match(/\.([a-z0-9]+)$/i)?.[1] ?? "png").toLowerCase();

  // 앞·뒷면을 합친 한 장의 PNG로 다운로드.
  const downloadComposite = async () => {
    if (!card || downloading) return;
    setDownloading(true);
    try {
      await downloadCompositeCard(card.frontUrl, card.backUrl, "모바일카드.png");
    } catch {
      showToast("이미지를 만드는 중 문제가 발생했습니다. 다시 시도해 주세요.");
    } finally {
      setDownloading(false);
    }
  };

  // 앞면/뒷면 원본 이미지를 각각 다운로드.
  const downloadSide = async (side: "front" | "back") => {
    if (!card) return;
    const url = side === "front" ? card.frontUrl : card.backUrl;
    try {
      await downloadImageFile(url, `모바일카드-${side === "front" ? "앞면" : "뒷면"}.${extOf(url)}`);
    } catch {
      showToast("다운로드 중 문제가 발생했습니다. 다시 시도해 주세요.");
    }
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();

    // 1) 테스트 카드번호(admin-test)는 항상 데모 카드를 보여준다.
    if (method === "card" && normalizeCardNumber(cardNumber) === TEST_CARD_NUMBER) {
      setError(null);
      setCard({ frontUrl: DEMO_CARD_FRONT, backUrl: DEMO_CARD_BACK });
      return;
    }

    // 2) 실제 조회는 API로 처리한다. 발급 카드 이미지를 받아 그대로 표시한다.
    try {
      const result = await api.lookupApplication({
        method: method === "card" ? "card" : "application",
        keyValue: method === "card" ? cardNumber : phone,
        phone: phone || undefined,
        email: email || undefined,
      });
      const download = await api.getCardDownload(result.applicationId).catch(() => null);
      const frontUrl = download?.cardFrontUrl || DEMO_CARD_FRONT;
      setError(null);
      setCard({
        frontUrl,
        // 뒷면 URL이 없으면 앞면과 매칭되는 뒷면을 유도해 사용한다.
        backUrl: download?.cardBackUrl || backFromFront(frontUrl),
      });
      return;
    } catch {
      // API 미연동 구간에서는 아래 로컬 데모 데이터로 폴백한다.
    }

    // 3) 로컬 데모 데이터 폴백.
    const contactMatches =
      normalizePhone(phone) === DEMO_PHONE && email.trim().toLowerCase() === DEMO_EMAIL;
    const savedApplicationMatches = loadApplications().some(
      (application) =>
        normalizePhone(application.phone) === normalizePhone(phone) &&
        application.applicantEmail?.toLowerCase() === email.trim().toLowerCase(),
    );
    const cardMatches = normalizeCardNumber(cardNumber) === normalizeCardNumber(DEMO_CARD_NUMBER);

    if ((method === "contact" && (contactMatches || savedApplicationMatches)) || (method === "card" && cardMatches)) {
      setError(null);
      setCard({ frontUrl: DEMO_CARD_FRONT, backUrl: DEMO_CARD_BACK });
      return;
    }

    setError(
      method === "contact"
        ? "입력하신 전화번호와 이메일에 해당하는 발급 카드를 찾을 수 없습니다."
        : "입력하신 카드번호에 해당하는 발급 카드를 찾을 수 없습니다.",
    );
  };

  return (
    <section className="lookup page-container">
      <header className="subpage-hero lookup__hero">
        <p className="eyebrow">조회</p>
        <h1 className="subpage-hero__title">모바일 카드 조회</h1>
        <p className="section-lead">발급받은 본인의 모바일 카드를 확인할 수 있습니다.</p>
      </header>

      {card ? (
        <div className="lookup__cardview">
          <FlipCard frontUrl={card.frontUrl} backUrl={card.backUrl} />
          <div className="lookup__downloads">
            <button type="button" className="lookup__download-btn" onClick={downloadComposite} disabled={downloading}>
              {downloading ? "이미지 생성 중…" : "카드 이미지 다운로드"}
            </button>
            <div className="lookup__download-split">
              <button type="button" className="lookup__download-link" onClick={() => downloadSide("front")}>
                앞면 다운로드
              </button>
              <span className="lookup__download-sep" aria-hidden="true">·</span>
              <button type="button" className="lookup__download-link" onClick={() => downloadSide("back")}>
                뒷면 다운로드
              </button>
            </div>
          </div>
          <button type="button" className="lookup__reset" onClick={resetLookup}>
            다른 카드 조회하기
          </button>
        </div>
      ) : (
        <div className="lookup__panel">
          <div className="lookup__tabs" role="tablist" aria-label="모바일 카드 조회 방법">
            <button
              type="button"
              role="tab"
              aria-selected={method === "contact"}
              className={`lookup__tab${method === "contact" ? " lookup__tab--active" : ""}`}
              onClick={() => changeMethod("contact")}
            >
              전화번호 · 이메일
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={method === "card"}
              className={`lookup__tab${method === "card" ? " lookup__tab--active" : ""}`}
              onClick={() => changeMethod("card")}
            >
              카드번호
            </button>
          </div>

          <form className="lookup__form" onSubmit={submit}>
            {method === "contact" ? (
              <>
                <label className="field">
                  <span className="field__label">전화번호<span className="req">*</span></span>
                  <input
                    className="field__input"
                    value={phone}
                    onChange={(event) => setPhone(event.target.value)}
                    type="tel"
                    inputMode="tel"
                    autoComplete="tel"
                    placeholder="010-1234-5678"
                    required
                  />
                </label>
                <label className="field">
                  <span className="field__label">이메일<span className="req">*</span></span>
                  <input
                    className="field__input"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    autoComplete="email"
                    placeholder="hong@example.com"
                    required
                  />
                </label>
              </>
            ) : (
              <label className="field">
                <span className="field__label">발급 카드번호<span className="req">*</span></span>
                <input
                  className="field__input"
                  value={cardNumber}
                  onChange={(event) => setCardNumber(event.target.value)}
                  placeholder="ROK-12345-6789"
                  autoComplete="off"
                  required
                />
              </label>
            )}

            {error && <p className="field-error" role="alert">{error}</p>}

            <Button type="submit" block>모바일 카드 확인</Button>
            <p className="lookup__note">
              {method === "contact"
                ? "발급 시 등록한 전화번호와 이메일을 모두 입력해 주세요."
                : "발급받은 카드에 표시된 카드번호를 입력해 주세요."}
            </p>
          </form>
        </div>
      )}
    </section>
  );
}
