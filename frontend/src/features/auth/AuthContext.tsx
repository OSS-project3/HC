// Auth context/provider: mock login, logout, and current-user state.
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api } from "../../services/api";

export type Role = "user" | "admin";

export interface AuthUser {
  name: string;
  email: string;
  role: Role;
  source?: "api" | "local";
  phone?: string;
  address?: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAdmin: boolean;
  login: (user: AuthUser) => void;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const STORAGE_KEY = "auth-user";

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

  const refreshProfile = useCallback(async () => {
    try {
      const profile = await api.getMe();
      // /api/users/me는 role을 포함하지 않는다(백엔드 설계 결정) — 로그인 시 확정된 기존 role을 유지하고
      // 프로필 표시 정보만 서버 기준으로 갱신한다. (role을 profile에서 파생하면 admin이 user로 강등된다.)
      setUser((prev) => ({
        name: profile.name,
        email: profile.email,
        role: prev?.role ?? "user",
        source: "api",
        phone: profile.phone,
        address: profile.address,
      }));
    } catch {
      // A local account or unauthenticated visitor keeps the existing mock state.
    }
  }, []);

  useEffect(() => { void refreshProfile(); }, [refreshProfile]);

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
      login: (u) => setUser({ ...u, source: u.source ?? "local" }),
      logout: () => { if (user?.source === "api") void api.logout().catch(() => undefined); setUser(null); },
      refreshProfile,
    }),
    [user, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within <AuthProvider>");
  return ctx;
}
