import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * cn — the canonical shadcn class-name helper.
 *
 * `clsx` lets you pass conditional/array/object class names ergonomically;
 * `twMerge` then resolves Tailwind conflicts so the LAST utility wins
 * (e.g. cn("p-2", "p-4") -> "p-4" instead of both being emitted). Every
 * UI primitive uses this so callers can override styles via a `className` prop.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * formatMoney — locale + currency aware money formatting.
 *
 * Money values arrive from the backend as strings (they are BigDecimal /
 * NUMERIC(19,4) server-side; sending them as JSON numbers would risk float
 * rounding). We parse to a number purely for display via Intl.NumberFormat,
 * which honors the active locale (grouping separators, digit shaping) and the
 * account currency. Never do arithmetic on these display values.
 */
export function formatMoney(
  amount: string | number,
  currency: string,
  locale: string,
): string {
  const value = typeof amount === "string" ? Number(amount) : amount;
  try {
    return new Intl.NumberFormat(locale, {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
    }).format(Number.isFinite(value) ? value : 0);
  } catch {
    // Fallback if the currency code is unknown to the runtime.
    return `${currency} ${value.toFixed(2)}`;
  }
}

/** Format an ISO timestamp into a localized date-time string. */
export function formatDateTime(iso: string, locale: string): string {
  try {
    return new Intl.DateTimeFormat(locale, {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

/** Format an ISO timestamp into a localized date-only string. */
export function formatDate(iso: string, locale: string): string {
  try {
    return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(
      new Date(iso),
    );
  } catch {
    return iso;
  }
}
