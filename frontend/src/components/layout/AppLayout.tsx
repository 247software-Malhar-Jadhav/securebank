import { Outlet } from "react-router-dom";
import { Sidebar } from "@/components/layout/Sidebar";
import { Topbar } from "@/components/layout/Topbar";

/**
 * AppLayout — the authenticated application shell.
 *
 * Layout: a fixed sidebar on the left (desktop) and a sticky topbar; the routed
 * page renders into <Outlet />. This is a react-router *layout route*, so it wraps
 * all the protected pages without each page re-declaring the chrome.
 */
export function AppLayout() {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-4 lg:p-8">
          <div className="mx-auto max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
