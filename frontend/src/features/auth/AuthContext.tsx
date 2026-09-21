// Auth context/provider: 서버 세션(HttpOnly 쿠키)이 Source of Truth인 인증 상태.
// 앱 시작 시 GET /api/users/me로 로그인 여부를 확정하고, localStorage에는 사용자 정보를 저장하지 않는다.
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "../../services/api";

export type Role = "user" | "admin";
/**
 * loading: 앱 시작 후 /me 확인 전 · authenticated: 서버 세션 확인됨 ·
 * unauthenticated: 최종 401/403(refresh 실패 포함) · error: 네트워크 장애·5xx(로그아웃 아님).
 */
export type AuthStatus = "loading" | "authenticated" | "unauthenticated" | "error";

export interface AuthUser {
  name: string;
  email: string;
  role: Role;
  phone?: string;
  address?: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  isAdmin: boolean;
  login: (user: AuthUser) => void;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);
// 과거 mock 인증이 쓰던 전체 사용자 저장 키 — 발견 시 제거만 한다.
const LEGACY_STORAGE_KEY = "auth-user";

function toRole(apiRole: "USER" | "ADMIN"): Role {
  return apiRole === "ADMIN" ? "admin" : "user";
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  const refreshProfile = useCallback(async () => {
    try {
      const profile = await api.getMe();
      setUser({
        name: profile.name,
        email: profile.email,
        role: toRole(profile.role),
        phone: profile.phone,
        address: profile.address,
      });
      setStatus("authenticated");
    } catch (error) {
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
        // refresh 재시도까지 실패한 최종 401/403 — 비로그인 상태로 확정한다.
        setUser(null);
        setStatus("unauthenticated");
      } else {
        // 네트워크 장애·timeout·5xx — 로그아웃으로 단정하지 않는다. 이미 확인된 세션은 유지.
        setStatus((prev) => (prev === "authenticated" ? prev : "error"));
      }
    }
  }, []);

  useEffect(() => {
    try {
      localStorage.removeItem(LEGACY_STORAGE_KEY);
    } catch {
      /* ignore */
    }
    void refreshProfile();
  }, [refreshProfile]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAdmin: user?.role === "admin",
      // 서버 로그인 성공 직후 호출된다(LoginPage 등) — 로그인 응답의 사용자·role로 즉시 동기화.
      login: (u) => {
        setUser(u);
        setStatus("authenticated");
      },
      logout: () => {
        void api.logout().catch(() => undefined);
        setUser(null);
        setStatus("unauthenticated");
      },
      refreshProfile,
    }),
    [user, status, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within <AuthProvider>");
  return ctx;
}
