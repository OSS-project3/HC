// 모바일 카드 이미지 다운로드 유틸.
// - buildCompositeCardBlob: 앞·뒷면을 브랜드 배경/타이틀과 함께 한 장으로 합성한 PNG 생성.
// - downloadImageFile: 개별 원본 이미지(앞면/뒷면)를 파일로 저장.

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "anonymous"; // /public 정적 파일은 동일 출처라 문제 없음.
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`이미지를 불러오지 못했습니다: ${src}`));
    img.src = src;
  });
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  const radius = Math.min(r, w / 2, h / 2);
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.arcTo(x + w, y, x + w, y + h, radius);
  ctx.arcTo(x + w, y + h, x, y + h, radius);
  ctx.arcTo(x, y + h, x, y, radius);
  ctx.arcTo(x, y, x + w, y, radius);
  ctx.closePath();
}

/** 카드 한 장을 그림자 + 살짝 둥근 모서리로 그린다. */
function drawCard(ctx: CanvasRenderingContext2D, img: HTMLImageElement, x: number, y: number, w: number, h: number) {
  const radius = 12; // 살짝만.
  // 1) 그림자용 흰 베이스.
  ctx.save();
  ctx.shadowColor = "rgba(20, 30, 48, 0.22)";
  ctx.shadowBlur = 26;
  ctx.shadowOffsetY = 12;
  ctx.fillStyle = "#ffffff";
  roundRect(ctx, x, y, w, h, radius);
  ctx.fill();
  ctx.restore();
  // 2) 둥근 모서리로 클립 후 이미지 그리기.
  ctx.save();
  roundRect(ctx, x, y, w, h, radius);
  ctx.clip();
  ctx.drawImage(img, x, y, w, h);
  ctx.restore();
}

interface CompositeOptions {
  title?: string;
}

/** 앞면 + 뒷면을 한 장의 PNG(Blob)로 합성한다. */
export async function buildCompositeCardBlob(
  frontUrl: string,
  backUrl: string,
  options: CompositeOptions = {},
): Promise<Blob> {
  const { title = "모바일 신분증" } = options;
  const [front, back] = await Promise.all([loadImage(frontUrl), loadImage(backUrl)]);

  const aspect = front.naturalWidth / front.naturalHeight || 1.58;
  const isLandscape = aspect >= 1;

  const cardW = 560;
  const cardH = Math.round(cardW / aspect);
  const pad = 56;
  const titleH = 104;
  const footerH = 28;
  const gap = 40;

  const contentW = isLandscape ? cardW : cardW * 2 + gap;
  const contentH = isLandscape ? cardH * 2 + gap : cardH;
  const canvasW = contentW + pad * 2;
  const canvasH = pad + titleH + contentH + footerH + pad;

  const scale = 2; // 선명하게 2배 해상도로 내보낸다.
  const canvas = document.createElement("canvas");
  canvas.width = canvasW * scale;
  canvas.height = canvasH * scale;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("캔버스를 사용할 수 없습니다.");
  ctx.scale(scale, scale);

  // 배경 그라디언트.
  const bg = ctx.createLinearGradient(0, 0, 0, canvasH);
  bg.addColorStop(0, "#fbf7ef");
  bg.addColorStop(1, "#f0e6d5");
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, canvasW, canvasH);

  // 타이틀.
  ctx.textAlign = "center";
  ctx.fillStyle = "#263d5b";
  ctx.font = "700 34px 'Noto Serif KR', 'Nanum Myeongjo', serif";
  ctx.fillText(title, canvasW / 2, pad + 46);
  // 타이틀 밑 장식선.
  const lineW = 72;
  ctx.strokeStyle = "rgba(38, 61, 91, 0.45)";
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(canvasW / 2 - lineW / 2, pad + 70);
  ctx.lineTo(canvasW / 2 + lineW / 2, pad + 70);
  ctx.stroke();

  // 카드 배치.
  const top = pad + titleH;
  if (isLandscape) {
    const x = (canvasW - cardW) / 2;
    drawCard(ctx, front, x, top, cardW, cardH);
    drawCard(ctx, back, x, top + cardH + gap, cardW, cardH);
  } else {
    const startX = (canvasW - contentW) / 2;
    drawCard(ctx, front, startX, top, cardW, cardH);
    drawCard(ctx, back, startX + cardW + gap, top, cardW, cardH);
  }

  return await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error("이미지 생성에 실패했습니다."))), "image/png");
  });
}

/** Blob 또는 URL을 파일로 저장한다. */
function triggerDownload(url: string, filename: string) {
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
}

/** 합성 PNG를 다운로드한다. */
export async function downloadCompositeCard(
  frontUrl: string,
  backUrl: string,
  filename: string,
  options?: CompositeOptions,
) {
  const blob = await buildCompositeCardBlob(frontUrl, backUrl, options);
  const url = URL.createObjectURL(blob);
  try {
    triggerDownload(url, filename);
  } finally {
    URL.revokeObjectURL(url);
  }
}

/** 원본 이미지(앞면/뒷면)를 그대로 파일로 저장한다. */
export async function downloadImageFile(url: string, filename: string) {
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error("네트워크 오류");
    const blob = await res.blob();
    const objUrl = URL.createObjectURL(blob);
    try {
      triggerDownload(objUrl, filename);
    } finally {
      URL.revokeObjectURL(objUrl);
    }
  } catch {
    // fetch 실패 시 직접 링크로 폴백(동일 출처면 대부분 동작).
    triggerDownload(url, filename);
  }
}
