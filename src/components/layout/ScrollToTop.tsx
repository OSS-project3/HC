import { useEffect } from "react";
import { useLocation } from "react-router-dom";

/** Scroll to top on route change, unless the URL carries a hash anchor. */
export function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (hash) return;
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [pathname, hash]);

  return null;
}
