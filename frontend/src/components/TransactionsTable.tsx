import { useTranslation } from "react-i18next";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Money } from "@/components/Money";
import { TransactionStatusBadge } from "@/components/StatusBadge";
import { formatDateTime } from "@/lib/utils";
import type { Transaction } from "@/types";

/**
 * TransactionsTable — reused on the dashboard (recent) and account detail (full).
 * Amounts are colored/signed by transaction type via the <Money> component.
 */
export function TransactionsTable({ transactions }: { transactions: Transaction[] }) {
  const { t, i18n } = useTranslation();
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t("common.date")}</TableHead>
          <TableHead>{t("common.type")}</TableHead>
          <TableHead>{t("common.description")}</TableHead>
          <TableHead>{t("common.status")}</TableHead>
          <TableHead className="text-right">{t("common.amount")}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {transactions.map((txn) => (
          <TableRow key={txn.id}>
            <TableCell className="whitespace-nowrap text-muted-foreground">
              {formatDateTime(txn.createdAt, i18n.language)}
            </TableCell>
            <TableCell>{t(`transactions.type.${txn.type}`)}</TableCell>
            <TableCell className="max-w-[220px] truncate">
              {txn.description || "—"}
            </TableCell>
            <TableCell>
              <TransactionStatusBadge status={txn.status} />
            </TableCell>
            <TableCell className="text-right">
              <Money
                amount={txn.amount}
                currency={txn.currency}
                signedBy={txn.type}
              />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
