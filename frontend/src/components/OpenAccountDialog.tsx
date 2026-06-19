import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
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
import { toast } from "@/components/ui/sonner";
import { useOpenAccountMutation } from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";
import type { AccountType } from "@/types";

const ACCOUNT_TYPES: AccountType[] = ["SAVINGS", "CURRENT", "FIXED_DEPOSIT"];
const CURRENCIES = ["INR", "USD", "EUR", "GBP"];

/**
 * OpenAccountDialog — modal form to open a new account.
 *
 * On success it relies on RTK Query tag invalidation: openAccount invalidates the
 * Account LIST tag, so the accounts list refetches automatically — we don't manually
 * push the new account anywhere.
 */
export function OpenAccountDialog() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [openAccount, { isLoading }] = useOpenAccountMutation();

  const schema = z.object({
    type: z.enum(["SAVINGS", "CURRENT", "FIXED_DEPOSIT"]),
    currency: z.string().min(1),
  });
  type FormValues = z.infer<typeof schema>;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: "SAVINGS", currency: "INR" },
  });

  async function onSubmit(values: FormValues) {
    try {
      await openAccount(values).unwrap();
      toast.success(t("accounts.openSuccess"));
      setOpen(false);
      form.reset();
    } catch (err) {
      toast.error(extractErrorMessage(err as never, t("errors.generic")));
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4" />
          {t("accounts.openAccount")}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("accounts.openAccountTitle")}</DialogTitle>
          <DialogDescription>{t("accounts.openAccountDesc")}</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("common.type")}</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {ACCOUNT_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {t(`accounts.type.${type}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="currency"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>currency</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {CURRENCIES.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? t("common.loading") : t("common.open")}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
