import { useTranslation } from "react-i18next";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/PageHeader";
import { EmptyState, ErrorState } from "@/components/States";
import { useGetAuditLogsQuery } from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";
import { formatDateTime } from "@/lib/utils";

/**
 * AuditLogsPage — ADMIN-only immutable change log.
 *
 * The route itself is guarded by <ProtectedRoute roles={["ADMIN"]}>, and the admin
 * nav link is hidden for non-admins; this is defense in depth on top of the backend's
 * own authorization (the API returns 403 to non-admins regardless).
 */
export function AuditLogsPage() {
  const { t, i18n } = useTranslation();
  const { data, isLoading, isError, error, refetch } = useGetAuditLogsQuery();

  return (
    <div>
      <PageHeader title={t("audit.title")} subtitle={t("audit.subtitle")} />

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState
          message={extractErrorMessage(error, t("errors.loadFailed"))}
          onRetry={() => void refetch()}
        />
      ) : data && data.length > 0 ? (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("common.date")}</TableHead>
                  <TableHead>{t("audit.actor")}</TableHead>
                  <TableHead>{t("audit.action")}</TableHead>
                  <TableHead>{t("audit.entity")}</TableHead>
                  <TableHead>{t("audit.entityId")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {formatDateTime(log.createdAt, i18n.language)}
                    </TableCell>
                    <TableCell>{log.actor}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{log.action}</Badge>
                    </TableCell>
                    <TableCell>{log.entityType}</TableCell>
                    <TableCell className="font-mono text-xs">
                      {log.entityId}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ) : (
        <EmptyState message={t("audit.empty")} />
      )}
    </div>
  );
}
