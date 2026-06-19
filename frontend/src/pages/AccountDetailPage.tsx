import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowLeft } from "lucide-react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { PageHeader } from "@/components/PageHeader";
import { Money } from "@/components/Money";
import { AccountStatusBadge } from "@/components/StatusBadge";
import { TransactionsTable } from "@/components/TransactionsTable";
import { EmptyState, ErrorState } from "@/components/States";
import {
  useGetAccountQuery,
  useGetAccountTransactionsQuery,
} from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";
import { formatDate } from "@/lib/utils";

/** AccountDetailPage — single account header + its full transaction history. */
export function AccountDetailPage() {
  const { t, i18n } = useTranslation();
  const params = useParams<{ id: string }>();
  const accountId = Number(params.id);

  const accountQ = useGetAccountQuery(accountId);
  const txnsQ = useGetAccountTransactionsQuery(accountId);

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("accounts.detailTitle")}
        action={
          <Button variant="outline" asChild>
            <Link to="/accounts">
              <ArrowLeft className="h-4 w-4" />
              {t("common.back")}
            </Link>
          </Button>
        }
      />

      {/* Account summary header */}
      {accountQ.isLoading ? (
        <Skeleton className="h-40 w-full" />
      ) : accountQ.isError ? (
        <ErrorState
          message={extractErrorMessage(accountQ.error, t("errors.loadFailed"))}
          onRetry={() => void accountQ.refetch()}
        />
      ) : accountQ.data ? (
        <Card>
          <CardContent className="p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm text-muted-foreground">
                  {t(`accounts.type.${accountQ.data.type}`)}
                </p>
                <p className="font-mono text-sm">{accountQ.data.accountNumber}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {t("accounts.openedOn")}{" "}
                  {formatDate(accountQ.data.openedAt, i18n.language)}
                </p>
              </div>
              <AccountStatusBadge status={accountQ.data.status} />
            </div>
            <Separator className="my-4" />
            <div>
              <p className="text-xs text-muted-foreground">{t("common.balance")}</p>
              <Money
                amount={accountQ.data.balance}
                currency={accountQ.data.currency}
                className="text-3xl font-bold"
              />
            </div>
          </CardContent>
        </Card>
      ) : null}

      {/* Transactions */}
      <Card>
        <CardHeader>
          <CardTitle>{t("accounts.transactions")}</CardTitle>
        </CardHeader>
        <CardContent>
          {txnsQ.isLoading ? (
            <div className="space-y-2">
              {[0, 1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : txnsQ.isError ? (
            <ErrorState
              message={extractErrorMessage(txnsQ.error, t("errors.loadFailed"))}
              onRetry={() => void txnsQ.refetch()}
            />
          ) : txnsQ.data && txnsQ.data.length > 0 ? (
            <TransactionsTable transactions={txnsQ.data} />
          ) : (
            <EmptyState message={t("dashboard.noTransactions")} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
