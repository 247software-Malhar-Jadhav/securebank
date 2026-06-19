import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { CheckCircle2, Send } from "lucide-react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { toast } from "@/components/ui/sonner";
import { PageHeader } from "@/components/PageHeader";
import { Money } from "@/components/Money";
import {
  useGetAccountsQuery,
  useGetBeneficiariesQuery,
  useTransferMutation,
} from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";
import { formatMoney } from "@/lib/utils";
import type { Transaction } from "@/types";

/**
 * TransferPage — the money-movement form.
 *
 * End-to-end flow:
 *   1. Load the user's accounts (source dropdown) and saved beneficiaries.
 *   2. zod validates: source chosen, a destination account number present, a
 *      positive 2-decimal amount, and source != destination.
 *   3. On submit we call the transfer mutation. RTK Query invalidates the Account
 *      LIST + the source account's transactions + Insights, so balances everywhere
 *      refresh on their own — that's the "optimistic UX" without hand-rolled cache
 *      surgery: the moment the server confirms, the dashboard is already correct.
 *   4. We show a success panel with the server-issued reference id.
 */
export function TransferPage() {
  const { t, i18n } = useTranslation();
  const accountsQ = useGetAccountsQuery();
  const beneficiariesQ = useGetBeneficiariesQuery();
  const [transfer, { isLoading }] = useTransferMutation();

  // Holds the completed transaction so we can show the success panel + reference.
  const [completed, setCompleted] = useState<Transaction | null>(null);

  const schema = z
    .object({
      fromAccountId: z.string().min(1, t("transfer.validation.fromRequired")),
      toAccountNumber: z
        .string()
        .min(1, t("transfer.validation.toRequired")),
      amount: z
        .string()
        .min(1, t("transfer.validation.amountRequired"))
        // Up to 2 decimals.
        .regex(/^\d+(\.\d{1,2})?$/, t("transfer.validation.amountFormat"))
        .refine((v) => Number(v) > 0, t("transfer.validation.amountPositive")),
      description: z.string().optional(),
    })
    // Cross-field rule: you can't transfer to the same account you're sending from.
    .refine(
      (data) => {
        const from = accountsQ.data?.find(
          (a) => String(a.id) === data.fromAccountId,
        );
        return !from || from.accountNumber !== data.toAccountNumber;
      },
      {
        message: t("transfer.validation.sameAccount"),
        path: ["toAccountNumber"],
      },
    );
  type FormValues = z.infer<typeof schema>;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      fromAccountId: "",
      toAccountNumber: "",
      amount: "",
      description: "",
    },
  });

  const selectedFrom = accountsQ.data?.find(
    (a) => String(a.id) === form.watch("fromAccountId"),
  );

  async function onSubmit(values: FormValues) {
    try {
      const result = await transfer({
        fromAccountId: Number(values.fromAccountId),
        toAccountNumber: values.toAccountNumber,
        amount: values.amount,
        description: values.description,
      }).unwrap();
      setCompleted(result);
      toast.success(t("transfer.success"));
      form.reset();
    } catch (err) {
      toast.error(extractErrorMessage(err as never, t("errors.generic")));
    }
  }

  // ---- Success view ------------------------------------------------------
  if (completed) {
    return (
      <div className="mx-auto max-w-md">
        <Card>
          <CardContent className="flex flex-col items-center gap-4 p-8 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-success/10">
              <CheckCircle2 className="h-8 w-8 text-success" />
            </div>
            <div>
              <h2 className="text-xl font-bold">{t("transfer.success")}</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {t("transfer.successDetail", {
                  amount: formatMoney(
                    completed.amount,
                    completed.currency,
                    i18n.language,
                  ),
                })}
              </p>
            </div>
            <div className="w-full rounded-md bg-muted/50 p-3 text-left">
              <p className="text-xs text-muted-foreground">
                {t("transfer.referenceLabel")}
              </p>
              <p className="font-mono text-sm font-medium">{completed.reference}</p>
            </div>
            <Button className="w-full" onClick={() => setCompleted(null)}>
              {t("transfer.newTransfer")}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // ---- Form view ---------------------------------------------------------
  return (
    <div className="mx-auto max-w-xl">
      <PageHeader title={t("transfer.title")} subtitle={t("transfer.subtitle")} />
      <Card>
        <CardHeader>
          <CardTitle>{t("transfer.send")}</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
              {/* From account */}
              <FormField
                control={form.control}
                name="fromAccountId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("transfer.fromAccount")}</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder={t("transfer.selectAccount")} />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {(accountsQ.data ?? []).map((a) => (
                          <SelectItem key={a.id} value={String(a.id)}>
                            {t(`accounts.type.${a.type}`)} · {a.accountNumber} ·{" "}
                            {formatMoney(a.balance, a.currency, i18n.language)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {selectedFrom && (
                      <FormDescription>
                        {t("common.balance")}:{" "}
                        <Money
                          amount={selectedFrom.balance}
                          currency={selectedFrom.currency}
                        />
                      </FormDescription>
                    )}
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* To: pick a saved beneficiary (fills the account number) ... */}
              <FormItem>
                <FormLabel>{t("transfer.toBeneficiary")}</FormLabel>
                <Select
                  onValueChange={(accountNumber) =>
                    form.setValue("toAccountNumber", accountNumber, {
                      shouldValidate: true,
                    })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t("transfer.selectBeneficiary")} />
                  </SelectTrigger>
                  <SelectContent>
                    {(beneficiariesQ.data ?? []).map((b) => (
                      <SelectItem key={b.id} value={b.accountNumber}>
                        {b.name} · {b.accountNumber}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormItem>

              {/* ... or type an account number directly. */}
              <FormField
                control={form.control}
                name="toAccountNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("transfer.toAccountNumber")}</FormLabel>
                    <FormControl>
                      <Input
                        placeholder={t("transfer.orEnterManually")}
                        className="font-mono"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Separator />

              {/* Amount */}
              <FormField
                control={form.control}
                name="amount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("common.amount")}</FormLabel>
                    <FormControl>
                      <Input
                        inputMode="decimal"
                        placeholder="0.00"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Description */}
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("common.description")}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Button type="submit" className="w-full" disabled={isLoading}>
                <Send className="h-4 w-4" />
                {isLoading ? t("transfer.sending") : t("transfer.send")}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}
