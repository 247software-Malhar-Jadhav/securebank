import { useTranslation } from "react-i18next";
import {
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
} from "recharts";
import { formatMoney } from "@/lib/utils";
import type { SpendingCategory } from "@/types";

/**
 * SpendingChart — a donut chart of spend-by-category from /insights/spending.
 *
 * recharts renders SVG and is responsive via <ResponsiveContainer>. We feed it the
 * category breakdown and color slices from our token-derived palette. The tooltip
 * formats amounts with the active locale + the insights currency.
 */

// A small, accessible palette that reads well in both light and dark themes.
const COLORS = [
  "hsl(221 83% 53%)",
  "hsl(152 60% 40%)",
  "hsl(38 92% 50%)",
  "hsl(280 65% 60%)",
  "hsl(199 89% 48%)",
  "hsl(0 72% 55%)",
  "hsl(160 60% 45%)",
  "hsl(326 78% 60%)",
];

export function SpendingChart({
  categories,
  currency,
}: {
  categories: SpendingCategory[];
  currency: string;
}) {
  const { i18n } = useTranslation();

  const data = categories.map((c) => ({
    name: c.category,
    value: Number(c.amount),
  }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          innerRadius={60}
          outerRadius={100}
          paddingAngle={2}
        >
          {data.map((_, index) => (
            <Cell key={index} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip
          formatter={(value: number) => formatMoney(value, currency, i18n.language)}
          contentStyle={{
            background: "hsl(var(--popover))",
            border: "1px solid hsl(var(--border))",
            borderRadius: "0.5rem",
            color: "hsl(var(--popover-foreground))",
          }}
        />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
