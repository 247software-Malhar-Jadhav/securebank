import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";

/** NotFoundPage — catch-all 404 route. */
export function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background p-4 text-center">
      <p className="text-6xl font-bold text-primary">404</p>
      <h1 className="text-2xl font-bold">{t("errors.pageNotFound")}</h1>
      <p className="text-muted-foreground">{t("errors.pageNotFoundDesc")}</p>
      <Button asChild>
        <Link to="/">{t("errors.goHome")}</Link>
      </Button>
    </div>
  );
}
