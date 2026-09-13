// ApplicationPhotoValidator 요건(≥300×400, PNG, magic number 일치)을 만족하는 얼굴사진
// fixture를 1회 생성한다. 실행: node e2e/fixtures/generate-face-photo.mjs
import sharp from "sharp";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outPath = path.join(__dirname, "face-photo.png");

await sharp({
  create: {
    width: 320,
    height: 420,
    channels: 3,
    background: { r: 210, g: 200, b: 190 },
  },
})
  .png()
  .toFile(outPath);

console.log("생성 완료:", outPath);
