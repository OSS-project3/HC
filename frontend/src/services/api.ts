export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

/** Bean-Validation / bulk-Excel field error detail (see spec §2.3). */
export interface ValidationErrorDetail {
  row: number | null;
  field: string;
  code: string;
  message: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  errorMessage: string | null;
  errors?: ValidationErrorDetail[];
}

export class ApiError extends Error {
  constructor(message: string, public status: number, public code?: string | null, public errors?: ValidationErrorDetail[]) {
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
    throw new ApiError(payload?.errorMessage || `API 요청에 실패했습니다. (${response.status})`, response.status, payload?.errorCode, payload?.errors);
  }
  return payload.data;
}

export interface ApiUser { id: number; name: string; email: string; role: "USER" | "ADMIN"; phone?: string; address?: string; }
export interface ApplicationResult { applicationId: number; applicationNumber: string; status: string; paymentStatus?: string; createdAt: string; totalQuantity?: number; }
export interface LookupResult { applicationId: number; applicationNumber: string; applicantNameMasked: string; cardType: string; status: string; photoRejectReason?: string; submittedAt: string; }
export interface CardDownload { applicationId: number; applicationType: "INDIVIDUAL" | "GROUP"; cardFrontUrl?: string; cardBackUrl?: string; downloadUrl?: string; expiresAt: string; }

/** Common paginated envelope for list endpoints. */
export interface PageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export type ApplicationType = "INDIVIDUAL" | "GROUP";
export interface CardTypeSummary { id: number; name: string; }

/** Build a `?a=1&b=2` string, skipping null/undefined/"" values. */
function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") search.append(key, String(value));
  }
  const str = search.toString();
  return str ? `?${str}` : "";
}

/** Build a multipart body with a JSON `request` part plus named file parts. */
function multipart(request: unknown, files: Array<{ name: string; file: File }> = []): FormData {
  const form = new FormData();
  form.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
  for (const { name, file } of files) form.append(name, file);
  return form;
}

// ── Reviews (후기) ──────────────────────────────────────────────
export type ReviewSearchType = "ALL" | "TITLE" | "CONTENT" | "AUTHOR";
export interface ReviewListItem { id: number; imageUrl?: string; applicationType: ApplicationType; cardType: CardTypeSummary; title: string; content: string; authorName: string; createdAt: string; }
export interface ReviewDetail extends ReviewListItem { next?: { id: number; title: string }; canEdit: boolean; canDelete: boolean; }
export interface ReviewListParams { cardTypeId?: number; hasPhoto?: boolean; searchType?: ReviewSearchType; keyword?: string; page?: number; size?: number; }
export interface ReviewWriteBody { title: string; applicationType: ApplicationType; cardTypeId: number; authorName: string; content: string; }

// ── Boards (공지사항 / FAQ) ─────────────────────────────────────
export type BoardType = "NOTICE" | "FAQ";
export interface BoardAttachment { id: number; originalFileName: string; url: string; }
export interface BoardListItem { id: number; boardType: BoardType; title: string; content: string; createdAt: string; }
export interface BoardDetail extends BoardListItem { attachments: BoardAttachment[]; next?: { id: number; title: string }; }
export interface BoardWriteBody { boardType: BoardType; title: string; content: string; keepAttachmentIds?: number[]; }

// ── Events (행사사업: 부스 / 협업) ───────────────────────────────
export type EventType = "BOOTH" | "COLLABORATION";
export interface EventImage { id: number; originalFileName: string; url: string; }
export interface EventListItem { id: number; eventType: EventType; title: string; eventDate?: string; eventDateText: string; place: string; host: string; cardLabel: string; content: string; thumbnailImageUrl?: string; displayOrder?: number; }
export interface EventDetail extends Omit<EventListItem, "displayOrder"> { images: EventImage[]; }
export interface EventWriteBody { eventType: EventType; title: string; eventDate?: string; eventDateText: string; place: string; host: string; cardLabel: string; content: string; visible?: boolean; displayOrder?: number; }

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

  // Reviews
  listReviews: (params: ReviewListParams = {}) => request<PageResponse<ReviewListItem>>(`/api/reviews${qs({ ...params })}`),
  listMyReviews: (params: { page?: number; size?: number } = {}) => request<PageResponse<ReviewListItem>>(`/api/my/reviews${qs({ ...params })}`),
  getReview: (id: number) => request<ReviewDetail>(`/api/reviews/${id}`),
  createReview: (body: ReviewWriteBody, image?: File) => request<{ id: number }>("/api/reviews", { method: "POST", body: multipart(body, image ? [{ name: "image", file: image }] : []) }),
  updateReview: (id: number, body: ReviewWriteBody & { removeImage: boolean }, image?: File) => request<void>(`/api/reviews/${id}`, { method: "PATCH", body: multipart(body, image ? [{ name: "image", file: image }] : []) }),
  deleteReview: (id: number) => request<void>(`/api/reviews/${id}`, { method: "DELETE" }),

  // Boards (notices / FAQ)
  listBoards: (params: { type?: BoardType; page?: number; size?: number } = {}) => request<PageResponse<BoardListItem>>(`/api/boards${qs({ ...params })}`),
  getBoard: (id: number) => request<BoardDetail>(`/api/boards/${id}`),
  createBoard: (body: BoardWriteBody, attachments: File[] = []) => request<{ id: number }>("/api/admin/boards", { method: "POST", body: multipart(body, attachments.map((file) => ({ name: "attachments", file }))) }),
  updateBoard: (id: number, body: BoardWriteBody, attachments: File[] = []) => request<void>(`/api/admin/boards/${id}`, { method: "PATCH", body: multipart(body, attachments.map((file) => ({ name: "attachments", file }))) }),
  deleteBoard: (id: number) => request<void>(`/api/admin/boards/${id}`, { method: "DELETE" }),

  // Events
  listEvents: (params: { type: EventType; page?: number; size?: number }) => request<PageResponse<EventListItem>>(`/api/events${qs({ ...params })}`),
  getEvent: (id: number) => request<EventDetail>(`/api/events/${id}`),
  createEvent: (body: EventWriteBody, thumbnail?: File, images: File[] = []) => request<{ id: number }>("/api/admin/events", { method: "POST", body: multipart(body, [...(thumbnail ? [{ name: "thumbnail", file: thumbnail }] : []), ...images.map((file) => ({ name: "images", file }))]) }),
  updateEvent: (id: number, body: EventWriteBody, thumbnail?: File) => request<void>(`/api/admin/events/${id}`, { method: "PATCH", body: multipart(body, thumbnail ? [{ name: "thumbnail", file: thumbnail }] : []) }),
  deleteEvent: (id: number) => request<void>(`/api/admin/events/${id}`, { method: "DELETE" }),
};
