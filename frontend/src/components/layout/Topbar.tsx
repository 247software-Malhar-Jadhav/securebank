import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { LogOut, Menu, ShieldCheck, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Avatar,
  AvatarFallback,
} from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { ThemeToggle } from "@/components/ThemeToggle";
import { NavLinks } from "@/components/layout/Sidebar";
import { useAppDispatch } from "@/app/hooks";
import { useAuth } from "@/hooks/useAuth";
import { logout } from "@/features/auth/authSlice";
import { api } from "@/services/api";

/**
 * Topbar — the persistent header inside the authenticated shell. It holds the
 * mobile nav trigger, the language switcher, theme toggle, and the user menu.
 */
export function Topbar() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const initials = user
    ? user.username.slice(0, 2).toUpperCase()
    : "?";

  function handleLogout() {
    // Clear the session AND wipe the RTK Query cache so the next user can't see
    // the previous user's cached data.
    dispatch(logout());
    dispatch(api.util.resetApiState());
    navigate("/login", { replace: true });
  }

  return (
    <header className="flex h-16 shrink-0 items-center gap-3 border-b bg-card px-4 lg:px-6">
      {/* Mobile menu trigger (hidden on desktop where the sidebar is visible). */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetTrigger asChild>
          <Button variant="ghost" size="icon" className="lg:hidden">
            <Menu className="h-5 w-5" />
            <span className="sr-only">Menu</span>
          </Button>
        </SheetTrigger>
        <SheetContent side="left" className="w-64 p-0">
          <SheetHeader className="flex h-16 flex-row items-center gap-2 border-b px-6">
            <ShieldCheck className="h-6 w-6 text-primary" />
            <SheetTitle>{t("app.name")}</SheetTitle>
          </SheetHeader>
          <div className="p-4">
            <NavLinks onNavigate={() => setMobileOpen(false)} />
          </div>
        </SheetContent>
      </Sheet>

      <div className="flex-1" />

      <LanguageSwitcher />
      <ThemeToggle />

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="gap-2 px-2">
            <Avatar>
              <AvatarFallback>{initials}</AvatarFallback>
            </Avatar>
            <span className="hidden text-sm font-medium sm:inline">
              {user?.username}
            </span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel className="flex flex-col gap-1">
            <span>{user?.username}</span>
            <span className="text-xs font-normal text-muted-foreground">
              {user?.email}
            </span>
            {user && (
              <Badge variant="outline" className="mt-1 w-fit">
                {user.role}
              </Badge>
            )}
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem disabled>
            <User className="h-4 w-4" />
            {user?.username}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={handleLogout}>
            <LogOut className="h-4 w-4" />
            {t("auth.logout")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
