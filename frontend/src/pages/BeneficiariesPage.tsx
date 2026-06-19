import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, Users } from "lucide-react";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "@/components/ui/sonner";
import { PageHeader } from "@/components/PageHeader";
import { EmptyState, ErrorState } from "@/components/States";
import {
  useCreateBeneficiaryMutation,
  useGetBeneficiariesQuery,
} from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";

/** AddBeneficiaryDialog — modal that creates a payee; list refetches via tag invalidation. */
function AddBeneficiaryDialog() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [create, { isLoading }] = useCreateBeneficiaryMutation();

  const schema = z.object({
    name: z.string().min(1, t("beneficiaries.validation.nameRequired")),
    accountNumber: z
      .string()
      .min(1, t("beneficiaries.validation.accountRequired")),
    bankName: z.string().optional(),
  });
  type FormValues = z.infer<typeof schema>;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", accountNumber: "", bankName: "" },
  });

  async function onSubmit(values: FormValues) {
    try {
      await create(values).unwrap();
      toast.success(t("beneficiaries.addSuccess"));
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
          {t("beneficiaries.add")}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("beneficiaries.addTitle")}</DialogTitle>
          <DialogDescription>{t("beneficiaries.addDesc")}</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("beneficiaries.name")}</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="accountNumber"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("beneficiaries.accountNumber")}</FormLabel>
                  <FormControl>
                    <Input className="font-mono" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="bankName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("beneficiaries.bankName")}</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? t("common.loading") : t("common.add")}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

/** BeneficiariesPage — list of saved payees + add dialog. */
export function BeneficiariesPage() {
  const { t } = useTranslation();
  const { data, isLoading, isError, error, refetch } = useGetBeneficiariesQuery();

  return (
    <div>
      <PageHeader
        title={t("beneficiaries.title")}
        subtitle={t("beneficiaries.subtitle")}
        action={<AddBeneficiaryDialog />}
      />

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((i) => (
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
                  <TableHead>{t("beneficiaries.name")}</TableHead>
                  <TableHead>{t("beneficiaries.accountNumber")}</TableHead>
                  <TableHead>{t("beneficiaries.bankName")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((b) => (
                  <TableRow key={b.id}>
                    <TableCell className="font-medium">{b.name}</TableCell>
                    <TableCell className="font-mono">{b.accountNumber}</TableCell>
                    <TableCell>{b.bankName || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ) : (
        <EmptyState
          message={t("beneficiaries.empty")}
          icon={<Users className="h-8 w-8" />}
          action={<AddBeneficiaryDialog />}
        />
      )}
    </div>
  );
}
