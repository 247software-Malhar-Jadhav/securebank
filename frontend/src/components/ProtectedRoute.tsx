import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import type { Role } from "@/types";

interface ProtectedRouteProps {
  /** If set, the user must have one of these roles, otherwise they're sent home. */
  roles?: Role[];
}

/**
 * ProtectedRoute — a route guard used as a layout route in the router.
 *
 * - Not authenticated -> redirect to /login, remembering where they were headed
 *   (via location state) so we can bounce them back after they sign in.
 * - Authenticated but lacking the required role -> redirect to the dashboard.
 * - Otherwise render the nested routes through <Outlet />.
 */
export function ProtectedRoute({ roles }: ProtectedRouteProps) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles && (!role || !roles.includes(role))) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
