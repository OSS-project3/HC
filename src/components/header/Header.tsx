// Site header: logo (+home-only tagline), primary nav, language/login actions, and the mobile drawer.
import { useEffect, useMemo, useRef, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { Logo } from "../brand/Logo";
import { mainNav, adminNavItem, supportMenu, type NavItem } from "../../config/navigation";
import { useAuth } from "../../features/auth/AuthContext";
import { GlobeIcon, ChevronRight } from "../ui/icons";
import "./Header.css";

export function Header() {
  const location = useLocation();
  const isHome = location.pathname === "/";
  const { user, isAdmin, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  // Transparent at the very top; gains a backdrop once the page is scrolled so
  // the header stays legible over content passing underneath.
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Admin entry appears (before 고객지원) only when logged in as admin.
  const navItems = useMemo<NavItem[]>(() => {
    if (!isAdmin) return mainNav;
    const items = [...mainNav];
    const supportIdx = items.findIndex((i) => i.to === "/support");
    items.splice(supportIdx < 0 ? items.length : supportIdx, 0, adminNavItem);
    return items;
  }, [isAdmin]);

  // Close the mobile drawer whenever the route changes.
  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  return (
    <>
      <header className={clsx("header", scrolled && "header--scrolled")}>
        <div className="header__inner">
        <Logo withTree={isHome} size="sm" />

        <nav className="header__nav" aria-label="주요 메뉴">
          {navItems.map((item) =>
            item.to === "/support" ? (
              <SupportNavItem key={item.to} />
            ) : (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => clsx("nav-item", isActive && "nav-item--active")}
              >
                {item.label}
              </NavLink>
            ),
          )}
        </nav>

        <div className="header__actions">
          <LanguageSelector />
          {user ? (
            <div className="header__account">
              <span className="header__username">{user.name}님</span>
              <button className="header__logout" onClick={logout}>
                로그아웃
              </button>
            </div>
          ) : (
            <NavLink to="/login" className="header__login">
              로그인
            </NavLink>
          )}
          <button
            className="header__burger"
            aria-label="메뉴 열기"
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen((v) => !v)}
          >
            <span />
            <span />
            <span />
          </button>
        </div>
        </div>
      </header>

      <MobileDrawer open={mobileOpen} onClose={() => setMobileOpen(false)} navItems={navItems} />
    </>
  );
}

/** Customer-support item: navy underline stays active; hover/focus opens an
 *  anchor dropdown that scrolls to a section within /support. */
function SupportNavItem() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const closeTimer = useRef<number | undefined>(undefined);

  const openNow = () => {
    window.clearTimeout(closeTimer.current);
    setOpen(true);
  };
  const closeSoon = () => {
    window.clearTimeout(closeTimer.current);
    closeTimer.current = window.setTimeout(() => setOpen(false), 260);
  };

  useEffect(() => () => window.clearTimeout(closeTimer.current), []);

  const go = (id: string) => {
    window.clearTimeout(closeTimer.current);
    setOpen(false);
    navigate(`/support#${id}`);
  };

  return (
    <div
      className="support-menu"
      ref={ref}
      onMouseEnter={openNow}
      onMouseLeave={closeSoon}
      onFocus={openNow}
      onBlur={(e) => {
        if (!ref.current?.contains(e.relatedTarget as Node)) closeSoon();
      }}
    >
      <NavLink
        to="/support"
        className={({ isActive }) => clsx("nav-item", isActive && "nav-item--active")}
        aria-haspopup="true"
        aria-expanded={open}
      >
        고객지원
      </NavLink>
      <div className={clsx("support-menu__panel", open && "support-menu__panel--open")} role="menu">
        {supportMenu.map((item) => (
          <button key={item.id} role="menuitem" className="support-menu__item" onClick={() => go(item.id)}>
            {item.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function LanguageSelector() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [open]);

  return (
    <div className="lang" ref={ref}>
      <button className="lang__toggle" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
        <GlobeIcon width={16} height={16} />
        <span>한국어</span>
        <span className="lang__caret" aria-hidden="true">
          ⌄
        </span>
      </button>
      {open && (
        <ul className="lang__menu">
          <li>
            <button className="lang__option lang__option--active">한국어</button>
          </li>
          <li>
            <button className="lang__option">English</button>
          </li>
        </ul>
      )}
    </div>
  );
}

function MobileDrawer({
  open,
  onClose,
  navItems,
}: {
  open: boolean;
  onClose: () => void;
  navItems: NavItem[];
}) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [supportOpen, setSupportOpen] = useState(false);

  return (
    <div className={clsx("drawer", open && "drawer--open")} aria-hidden={!open}>
      <div className="drawer__backdrop" onClick={onClose} />
      <div className="drawer__panel">
        <nav aria-label="모바일 메뉴">
          {navItems.map((item) =>
            item.to === "/support" ? (
              <div key="support" className="drawer__group">
                <button className="drawer__link drawer__accordion" onClick={() => setSupportOpen((v) => !v)}>
                  고객지원
                  <span className={clsx("drawer__chev", supportOpen && "drawer__chev--open")}>
                    <ChevronRight width={16} height={16} />
                  </span>
                </button>
                {supportOpen && (
                  <div className="drawer__sub">
                    {supportMenu.map((s) => (
                      <button
                        key={s.id}
                        className="drawer__sublink"
                        onClick={() => {
                          onClose();
                          navigate(`/support#${s.id}`);
                        }}
                      >
                        {s.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <NavLink key={item.to} to={item.to} className="drawer__link" onClick={onClose}>
                {item.label}
              </NavLink>
            ),
          )}
          {user ? (
            <button
              className="drawer__link drawer__link--login"
              onClick={() => {
                logout();
                onClose();
              }}
            >
              로그아웃 ({user.name}님)
            </button>
          ) : (
            <NavLink to="/login" className="drawer__link drawer__link--login" onClick={onClose}>
              로그인
            </NavLink>
          )}
        </nav>
      </div>
    </div>
  );
}
