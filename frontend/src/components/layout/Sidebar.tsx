import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  LayoutDashboard,
  Wallet,
  ArrowLeftRight,
  Users,
  Bot,
  ShieldCheck,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/hooks/useAuth";

interface NavItem {
  to: string;
  labelKey: string;
  icon: LucideIcon;
  adminOnly?: boolean;
}

// Single source of truth for the nav. The same list drives the mobile drawer.
export const NAV_ITEMS: NavItem[] = [
  { to: "/", labelKey: "nav.dashboard", icon: LayoutDashboard },
  { to: "/accounts", labelKey: "nav.accounts", icon: Wallet },
  { to: "/transfer", labelKey: "nav.transfer", icon: ArrowLeftRight },
  { to: "/beneficiaries", labelKey: "nav.beneficiaries", icon: Users },
  { to: "/assistant", labelKey: "nav.assistant", icon: Bot },
  {
    to: "/admin/audit-logs",
    labelKey: "nav.auditLogs",
    icon: ShieldCheck,
    adminOnly: true,
  },
];

/** The list of nav links, shared between the desktop sidebar and mobile sheet. */
export function NavLinks({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation();
  const { isAdmin } = useAuth();

  return (
    <nav className="flex flex-col gap-1">
      {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          // `end` on the dashboard route so "/" isn't marked active for every path.
          end={item.to === "/"}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
              isActive
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
            )
          }
        >
          <item.icon className="h-4 w-4" />
          {t(item.labelKey)}
        </NavLink>
      ))}
    </nav>
  );
}

/** Desktop sidebar (hidden on small screens; the topbar opens a Sheet there). */
export function Sidebar() {
  const { t } = useTranslation();
  return (
    <aside className="hidden w-64 shrink-0 border-r bg-card lg:flex lg:flex-col">
      <div className="flex h-16 items-center gap-2 border-b px-6">
        <ShieldCheck className="h-6 w-6 text-primary" />
        <span className="text-lg font-bold">{t("app.name")}</span>
      </div>
      <div className="flex-1 overflow-y-auto p-4">
        <NavLinks />
      </div>
    </aside>
  );
}
