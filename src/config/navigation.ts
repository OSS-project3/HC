export interface NavItem {
  label: string;
  to: string;
}

/**
 * Primary header / footer navigation.
 * "관리" (admin) is gated behind auth in production — see the note in the
 * requirements. It is listed here so the header can render it when allowed.
 */
export const mainNav: NavItem[] = [
  { label: "회사 소개", to: "/company" },
  { label: "디자인", to: "/design" },
  { label: "제작 신청", to: "/apply" },
  { label: "조회", to: "/lookup" },
  { label: "후기", to: "/reviews" },
  { label: "행사사업", to: "/events" },
  { label: "관리", to: "/admin" },
  { label: "고객지원", to: "/support" },
];

/** Footer omits "관리" (admin-only). */
export const footerNav: NavItem[] = mainNav.filter((item) => item.to !== "/admin");

/**
 * Customer-support dropdown items. These are NOT separate pages — each one
 * scrolls to a section anchor within /support.
 */
export interface SupportMenuItem {
  label: string;
  id: string;
}

export const supportMenu: SupportMenuItem[] = [
  { label: "공지사항", id: "notice" },
  { label: "자주 묻는 질문", id: "faq" },
  { label: "제작 이야기", id: "story" },
  { label: "상담 문의", id: "contact" },
];
