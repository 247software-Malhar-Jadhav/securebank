import { useTranslation } from "react-i18next";
import { cn } from "@/lib/utils";
import { formatMoney } from "@/lib/utils";
import type { TransactionType } from "@/types";

/**
 * Money — renders a currency amount localized to the active language.
 *
 * For transaction rows we optionally color and sign the value by direction:
 * deposits are green & positive, withdrawals/transfers are red & negative. The
 * `signedBy` prop opts into that behavior; plain balances render neutrally.
 */
export function Money({
  amount,
  currency,
  signedBy,
  className,
}: {
  amount: string | number;
  currency: string;
  signedBy?: TransactionType;
  className?: string;
}) {
  const { i18n } = useTranslation();
  const formatted = formatMoney(amount, currency, i18n.language);

  if (!signedBy) {
    return <span className={cn("tabular-nums", className)}>{formatted}</span>;
  }

  const isCredit = signedBy === "DEPOSIT";
  return (
    <span
      className={cn(
        "tabular-nums font-medium",
        isCredit ? "text-success" : "text-destructive",
        className,
      )}
    >
      {isCredit ? "+" : "−"}
      {formatted}
    </span>
  );
}
