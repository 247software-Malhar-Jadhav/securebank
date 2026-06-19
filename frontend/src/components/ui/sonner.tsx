import type { ComponentProps } from "react";
import { Toaster as Sonner, toast } from "sonner";
import { useTheme } from "@/hooks/useTheme";

/**
 * Toaster — thin wrapper over the `sonner` toast library, themed to match our
 * design tokens. We mount <Toaster /> once near the app root; anywhere else we
 * just call `toast.success(...)` / `toast.error(...)`. RTK Query error handling
 * surfaces the backend's localized RFC-7807 message through these toasts.
 */
type ToasterProps = ComponentProps<typeof Sonner>;

const Toaster = ({ ...props }: ToasterProps) => {
  const { theme } = useTheme();
  return (
    <Sonner
      theme={theme}
      className="toaster group"
      toastOptions={{
        classNames: {
          toast:
            "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
          description: "group-[.toast]:text-muted-foreground",
          actionButton:
            "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
          cancelButton:
            "group-[.toast]:bg-muted group-[.toast]:text-muted-foreground",
          error:
            "group-[.toaster]:border-destructive/40 group-[.toaster]:text-destructive",
          success:
            "group-[.toaster]:border-success/40 group-[.toaster]:text-success",
        },
      }}
      {...props}
    />
  );
};

export { Toaster, toast };
