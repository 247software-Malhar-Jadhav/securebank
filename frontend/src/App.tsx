import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { RootErrorFallback } from "@/components/RootErrorFallback";
import { Toaster } from "@/components/ui/sonner";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { AccountsPage } from "@/pages/AccountsPage";
import { AccountDetailPage } from "@/pages/AccountDetailPage";
import { TransferPage } from "@/pages/TransferPage";
import { BeneficiariesPage } from "@/pages/BeneficiariesPage";
import { AssistantPage } from "@/pages/AssistantPage";
import { AuditLogsPage } from "@/pages/AuditLogsPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

/**
 * App — top-level component: error boundary, router, and the global toaster.
 *
 * Route structure (react-router v6 nested routes):
 *   /login, /register                 -> public
 *   <ProtectedRoute>                   -> requires auth
 *     <AppLayout> (sidebar + topbar)
 *       /            -> Dashboard
 *       /accounts, /accounts/:id, /transfer, /beneficiaries, /assistant
 *       <ProtectedRoute roles=ADMIN>
 *         /admin/audit-logs           -> ADMIN only
 *   *                                  -> 404
 *
 * Layout routes (ProtectedRoute, AppLayout) render an <Outlet/> for their children,
 * so the shell and the guards are declared once and apply to everything nested.
 */
export default function App() {
  return (
    <ErrorBoundary fallback={(error) => <RootErrorFallback error={error} />}>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Authenticated app */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="accounts" element={<AccountsPage />} />
              <Route path="accounts/:id" element={<AccountDetailPage />} />
              <Route path="transfer" element={<TransferPage />} />
              <Route path="beneficiaries" element={<BeneficiariesPage />} />
              <Route path="assistant" element={<AssistantPage />} />

              {/* Admin-only nested guard */}
              <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
                <Route path="admin/audit-logs" element={<AuditLogsPage />} />
              </Route>
            </Route>
          </Route>

          {/* Redirect bare /admin to its only page, then catch-all 404. */}
          <Route path="/admin" element={<Navigate to="/admin/audit-logs" replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
      {/* Mounted once; toast(...) calls anywhere render here. */}
      <Toaster richColors position="top-right" />
    </ErrorBoundary>
  );
}
