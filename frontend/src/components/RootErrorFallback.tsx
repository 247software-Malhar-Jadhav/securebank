import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";

/** Fallback UI rendered by the top-level ErrorBoundary when a render crashes. */
export function RootErrorFallback({ error }: { error: Error }) {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background p-6 text-center">
      <h1 className="text-2xl font-bold text-destructive">
        {t("errors.boundaryTitle")}
      </h1>
      <p className="max-w-md text-muted-foreground">{t("errors.boundaryDesc")}</p>
      <pre className="max-w-md overflow-auto rounded-md bg-muted p-3 text-left text-xs text-muted-foreground">
        {error.message}
      </pre>
      <Button onClick={() => window.location.reload()}>
        {t("errors.reload")}
      </Button>
    </div>
  );
}
