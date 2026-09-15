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

export function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export function asDataUrl(base64: string) {
  return base64.startsWith("data:") ? base64 : `data:image/png;base64,${base64}`;
}


export function genderLabel(g?: "MALE" | "FEMALE"): string | undefined {
  return g === "MALE" ? "남성" : g === "FEMALE" ? "여성" : undefined;
}

