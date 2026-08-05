interface KakaoPostcodeResult {
  zonecode: string;
  roadAddress: string;
  jibunAddress: string;
  userSelectedType: "R" | "J";
}

declare global {
  interface Window {
    kakao?: {
      Postcode: new (options: { oncomplete: (data: KakaoPostcodeResult) => void; onclose?: () => void }) => { open: (options?: { popupTitle?: string; width?: number; height?: number; left?: number; top?: number }) => void };
    };
  }
}

let loader: Promise<void> | null = null;

function loadPostcodeScript() {
  if (window.kakao?.Postcode) return Promise.resolve();
  if (loader) return loader;
  loader = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-kakao-postcode]');
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("postcode-script-error")), { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src = "https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    script.dataset.kakaoPostcode = "true";
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("postcode-script-error"));
    document.head.appendChild(script);
  });
  return loader;
}

export async function openPostcodeSearch(onComplete: (postalCode: string, address: string) => void) {
  await loadPostcodeScript();
  if (!window.kakao?.Postcode) throw new Error("postcode-unavailable");
  const width = Math.min(500, window.screen.availWidth - 40);
  const height = Math.min(620, window.screen.availHeight - 80);
  const screenLeft = window.screenLeft ?? window.screenX;
  const screenTop = window.screenTop ?? window.screenY;
  const viewportLeft = screenLeft + Math.max(0, (window.outerWidth - width) / 2);
  const viewportTop = screenTop + Math.max(0, (window.outerHeight - height) / 2);

  new window.kakao.Postcode({
    oncomplete: (data) => {
      const address = data.userSelectedType === "R" ? data.roadAddress : data.jibunAddress;
      onComplete(data.zonecode, address);
    },
  }).open({
    popupTitle: "한글과 세종 우편번호 검색",
    width,
    height,
    left: Math.round(viewportLeft),
    top: Math.round(viewportTop),
  });
}

export {};
