export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  errorMessage: string | null;
}

export class ApiError extends Error {
  constructor(message: string, public status: number, public code?: string | null) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: init.body instanceof FormData ? init.headers : { "Content-Type": "application/json", ...init.headers },
  });
  if (response.status === 401 && !retried && path !== "/api/auth/refresh") {
    const refreshed = await fetch(`${API_BASE_URL}/api/auth/refresh`, { method: "POST", credentials: "include" });
    if (refreshed.ok) return request<T>(path, init, true);
  }
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !payload?.success) {
    throw new ApiError(payload?.errorMessage || `API 요청에 실패했습니다. (${response.status})`, response.status, payload?.errorCode);
  }
  return payload.data;
}

export interface ApiUser { id: number; name: string; email: string; role: "USER" | "ADMIN"; phone?: string; address?: string; }
export interface ApplicationResult { applicationId: number; applicationNumber: string; status: string; paymentStatus?: string; createdAt: string; totalQuantity?: number; }
export interface LookupResult { applicationId: number; applicationNumber: string; applicantNameMasked: string; cardType: string; status: string; photoRejectReason?: string; submittedAt: string; }
export interface CardDownload { applicationId: number; applicationType: "INDIVIDUAL" | "GROUP"; cardFrontUrl?: string; cardBackUrl?: string; downloadUrl?: string; expiresAt: string; }

export const api = {
  getMe: () => request<ApiUser>("/api/users/me"),
  updateMe: (body: { name?: string; phone?: string; address?: string }) => request<ApiUser>("/api/users/me", { method: "PATCH", body: JSON.stringify(body) }),
  withdraw: () => request<void>("/api/users/me/withdraw", { method: "POST" }),
  agreeTerms: (body: { privacyAgreed: boolean; imageUploadAgreed: boolean; shippingAgreed: boolean }) => request("/api/auth/terms", { method: "POST", body: JSON.stringify(body) }),
  refresh: () => request<void>("/api/auth/refresh", { method: "POST" }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
  createApplication: (form: FormData, bulk = false) => request<ApplicationResult>(`/api/applications${bulk ? "/bulk" : ""}`, { method: "POST", body: form }),
  lookupApplication: (body: { method: "application" | "card"; keyValue: string; phone?: string; email?: string }) => request<LookupResult>("/api/applications/lookup", { method: "POST", body: JSON.stringify(body) }),
  reuploadPhoto: (id: number, form: FormData) => request(`/api/applications/${id}/photo`, { method: "PATCH", body: form }),
  getCardDownload: (id: number) => request<CardDownload>(`/api/applications/${id}/cards/download`),
  oauthUrl: (provider: "google" | "naver") => `${API_BASE_URL}/oauth2/authorization/${provider}`,
};
