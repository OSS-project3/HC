import { getLanguage } from "../features/i18n/LanguageContext";
import { resolveServerErrorMessage } from "../features/i18n/serverErrors";

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

/** Language header so the backend can localize translatable content (boards, events, reviews …). */
function languageHeaders(init: RequestInit): HeadersInit {
  const base: Record<string, string> = { "Accept-Language": getLanguage() };
  if (!(init.body instanceof FormData)) base["Content-Type"] = "application/json";
  return { ...base, ...init.headers };
}

async function request<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: languageHeaders(init),
  });
  if (response.status === 401 && !retried && path !== "/api/auth/refresh") {
    const refreshed = await fetch(`${API_BASE_URL}/api/auth/refresh`, { method: "POST", credentials: "include" });
    if (refreshed.ok) return request<T>(path, init, true);
  }
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !payload?.success) {
    const language = getLanguage();
    const fallback = language === "en" ? `The request failed. (${response.status})` : `API 요청에 실패했습니다. (${response.status})`;
    throw new ApiError(resolveServerErrorMessage(language, payload?.errorCode, payload?.errorMessage, fallback), response.status, payload?.errorCode, payload?.errors);
  }
  return payload.data;
}

/** request()와 동일한 인증(쿠키+401 refresh) 처리를 하되, JSON envelope이 아니라 바이너리(파일)를 반환한다. */
async function requestFile(path: string, init: RequestInit = {}, retried = false): Promise<{ blob: Blob; filename: string }> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: languageHeaders(init),
  });
  if (response.status === 401 && !retried && path !== "/api/auth/refresh") {
    const refreshed = await fetch(`${API_BASE_URL}/api/auth/refresh`, { method: "POST", credentials: "include" });
    if (refreshed.ok) return requestFile(path, init, true);
  }
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as ApiEnvelope<unknown> | null;
    const language = getLanguage();
    const fallback = language === "en" ? `The file request failed. (${response.status})` : `파일 요청에 실패했습니다. (${response.status})`;
    throw new ApiError(resolveServerErrorMessage(language, payload?.errorCode, payload?.errorMessage, fallback), response.status, payload?.errorCode, payload?.errors);
  }
  const disposition = response.headers.get("Content-Disposition") || "";
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  return { blob: await response.blob(), filename: match?.[1] ? decodeURIComponent(match[1]) : "download" };
}

export interface ApiUser { id: number; name: string; email: string; role: "USER" | "ADMIN"; phone?: string; address?: string; }
export interface ApplicationResult { applicationId: number; applicationNumber: string; status: string; paymentStatus?: string; createdAt: string; totalQuantity?: number; }
export interface LookupResult { applicationId: number; applicationNumber: string; applicationType: "INDIVIDUAL" | "GROUP"; applicantNameMasked: string; cardType: string; status: string; photoRejectReason?: string; submittedAt: string; }
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
export interface SchoolOption { id: number; name: string; schoolType: "UNIVERSITY" | "HIGH_SCHOOL"; }
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
  memberCount: number; createdAt: string; depositorName?: string; version?: number;
}
export interface AdminApplicationMember {
  memberId: number; englishName?: string; nationality?: string; gender?: "MALE" | "FEMALE";
  birthDate?: string; birthTime?: string; birthRegion?: string;
  assignedName?: string; assignedHanja?: string; photoNumber?: string; cardNumber?: string;
}
export interface NameSelectionStat { name: string; hanja: string; count: number; }
export interface ApplicationStatusResult { applicationId: number; status: ApplicationStatus; }

// ── Inquiries (1:1 문의, 고객지원) ───────────────────────────────
// 백엔드가 @JsonValue/@JsonCreator로 한글 값을 그대로 주고받는다(InquiryCategory enum). 표시값 = 이 문자열.
export type InquiryCategory = "제작 신청" | "결제 및 배송" | "카드 발급" | "행사·단체 협업" | "기타";
export type InquiryStatus = "PENDING" | "COMPLETED";
export interface InquiryListItem { id: number; category: InquiryCategory; title: string; name: string; email: string; phone: string; status: InquiryStatus; createdAt: string; }
export interface InquiryDetail { id: number; category: InquiryCategory; name: string; email: string; phone: string; title: string; content: string; status: InquiryStatus; answer?: string; answeredAt?: string; createdAt: string; }

// ── Account recovery (아이디/비밀번호 찾기) ──────────────────────
export interface RecoveryChallenge { requestId: string; expiresInSeconds: number; resendAfterSeconds: number; }

export const api = {
  getMe: () => request<ApiUser>("/api/users/me"),
  updateMe: (body: { name?: string; phone?: string; address?: string }) => request<ApiUser>("/api/users/me", { method: "PATCH", body: JSON.stringify(body) }),
  withdraw: () => request<void>("/api/users/me/withdraw", { method: "POST" }),
  changePassword: (currentPassword: string, newPassword: string) => request<void>("/api/users/me/password", { method: "PATCH", body: JSON.stringify({ currentPassword, newPassword }) }),
  agreeTerms: (body: { privacyAgreed: boolean; imageUploadAgreed: boolean; shippingAgreed: boolean }) => request("/api/auth/terms", { method: "POST", body: JSON.stringify(body) }),
  refresh: () => request<void>("/api/auth/refresh", { method: "POST" }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
  loginWithPassword: (email: string, password: string) => request<ApiUser>("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  // 일반 이메일 회원가입 — 인증코드 요청 → 확인(signupToken) → 가입.
  checkEmail: (email: string) => request<{ exists: boolean }>("/api/auth/email/check", { method: "POST", body: JSON.stringify({ email }) }),
  requestSignupEmailCode: (email: string) => request<{ expiresInSeconds: number; resendAfterSeconds: number }>("/api/auth/signup/email-verification/request", { method: "POST", body: JSON.stringify({ email }) }),
  confirmSignupEmailCode: (email: string, code: string) => request<{ signupToken: string; expiresInSeconds: number }>("/api/auth/signup/email-verification/confirm", { method: "POST", body: JSON.stringify({ email, code }) }),
  signup: (body: { email: string; signupToken: string; password: string; name: string; phone: string }) => request<ApiUser>("/api/auth/signup", { method: "POST", body: JSON.stringify(body) }),
  createApplication: (form: FormData, bulk = false) => request<ApplicationResult>(`/api/applications${bulk ? "/bulk" : ""}`, { method: "POST", body: form }),
  // 학생증 신청 폼의 학교 검색select — 비로그인 공개 API. 서버 검색이라 query 없이 부르면 빈 배열을
  // 받는다(학교 수가 약 2,800개라 전체 목록을 한 번에 안 준다). 결과는 관련도순 정렬, 최대 20건.
  searchSchools: (query?: string) => request<SchoolOption[]>(`/api/schools/search${qs({ query })}`),
  lookupApplication: (body: { method: "application" | "card"; keyValue: string; phone?: string; email?: string }) => request<LookupResult>("/api/applications/lookup", { method: "POST", body: JSON.stringify(body) }),
  reuploadPhoto: (id: number, form: FormData) => request(`/api/applications/${id}/photo`, { method: "PATCH", body: form }),
  getCardDownload: (id: number) => request<CardDownload>(`/api/applications/${id}/cards/download`),
  cancelApplication: (id: number) => request<{ applicationId: number; status: ApplicationStatus; refundRequired?: boolean }>(`/api/applications/${id}/cancel`, { method: "POST" }),
  // 입금자명 등록/수정 — 완료 화면에서 호출(결제 확인 전까지만 허용, 본인 신청).
  updateDepositor: (id: number, depositorName: string) => request<void>(`/api/applications/${id}/depositor`, { method: "PATCH", body: JSON.stringify({ depositorName }) }),
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
  // Applications (admin, 제작신청 관리) — 조회(목록/상세/구성원)·이름확정·선택이력·상태전이 8종·엑셀 export·작명결과·카드번호 전부 연결됨.
  listAdminApplications: (params: { status?: ApplicationStatus; page?: number; size?: number } = {}) => request<PageResponse<AdminApplicationListItem>>(`/api/admin/applications${qs({ ...params })}`),
  getAdminApplication: (id: number) => request<AdminApplicationDetail>(`/api/admin/applications/${id}`),
  getAdminApplicationMembers: (id: number) => request<AdminApplicationMember[]>(`/api/admin/applications/${id}/members`),
  // 인앱 작명 확정 — 서버에 저장(멤버 이름 반영 + 선택이력 +1). 프론트 localStorage 미사용.
  saveMemberName: (applicationId: number, memberId: number, body: { name: string; hanja?: string; reading?: string; meaning?: string }) =>
    request<void>(`/api/admin/applications/${applicationId}/members/${memberId}/name`, { method: "POST", body: JSON.stringify(body) }),
  getNameSelectionStats: () => request<NameSelectionStat[]>("/api/admin/name-selection-stats"),
  // 카드번호 확정 — 개인/단일 멤버(관리자 직접 입력, 서버 채번 없음).
  assignCardNumber: (applicationId: number, memberId: number, cardNumber: string) =>
    request<void>(`/api/admin/applications/${applicationId}/members/${memberId}/card-number`, { method: "PUT", body: JSON.stringify({ cardNumber }) }),
  // 카드번호 일괄 확정(단체) — 사진번호 기준 매칭, all-or-nothing. applicationVersion으로 동시수정 감지.
  assignCardNumbersBatch: (applicationId: number, applicationVersion: number, items: { photoNumber: string; cardNumber: string }[]) =>
    request<{ updatedCount: number }>(`/api/admin/applications/${applicationId}/card-numbers`, { method: "PUT", body: JSON.stringify({ applicationVersion, items }) }),
  // 신청 명단 엑셀 내보내기 — xlsx 바이너리 다운로드. GROUP은 원본 서식 보존을 위해 정확히 1건만 허용(백엔드 검증).
  exportApplications: (applicationIds: number[], type: ApplicationType) =>
    requestFile("/api/admin/applications/export", { method: "POST", body: JSON.stringify({ applicationIds, type }) }),
  // 사주 프로그램이 돌려준 "이름 포함" 엑셀을 업로드해 구성원 한글이름을 반영한다(단체 작명 결과 반영).
  applyNamingResult: (applicationId: number, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return request<{ updatedCount: number }>(`/api/admin/applications/${applicationId}/naming-result`, { method: "POST", body: form });
  },
  // 신청 상태 전이(관리자) — 백엔드 존재 엔드포인트 연결.
  confirmApplicationPayment: (id: number) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/confirm-payment`, { method: "POST" }),
  startApplicationReview: (id: number) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/start-review`, { method: "POST" }),
  approveApplicationNaming: (id: number) => request<ApplicationStatusResult>(`/api/admin/applications/${id}/approve-naming`, { method: "POST" }),
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
  // Inquiries (공개 생성 + 내 문의) — 로그인 세션 필요.
  createInquiry: (body: { category: InquiryCategory; name: string; email: string; phone: string; title: string; content: string; privacyConsent: boolean }) => request<{ id: number }>("/api/inquiries", { method: "POST", body: JSON.stringify(body) }),
  listMyInquiries: () => request<InquiryListItem[]>("/api/my/inquiries"),
  getMyInquiry: (id: number) => request<InquiryDetail>(`/api/my/inquiries/${id}`),
  // Applications (내 신청) — 로그인 세션 필요.
  listMyApplications: (params: { status?: ApplicationStatus; page?: number; size?: number } = {}) => request<PageResponse<AdminApplicationListItem>>(`/api/my/applications${qs({ ...params })}`),
  getMyApplication: (id: number) => request<AdminApplicationDetail>(`/api/my/applications/${id}`),

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
