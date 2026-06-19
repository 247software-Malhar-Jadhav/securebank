import { useTranslation } from "react-i18next";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/PageHeader";
import { AccountCard } from "@/components/AccountCard";
import { OpenAccountDialog } from "@/components/OpenAccountDialog";
import { EmptyState, ErrorState } from "@/components/States";
import { useGetAccountsQuery } from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";

/** AccountsPage — lists all of the customer's accounts with an "open account" action. */
export function AccountsPage() {
  const { t } = useTranslation();
  const { data, isLoading, isError, error, refetch } = useGetAccountsQuery();

  return (
    <div>
      <PageHeader title={t("accounts.title")} action={<OpenAccountDialog />} />

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState
          message={extractErrorMessage(error, t("errors.loadFailed"))}
          onRetry={() => void refetch()}
        />
      ) : data && data.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((a) => (
            <AccountCard key={a.id} account={a} />
          ))}
        </div>
      ) : (
        <EmptyState
          message={t("dashboard.noAccounts")}
          action={<OpenAccountDialog />}
        />
      )}
    </div>
  );
}
