import { useEffect, type RefObject } from "react";

/**
 * Scroll-reveal for content inside `root`:
 *  - `.reveal`        → the element fades / slides up as a whole when it enters view.
 *  - `.reveal-group`  → when it enters view, its `.reveal-item` descendants fade up
 *                       one-by-one with a small stagger delay.
 * Toggling classes are `.reveal--visible` / `.reveal-item--visible` (see globals.css).
 */
const STAGGER_STEP = 90; // ms between consecutive items
const STAGGER_MAX = 8; // cap so long lists don't drag on

export function useScrollReveal(root: RefObject<HTMLElement>) {
  useEffect(() => {
    const container = root.current;
    if (!container) return;

    const singles = Array.from(container.querySelectorAll<HTMLElement>(".reveal"));
    const groups = Array.from(container.querySelectorAll<HTMLElement>(".reveal-group"));
    if (singles.length === 0 && groups.length === 0) return;

    const revealGroup = (group: HTMLElement) => {
      const items = Array.from(group.querySelectorAll<HTMLElement>(".reveal-item"));
      items.forEach((item, i) => {
        item.style.transitionDelay = `${Math.min(i, STAGGER_MAX) * STAGGER_STEP}ms`;
        item.classList.add("reveal-item--visible");
      });
    };

    // The reveal plays regardless of the OS "reduce motion" setting (by request).
    // Only bail to an immediate show if IntersectionObserver is unavailable, so
    // content is never left hidden.
    if (!("IntersectionObserver" in window)) {
      singles.forEach((el) => el.classList.add("reveal--visible"));
      groups.forEach(revealGroup);
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          const el = entry.target as HTMLElement;
          if (el.classList.contains("reveal-group")) revealGroup(el);
          else el.classList.add("reveal--visible");
          observer.unobserve(el);
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -8% 0px" },
    );

    singles.forEach((el) => observer.observe(el));
    groups.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [root]);
}
