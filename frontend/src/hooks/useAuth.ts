import { useAppSelector } from "@/app/hooks";

/**
 * useAuth — convenience selector for the current session.
 *
 * Centralizes "am I logged in?" and "what's my role?" so components and route
 * guards don't reach into the slice shape directly.
 */
export function useAuth() {
  const { accessToken, user } = useAppSelector((s) => s.auth);
  return {
    user,
    isAuthenticated: !!accessToken && !!user,
    isAdmin: user?.role === "ADMIN",
    role: user?.role ?? null,
  };
}
