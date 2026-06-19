import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import type {
  AccountStatus,
  TransactionStatus,
  KycStatus,
} from "@/types";

/**
 * Maps a domain status to a badge color variant. Keeping this here means every
 * page renders statuses consistently (green = good, red = bad, etc.).
 */
const accountVariant: Record<
  AccountStatus,
  "success" | "secondary" | "destructive"
> = {
  ACTIVE: "success",
  FROZEN: "secondary",
  CLOSED: "destructive",
};

const txnVariant: Record<
  TransactionStatus,
  "success" | "secondary" | "destructive" | "outline"
> = {
  COMPLETED: "success",
  PENDING: "secondary",
  FAILED: "destructive",
  REVERSED: "outline",
};

const kycVariant: Record<KycStatus, "success" | "secondary" | "destructive"> = {
  VERIFIED: "success",
  PENDING: "secondary",
  REJECTED: "destructive",
};

export function AccountStatusBadge({ status }: { status: AccountStatus }) {
  const { t } = useTranslation();
  return (
    <Badge variant={accountVariant[status]}>
      {t(`accounts.statusLabel.${status}`)}
    </Badge>
  );
}

export function TransactionStatusBadge({
  status,
}: {
  status: TransactionStatus;
}) {
  const { t } = useTranslation();
  return (
    <Badge variant={txnVariant[status]}>
      {t(`transactions.statusLabel.${status}`)}
    </Badge>
  );
}

export function KycBadge({ status }: { status: KycStatus }) {
  return <Badge variant={kycVariant[status]}>{status}</Badge>;
}
