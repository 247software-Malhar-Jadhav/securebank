import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Sparkles, Wallet } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/PageHeader";
import { AccountCard } from "@/components/AccountCard";
import { TransactionsTable } from "@/components/TransactionsTable";
import { SpendingChart } from "@/components/SpendingChart";
import { Money } from "@/components/Money";
import { EmptyState, ErrorState } from "@/components/States";
import {
  useGetAccountsQuery,
  useGetAccountTransactionsQuery,
  useGetMeQuery,
  useGetSpendingInsightsQuery,
} from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";
import { useAuth } from "@/hooks/useAuth";

/**
 * DashboardPage — the landing screen after login.
 *
 * It composes four independent RTK Query hooks (customer, accounts, insights, and
 * the first account's transactions). Each renders its own loading skeleton / error
 * state, so a slow or failing section never blocks the rest of the page.
 */
export function DashboardPage() {
  const { t } = useTranslation();
  const { user } = useAuth();

  const me = useGetMeQuery();
  const accountsQ = useGetAccountsQuery();
  const insightsQ = useGetSpendingInsightsQuery();

  // Show the most recent transactions from the first account as an overview.
  const firstAccountId = accountsQ.data?.[0]?.id;
  const txnsQ = useGetAccountTransactionsQuery(firstAccountId as number, {
    skip: firstAccountId === undefined, // don't fire until we know an account id
  });

  const displayName = me.data?.firstName ?? user?.username ?? "";

  // Total balance across all accounts (display only; arithmetic on the numeric form).
  const total = (accountsQ.data ?? []).reduce(
    (sum, a) => sum + Number(a.balance),
    0,
  );
  const totalCurrency = accountsQ.data?.[0]?.currency ?? "USD";

  return (
    <div className="space-y-8">
      <PageHeader
        title={t("dashboard.title")}
        subtitle={t("dashboard.greeting", { name: displayName })}
        action={
          <Button asChild>
            <Link to="/transfer">{t("transfer.title")}</Link>
          </Button>
        }
      />

      {/* Total balance hero */}
      <Card className="bg-primary text-primary-foreground">
        <CardContent className="flex items-center justify-between p-6">
          <div>
            <p className="text-sm opacity-80">{t("dashboard.totalBalance")}</p>
            {accountsQ.isLoading ? (
              <Skeleton className="mt-2 h-9 w-40 bg-primary-foreground/20" />
            ) : (
              <Money
                amount={total}
                currency={totalCurrency}
                className="text-3xl font-bold"
              />
            )}
          </div>
          <Wallet className="h-10 w-10 opacity-80" />
        </CardContent>
      </Card>

      {/* Account cards */}
      <section>
        <h2 className="mb-3 text-lg font-semibold">{t("dashboard.yourAccounts")}</h2>
        {accountsQ.isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        ) : accountsQ.isError ? (
          <ErrorState
            message={extractErrorMessage(accountsQ.error, t("errors.loadFailed"))}
            onRetry={() => void accountsQ.refetch()}
          />
        ) : accountsQ.data && accountsQ.data.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {accountsQ.data.map((a) => (
              <AccountCard key={a.id} account={a} />
            ))}
          </div>
        ) : (
          <EmptyState
            message={t("dashboard.noAccounts")}
            action={
              <Button asChild size="sm">
                <Link to="/accounts">{t("accounts.openAccount")}</Link>
              </Button>
            }
          />
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Spending insights chart + AI summary */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-primary" />
              {t("dashboard.spendingInsights")}
            </CardTitle>
            <CardDescription>{t("dashboard.aiSummary")}</CardDescription>
          </CardHeader>
          <CardContent>
            {insightsQ.isLoading ? (
              <Skeleton className="h-64 w-full" />
            ) : insightsQ.isError ? (
              <ErrorState
                message={extractErrorMessage(insightsQ.error, t("errors.loadFailed"))}
                onRetry={() => void insightsQ.refetch()}
              />
            ) : insightsQ.data && insightsQ.data.categories.length > 0 ? (
              <div className="space-y-4">
                <SpendingChart
                  categories={insightsQ.data.categories}
                  currency={insightsQ.data.currency}
                />
                {/* The backend already localizes this summary string. */}
                <p className="rounded-md bg-muted/50 p-3 text-sm text-muted-foreground">
                  {insightsQ.data.summary}
                </p>
              </div>
            ) : (
              <EmptyState message={t("insights.noData")} />
            )}
          </CardContent>
        </Card>

        {/* Recent transactions */}
        <Card>
          <CardHeader>
            <CardTitle>{t("dashboard.recentTransactions")}</CardTitle>
          </CardHeader>
          <CardContent>
            {txnsQ.isLoading || accountsQ.isLoading ? (
              <div className="space-y-2">
                {[0, 1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : txnsQ.data && txnsQ.data.length > 0 ? (
              <TransactionsTable transactions={txnsQ.data.slice(0, 6)} />
            ) : (
              <EmptyState message={t("dashboard.noTransactions")} />
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
