import {
  api,
  type ApplicationStatus,
  type ApplicationType
} from "../../../services/api";

export const statusLabels: Record<ApplicationStatus, string> = {
  SUBMITTED: "접수", REVIEWING: "검토중", PHOTO_REJECTED: "사진반려", NAME_EDITING: "작명중",
  PRODUCTION_READY: "제작대기", PRODUCING: "제작중", COMPLETED: "발급완료", CANCELLED: "취소",
};

// 신청 명단 엑셀(xlsx)을 실제 API로 받아 브라우저 다운로드를 트리거한다. POST /api/admin/applications/export.
export async function downloadApplicationsExcel(ids: number[], type: ApplicationType) {
  const { blob, filename } = await api.exportApplications(ids, type);
  downloadBlob(blob, filename.endsWith(".xlsx") ? filename : "applications-export.xlsx");
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

// 이미 원격(예: presigned S3) URL이 있고 그 응답 자체에 Content-Disposition: attachment가 실려
// 있을 때 쓴다 — window.open(url, "_blank") 대신 숨긴 iframe으로 새 탭을 만들지 않는다.
// <a> 클릭 방식은 카드 앞/뒤처럼 같은 틱에 두 번 연달아 호출하면 먼저 시작한 다운로드가
// 뒤이은 두 번째 네비게이션에 취소당해(실제로 겪은 버그 — 앞면 요청은 나가지만 브라우저가
// "download" 이벤트로 완결짓지 못함) 하나만 저장됐다. iframe은 각자 별도 브라우징 컨텍스트라
// 여러 번 연달아 불러도 서로 취소하지 않는다.
export function triggerRemoteDownload(url: string) {
  const iframe = document.createElement("iframe");
  iframe.style.display = "none";
  iframe.src = url;
  document.body.appendChild(iframe);
  // 다운로드가 실제로 시작될 시간을 준 뒤 정리한다 — 너무 빨리 지우면 요청 자체가 취소될 수 있다.
  setTimeout(() => iframe.remove(), 60000);
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export function asDataUrl(base64: string) {
  return base64.startsWith("data:") ? base64 : `data:image/png;base64,${base64}`;
}


export function genderLabel(g?: "MALE" | "FEMALE"): string | undefined {
  return g === "MALE" ? "남성" : g === "FEMALE" ? "여성" : undefined;
}

