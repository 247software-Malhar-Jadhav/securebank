import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { CreditCard, PiggyBank, Landmark } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Money } from "@/components/Money";
import { AccountStatusBadge } from "@/components/StatusBadge";
import type { Account, AccountType } from "@/types";

const ICONS: Record<AccountType, typeof CreditCard> = {
  SAVINGS: PiggyBank,
  CURRENT: CreditCard,
  FIXED_DEPOSIT: Landmark,
};

/** AccountCard — a tappable summary card linking to the account detail page. */
export function AccountCard({ account }: { account: Account }) {
  const { t } = useTranslation();
  const Icon = ICONS[account.type];

  return (
    <Link to={`/accounts/${account.id}`} className="block">
      <Card className="transition-shadow hover:shadow-md">
        <CardContent className="p-5">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Icon className="h-5 w-5" />
              </div>
              <div>
                <p className="font-medium">{t(`accounts.type.${account.type}`)}</p>
                <p className="font-mono text-xs text-muted-foreground">
                  {account.accountNumber}
                </p>
              </div>
            </div>
            <AccountStatusBadge status={account.status} />
          </div>
          <div className="mt-4">
            <p className="text-xs text-muted-foreground">{t("common.balance")}</p>
            <Money
              amount={account.balance}
              currency={account.currency}
              className="text-2xl font-bold"
            />
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
