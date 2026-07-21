// Auth context/provider: mock login, logout, and current-user state.
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

export type Role = "user" | "admin";

export interface AuthUser {
  name: string;
  email: string;
  role: Role;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAdmin: boolean;
  login: (user: AuthUser) => void;
  loginAsUser: () => void;
  loginAsAdmin: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const STORAGE_KEY = "auth-user";

/** Demo accounts used by the "데모 로그인" buttons on the login page. */
export const demoUser: AuthUser = { name: "홍길동", email: "user@demo.com", role: "user" };
export const demoAdmin: AuthUser = { name: "관리자", email: "admin@demo.com", role: "admin" };

/**
 * Lightweight mock auth. Persists the current user in localStorage so the admin
 * menu stays visible across reloads. This is front-end only — real auth must be
 * verified on the server.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    try {
      if (user) localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  }, [user]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAdmin: user?.role === "admin",
      login: (u) => setUser(u),
      loginAsUser: () => setUser(demoUser),
      loginAsAdmin: () => setUser(demoAdmin),
      logout: () => setUser(null),
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within <AuthProvider>");
  return ctx;
}
