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
export interface ReviewImageItem { id: number; imageUrl: string; }
export interface ReviewDetail extends ReviewListItem { images: ReviewImageItem[]; next?: { id: number; title: string }; canEdit: boolean; canDelete: boolean; }
export interface ReviewListParams { cardTypeId?: number; hasPhoto?: boolean; searchType?: ReviewSearchType; keyword?: string; page?: number; size?: number; }
export interface ReviewWriteBody { title: string; applicationType: ApplicationType; cardTypeId: number; authorName: string; content: string; }
/** Update body: keepImageIds lists existing image ids to keep (in order); new files are appended. */
export interface ReviewUpdateBody extends ReviewWriteBody { keepImageIds: number[]; }

// ── Boards (공지사항 / FAQ) ─────────────────────────────────────
export type BoardType = "NOTICE" | "FAQ";
export interface BoardAttachment { id: number; originalFileName: string; url: string; }
export interface BoardListItem { id: number; boardType: BoardType; title: string; content: string; createdAt: string; }
export interface BoardDetail extends BoardListItem { attachments: BoardAttachment[]; next?: { id: number; title: string }; }
export interface BoardWriteBody { boardType: BoardType; title: string; content: string; keepAttachmentIds?: number[]; }

// ── Events (행사사업: 부스 / 협업) ───────────────────────────────
export type EventType = "BOOTH" | "COLLABORATION";
export interface EventImage { id: number; originalFileName: string; url: string; }
export interface EventListItem { id: number; eventType: EventType; title: string; eventDate?: string; eventDateText: string; place: string; host: string; cardLabel: string; content: string; thumbnailImageUrl?: string; company?: string; logoUrl?: string; displayOrder?: number; }
export interface EventDetail extends Omit<EventListItem, "displayOrder"> { images: EventImage[]; }
export interface EventAdminListItem extends EventListItem { visible: boolean; }
export interface EventAdminDetail extends EventAdminListItem { images: EventImage[]; }
export interface EventWriteBody { eventType: EventType; title: string; eventDate?: string; eventDateText: string; place: string; host: string; cardLabel: string; content: string; company?: string; visible?: boolean; displayOrder?: number; }
/** Update body: gallery edit via keepImageIds (kept ids in order; new files appended); removeThumbnail/removeLogo drop those. */
export interface EventUpdateBody extends EventWriteBody { removeThumbnail?: boolean; removeLogo?: boolean; keepImageIds?: number[]; }
/** Files for create/update: thumbnail (single), logo (single, COLLABORATION only), images (gallery, multiple). */
export interface EventFiles { thumbnail?: File; logo?: File; images?: File[]; }

// ── Applications (admin, 제작신청 관리) ──────────────────────────
export type ApplicationStatus =
  | "SUBMITTED" | "REVIEWING" | "PHOTO_REJECTED" | "NAME_EDITING"
  | "PRODUCTION_READY" | "PRODUCING" | "COMPLETED" | "CANCELLED";
export type PaymentStatus = "WAITING" | "CONFIRMED";
export type IssueType = "MOBILE" | "MOBILE_AND_PHYSICAL";
export interface AdminApplicationListItem {
  applicationId: number; applicationNumber: string; applicationType: ApplicationType;
  cardTypeId: number; cardTypeName: string; totalQuantity: number;
  status: ApplicationStatus; paymentStatus: PaymentStatus; createdAt: string;
}
export interface AdminApplicantSummary { name: string; email: string; phone: string; organizationName?: string; department?: string; }
export interface AdminReceiverSummary { name: string; phone: string; zipCode?: string; address?: string; detailAddress?: string; deliveryRequest?: string; organizationName?: string; department?: string; }
export interface AdminApplicationDetail {
  applicationId: number; applicationNumber: string; applicationType: ApplicationType;
  cardTypeId: number; cardTypeName: string; issueType: IssueType; totalQuantity: number;
  status: ApplicationStatus; paymentStatus: PaymentStatus;
  paymentGuidedAt?: string; paymentDueAt?: string; cancelledAt?: string; refundedAt?: string;
  cardReadyAt?: string; physicalDispatchedAt?: string; photoRejectReason?: string;
  applicant: AdminApplicantSummary; receiver?: AdminReceiverSummary;
  memberCount: number; createdAt: string;
}
export interface AdminApplicationMember {
  memberId: number; englishName?: string; nationality?: string; gender?: "MALE" | "FEMALE";
  birthDate?: string; birthTime?: string; birthRegion?: string;
  assignedName?: string; assignedHanja?: string;
}
export interface NameSelectionStat { name: string; hanja: string; count: number; }
export interface ApplicationStatusResult { applicationId: number; status: ApplicationStatus; }

// ── Inquiries (1:1 문의, 고객지원) ───────────────────────────────
export type InquiryCategory = "PRODUCTION" | "PAYMENT_AND_SHIPPING" | "CARD_ISSUANCE" | "EVENT_COLLABORATION" | "OTHER";
export type InquiryStatus = "PENDING" | "COMPLETED";
export interface InquiryListItem { id: number; category: InquiryCategory; title: string; name: string; email: string; phone: string; status: InquiryStatus; createdAt: string; }
export interface InquiryDetail { id: number; category: InquiryCategory; name: string; email: string; phone: string; title: string; content: string; status: InquiryStatus; answer?: string; answeredAt?: string; createdAt: string; }

// ── Account recovery (아이디/비밀번호 찾기) ──────────────────────
export interface RecoveryChallenge { requestId: string; expiresInSeconds: number; resendAfterSeconds: number; }

export const api = {
  getMe: () => request<ApiUser>("/api/users/me"),
  updateMe: (body: { name?: string; phone?: string; address?: string }) => request<ApiUser>("/api/users/me", { method: "PATCH", body: JSON.stringify(body) }),
  withdraw: () => request<void>("/api/users/me/withdraw", { method: "POST" }),
  agreeTerms: (body: { privacyAgreed: boolean; imageUploadAgreed: boolean; shippingAgreed: boolean }) => request("/api/auth/terms", { method: "POST", body: JSON.stringify(body) }),
  refresh: () => request<void>("/api/auth/refresh", { method: "POST" }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
  loginWithPassword: (email: string, password: string) => request<ApiUser>("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  createApplication: (form: FormData, bulk = false) => request<ApplicationResult>(`/api/applications${bulk ? "/bulk" : ""}`, { method: "POST", body: form }),
  lookupApplication: (body: { method: "application" | "card"; keyValue: string; phone?: string; email?: string }) => request<LookupResult>("/api/applications/lookup", { method: "POST", body: JSON.stringify(body) }),
  reuploadPhoto: (id: number, form: FormData) => request(`/api/applications/${id}/photo`, { method: "PATCH", body: form }),
  getCardDownload: (id: number) => request<CardDownload>(`/api/applications/${id}/cards/download`),
  oauthUrl: (provider: "google" | "naver") => `${API_BASE_URL}/oauth2/authorization/${provider}`,

  // Reviews
  listReviews: (params: ReviewListParams = {}) => request<PageResponse<ReviewListItem>>(`/api/reviews${qs({ ...params })}`),
  listMyReviews: (params: { page?: number; size?: number } = {}) => request<PageResponse<ReviewListItem>>(`/api/my/reviews${qs({ ...params })}`),
  getReview: (id: number) => request<ReviewDetail>(`/api/reviews/${id}`),
  createReview: (body: ReviewWriteBody, images: File[] = []) => request<{ id: number }>("/api/reviews", { method: "POST", body: multipart(body, images.map((file) => ({ name: "image", file }))) }),
  updateReview: (id: number, body: ReviewUpdateBody, images: File[] = []) => request<void>(`/api/reviews/${id}`, { method: "PATCH", body: multipart(body, images.map((file) => ({ name: "image", file }))) }),
  deleteReview: (id: number) => request<void>(`/api/reviews/${id}`, { method: "DELETE" }),

  // Boards (notices / FAQ)
  listBoards: (params: { type?: BoardType; page?: number; size?: number } = {}) => request<PageResponse<BoardListItem>>(`/api/boards${qs({ ...params })}`),
  getBoard: (id: number) => request<BoardDetail>(`/api/boards/${id}`),
  createBoard: (body: BoardWriteBody, attachments: File[] = []) => request<{ id: number }>("/api/admin/boards", { method: "POST", body: multipart(body, attachments.map((file) => ({ name: "attachments", file }))) }),
  updateBoard: (id: number, body: BoardWriteBody, attachments: File[] = []) => request<void>(`/api/admin/boards/${id}`, { method: "PATCH", body: multipart(body, attachments.map((file) => ({ name: "attachments", file }))) }),
  deleteBoard: (id: number) => request<void>(`/api/admin/boards/${id}`, { method: "DELETE" }),

  // Events (public)
  listEvents: (params: { type: EventType; page?: number; size?: number }) => request<PageResponse<EventListItem>>(`/api/events${qs({ ...params })}`),
  getEvent: (id: number) => request<EventDetail>(`/api/events/${id}`),
  // Applications (admin, 제작신청 관리) — 읽기 전용(목록/상세). 상태 전이·이름확정·엑셀출력은 미구현(문서 참고).
  listAdminApplications: (params: { status?: ApplicationStatus; page?: number; size?: number } = {}) => request<PageResponse<AdminApplicationListItem>>(`/api/admin/applications${qs({ ...params })}`),
  getAdminApplication: (id: number) => request<AdminApplicationDetail>(`/api/admin/applications/${id}`),
  getAdminApplicationMembers: (id: number) => request<AdminApplicationMember[]>(`/api/admin/applications/${id}/members`),
  // 인앱 작명 확정 — 서버에 저장(멤버 이름 반영 + 선택이력 +1). 프론트 localStorage 미사용.
  saveMemberName: (applicationId: number, memberId: number, body: { name: string; hanja?: string; reading?: string; meaning?: string }) =>
    request<void>(`/api/admin/applications/${applicationId}/members/${memberId}/name`, { method: "POST", body: JSON.stringify(body) }),
  getNameSelectionStats: () => request<NameSelectionStat[]>("/api/admin/name-selection-stats"),
  // 신청 상태 전이(관리자) — 백엔드 존재 엔드포인트 연결.
  completeNaming: (id: number) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/complete-naming`, { method: "POST" }),
  startProducing: (id: number) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/start-producing`, { method: "POST" }),
  markCardReady: (id: number) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/card-ready`, { method: "POST" }),
  rejectApplicationPhoto: (id: number, reason: string) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/reject-photo`, { method: "POST", body: JSON.stringify({ reason }) }),
  dispatchApplication: (id: number, trackingNumber: string) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/dispatch`, { method: "POST", body: JSON.stringify({ trackingNumber }) }),

  // Inquiries (1:1 문의) — 관리자 목록/상세/답변/상태
  listAdminInquiries: () => request<InquiryListItem[]>("/api/admin/inquiries"),
  getAdminInquiry: (id: number) => request<InquiryDetail>(`/api/admin/inquiries/${id}`),
  answerInquiry: (id: number, answer: string) => request<void>(`/api/admin/inquiries/${id}/answer`, { method: "PATCH", body: JSON.stringify({ answer }) }),
  updateInquiryStatus: (id: number, status: InquiryStatus) => request<void>(`/api/admin/inquiries/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),

  // Events (admin)
  listAdminEvents: (params: { type?: EventType; visible?: boolean; page?: number; size?: number } = {}) => request<PageResponse<EventAdminListItem>>(`/api/admin/events${qs({ ...params })}`),
  getAdminEvent: (id: number) => request<EventAdminDetail>(`/api/admin/events/${id}`),
  createEvent: (body: EventWriteBody, files: EventFiles = {}) => request<{ id: number }>("/api/admin/events", { method: "POST", body: eventMultipart(body, files) }),
  updateEvent: (id: number, body: EventUpdateBody, files: EventFiles = {}) => request<void>(`/api/admin/events/${id}`, { method: "PATCH", body: eventMultipart(body, files) }),
  deleteEvent: (id: number) => request<void>(`/api/admin/events/${id}`, { method: "DELETE" }),

  // Account recovery (아이디/비밀번호 찾기) — 2-step: request → confirm(code)
  requestIdRecovery: (body: { name: string; phone: string }) => request<RecoveryChallenge>("/api/auth/recovery/id/request", { method: "POST", body: JSON.stringify(body) }),
  confirmIdRecovery: (body: { requestId: string; code: string }) => request<{ maskedEmail: string }>("/api/auth/recovery/id/confirm", { method: "POST", body: JSON.stringify(body) }),
  requestPasswordRecovery: (body: { email: string }) => request<RecoveryChallenge>("/api/auth/recovery/password/request", { method: "POST", body: JSON.stringify(body) }),
  confirmPasswordRecovery: (body: { requestId: string; code: string; newPassword: string }) => request<void>("/api/auth/recovery/password/confirm", { method: "POST", body: JSON.stringify(body) }),
};

/** Build the events multipart body: JSON `request` + thumbnail/logo (single) + images (gallery). */
function eventMultipart(body: unknown, files: EventFiles): FormData {
  const parts: Array<{ name: string; file: File }> = [];
  if (files.thumbnail) parts.push({ name: "thumbnail", file: files.thumbnail });
  if (files.logo) parts.push({ name: "logo", file: files.logo });
  for (const file of files.images ?? []) parts.push({ name: "images", file });
  return multipart(body, parts);
}
