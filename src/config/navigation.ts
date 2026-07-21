// Navigation config: main menu items, admin entry, and support submenu.
export interface NavItem {
  label: string;
  to: string;
}

/**
 * Primary header / footer navigation.
 * "관리" (admin) is NOT part of the default nav — it is inserted into the header
 * only when an admin is logged in (see Header + useAuth).
 */
export const mainNav: NavItem[] = [
  { label: "회사 소개", to: "/company" },
  { label: "디자인", to: "/design" },
  { label: "제작 신청", to: "/apply" },
  { label: "조회", to: "/lookup" },
  { label: "후기", to: "/reviews" },
  { label: "행사사업", to: "/events" },
  { label: "고객지원", to: "/support" },
];

/** Admin-only entry, shown in the header when logged in as admin. */
export const adminNavItem: NavItem = { label: "관리", to: "/admin" };

/** Footer navigation (never includes 관리). */
export const footerNav: NavItem[] = mainNav;

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
